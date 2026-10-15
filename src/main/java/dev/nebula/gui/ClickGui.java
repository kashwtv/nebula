package dev.nebula.gui;

import dev.nebula.Nebula;
import dev.nebula.NebulaClient;
import dev.nebula.config.ConfigIO;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.client.ClickGuiModule;
import dev.nebula.module.client.HudEditorModule;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.MultiSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.Blur;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.util.Anim;
import dev.nebula.util.Col;
import dev.nebula.util.Ease;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Right Shift window.
 *
 *   Left click a card ........ toggle it
 *   Right click / arrow ...... open its settings
 *   Middle click ............. set a keybind
 *   Diamond on a card ........ add to Favorites
 *   Just type ................ search every module
 */
public final class ClickGui extends Screen {
    // persisted: a Category name, "FAVORITES" or "ACTIVE"
    public static String view = "COMBAT";
    public static float offX, offY;

    private static final float SB = 128, SP = 184, HEAD = 52, CARD_H = 40, GAP = 7, PAD = 14, NAV_H = 22;

    private final long openTime = System.currentTimeMillis();
    private boolean closing;
    private long closeTime;
    private final Particles particles = new Particles();
    private String search = "";

    private final Anim paneAnim = new Anim(0);
    private final Anim gridScroll = new Anim(0), paneScroll = new Anim(0);
    private final Map<String, Anim> navHover = new HashMap<>();
    private final Anim hudBtn = new Anim(0), closeBtn = new Anim(0), paneSwitch = new Anim(0);
    private float gridTarget, paneTarget, gridMax, paneMax;
    private long switchTime = System.currentTimeMillis();
    private final SettingsView view0 = new SettingsView();

    Module selected;
    Module listening;
    NumberSetting slider;
    float sliderX, sliderW;
    ColorSetting hue;
    float hueX, hueW;

    private boolean draggingWindow;
    private float dragDX, dragDY;
    private float s, vw, vh, wx, wy, ww, wh;
    private final List<Object[]> hits = new ArrayList<>();

    public ClickGui() {
        super(Component.literal("Nebula"));
    }

    private ClickGuiModule cfg() {
        return ModuleManager.get(ClickGuiModule.class);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float progress() {
        long now = System.currentTimeMillis();
        if (closing) return 1 - Ease.clamp01((now - closeTime) / 220f);
        return Ease.clamp01((now - openTime) / 380f);
    }

    void click(boolean up) {
        ClickGuiModule c = cfg();
        if (c != null && c.sounds.get())
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), up ? 1.35f : 1.0f, 0.2f));
        ConfigIO.save();
    }

    void openHudEditor() {
        click(true);
        minecraft.setScreen(new HudEditor());
    }

    void dragSlider(float mx) {
        if (slider != null) slider.set(slider.min + (slider.max - slider.min) * Ease.clamp01((mx - sliderX) / sliderW));
    }

    void dragHue(float mx) {
        if (hue != null) hue.hue = Ease.clamp01((mx - hueX) / hueW);
    }

    /** Used by the dev test driver. */
    public void devSelect(Module m) {
        if (m != null) view = m.category.name();
        selected = m;
    }

    private void setView(String v) {
        if (v.equals(view) && search.isEmpty()) return;
        view = v;
        search = "";
        gridTarget = 0;
        gridScroll.set(0);
        switchTime = System.currentTimeMillis();
        click(true);
    }

    private String viewLabel() {
        if (!search.isEmpty()) return "Search";
        return switch (view) {
            case "FAVORITES" -> "Favorites";
            case "ACTIVE" -> "Active";
            default -> category().label;
        };
    }

    private Category category() {
        try {
            return Category.valueOf(view);
        } catch (Exception e) {
            return Category.COMBAT;
        }
    }

    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<>();
        if (!search.isEmpty()) {
            String q = search.toLowerCase(Locale.ROOT);
            for (Module m : ModuleManager.MODULES)
                if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            return out;
        }
        switch (view) {
            case "FAVORITES" -> { for (Module m : ModuleManager.MODULES) if (m.favorite) out.add(m); }
            case "ACTIVE" -> { for (Module m : ModuleManager.MODULES) if (m.toggleable() && m.isEnabled()) out.add(m); }
            default -> out.addAll(ModuleManager.of(category()));
        }
        return out;
    }

    private void hit(float x, float y, float w, float h, String kind, Object data) {
        hits.add(new Object[]{new float[]{x, y, w, h}, kind, data});
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        hits.clear();
        float p = progress();
        float ep = Ease.outCubic(p);
        ClickGuiModule c = cfg();
        s = R2D.uiScale();
        vw = width / s;
        vh = height / s;
        float mx = mouseX / s, my = mouseY / s;
        R2D.smooth = 0.6f / R2D.ppu();

        if (c.blur.get()) Blur.apply(g, c.blurStrength.f() * ep);
        g.pose().pushPose();
        g.pose().scale(s, s, 1);
        R2D.alpha = ep;
        R2D.rect(g, 0, 0, vw, vh, 0, Col.alpha(0xFF000000, c.dim.f()));
        if (c.particles.get()) particles.render(g, vw, vh, mx, my, ep * 0.6f);

        if (slider != null) dragSlider(mx);
        if (hue != null) dragHue(mx);

        ww = Math.min(vw - 24, 640);
        wh = Math.min(vh - 30, 372);
        if (draggingWindow) {
            offX = mx - dragDX - (vw - ww) / 2;
            offY = my - dragDY - (vh - wh) / 2;
        }
        offX = Math.max(-(vw - ww) / 2, Math.min((vw - ww) / 2, offX));
        offY = Math.max(-(vh - wh) / 2, Math.min((vh - wh) / 2, offY));
        wx = (vw - ww) / 2 + offX;
        wy = (vh - wh) / 2 + offY;

        var pose = g.pose();
        pose.pushPose();
        float sc = 0.97f + 0.03f * ep;
        pose.translate(wx + ww / 2, wy + wh / 2 + (1 - ep) * 10, 0);
        pose.scale(sc, sc, 1);
        pose.translate(-(wx + ww / 2), -(wy + wh / 2), 0);

        int acc = Theme.accent();
        float px = R2D.px();
        R2D.glow(g, wx, wy + 4, ww, wh, 10, 44, 0x88000000);
        R2D.rect(g, wx, wy, ww, wh, 10, Theme.BG);
        R2D.rect(g, wx, wy, SB, wh, 10, Theme.SURFACE);
        R2D.rect(g, wx + SB - 10, wy, 10, wh, 0, Theme.SURFACE);
        R2D.rect(g, wx + SB, wy, px, wh, 0, 0x10FFFFFF);

        sidebar(g, mx, my, acc);
        float pa = paneAnim.update(selected != null ? 1 : 0, 14);
        float gridW = ww - SB - SP * pa;
        grid(g, mx, my, acc, wx + SB, gridW);
        if (pa > 0.01f) pane(g, mx, my, acc, wx + ww - SP * pa, pa);

        R2D.outline(g, wx, wy, ww, wh, 10, px, Theme.BORDER);
        pose.popPose();

        Notifications.render(g, vw, vh);
        R2D.alpha = 1;
        g.pose().popPose();
    }

    private void navRow(GuiGraphics g, String key, String label, float cy, float mx, float my, int acc, int count) {
        boolean sel = key.equals(view) && search.isEmpty();
        boolean hov = mx >= wx + 8 && mx < wx + SB - 8 && my >= cy && my < cy + NAV_H;
        float hv = navHover.computeIfAbsent(key, k -> new Anim(0)).update(sel ? 1 : hov ? 0.5f : 0, 16);
        if (hv > 0.01f) R2D.rect(g, wx + 8, cy, SB - 16, NAV_H, 7, Col.alpha(Theme.RAISED, hv));
        if (sel) R2D.rect(g, wx + 8, cy + 6, 2.5f, NAV_H - 12, 1.25f, acc);
        Icons.draw(g, key, wx + 23, cy + NAV_H / 2, sel ? acc : Col.lerp(Theme.DIM, Theme.MUTED, hv * 2));
        R2D.text(g, label, wx + 35, cy + 6.5f, Col.lerp(Theme.MUTED, Theme.TEXT, hv));
        if (count > 0) {
            String n = String.valueOf(count);
            float nw = R2D.width(n);
            R2D.rect(g, wx + SB - 14 - nw - 8, cy + 5, nw + 8, 12, 6, sel ? Col.alpha(acc, 0.2f) : Theme.TRACK);
            R2D.text(g, n, wx + SB - 14 - nw - 4, cy + 6.5f, sel ? acc : Theme.MUTED);
        }
        hit(wx + 8, cy, SB - 16, NAV_H, "nav", key);
    }

    private void sidebar(GuiGraphics g, float mx, float my, int acc) {
        float x = wx + 15, y = wy + 17;
        // logo mark: two offset rounded squares
        R2D.rect(g, x, y, 9, 9, 3, Col.alpha(acc, 0.45f));
        R2D.rect(g, x + 4, y + 4, 9, 9, 3, acc);
        R2D.bold(g, "Nebula", x + 19, y + 2, Theme.TEXT);
        R2D.text(g, Nebula.VERSION, x + 22 + R2D.boldWidth("Nebula"), y + 2, Theme.DIM);

        float cy = wy + 44;
        int favs = 0, active = 0;
        for (Module m : ModuleManager.MODULES) {
            if (m.favorite) favs++;
            if (m.toggleable() && m.isEnabled()) active++;
        }
        navRow(g, "FAVORITES", "Favorites", cy, mx, my, acc, favs);
        cy += NAV_H + 1;
        navRow(g, "ACTIVE", "Active", cy, mx, my, acc, active);
        cy += NAV_H + 8;
        R2D.text(g, "Modules", wx + 17, cy, Theme.DIM);
        cy += 14;
        for (Category cat : Category.values()) {
            int on = 0;
            for (Module m : ModuleManager.of(cat)) if (m.toggleable() && m.isEnabled()) on++;
            navRow(g, cat.name(), cat.label, cy, mx, my, acc, on);
            cy += NAV_H + 1;
        }

        // bottom: profile, accent picker, Edit HUD
        float by = wy + wh - 28;
        float bx = wx + 8, bw = SB - 16, bh = 18;
        boolean hov = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
        float hv = hudBtn.update(hov ? 1 : 0, 16);
        R2D.rect(g, bx, by, bw, bh, 6, Col.lerp(Theme.RAISED, acc, hv));
        Icons.draw(g, "HUD", bx + 13, by + 9, Col.lerp(Theme.MUTED, Theme.onAccent(), hv));
        R2D.text(g, "Edit HUD", bx + 23, by + 5, Col.lerp(Theme.TEXT, Theme.onAccent(), hv));
        hit(bx, by, bw, bh, "hud", null);

        float dy = by - 16;
        if (dy > cy + 30) {
            ClickGuiModule c = cfg();
            float dx = wx + 15;
            for (int i = 0; i < Theme.NAMES.length - 1; i++) {
                boolean on = c.theme.index == i;
                boolean dh = mx >= dx - 5 && mx < dx + 5 && my >= dy - 5 && my < dy + 5;
                if (on || dh) R2D.ring(g, dx, dy, 5, 1.1f, on ? Theme.TEXT : Theme.DIM);
                R2D.circle(g, dx, dy, 3.3f, Theme.swatch(i));
                hit(dx - 5, dy - 5, 10, 10, "accent", i);
                dx += 14;
            }
        }
        float py = dy - 30;
        var me = minecraft.player;
        if (me != null && py > cy + 6) {
            R2D.rect(g, wx + 8, py, SB - 16, 22, 6, Theme.BG);
            g.pose().pushPose();
            g.pose().translate(wx + 13, py + 3, 0);
            PlayerFaceRenderer.draw(g, me.getSkinTextureLocation(), 0, 0, 16);
            g.pose().popPose();
            String name = SettingsView.fit(me.getGameProfile().getName(), SB - 50);
            R2D.text(g, name, wx + 34, py + 3, Theme.TEXT);
            var info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(me.getUUID());
            String sub = minecraft.getCurrentServer() == null ? "Singleplayer" : (info == null ? 0 : info.getLatency()) + " ms";
            R2D.text(g, sub, wx + 34, py + 12, Theme.DIM);
        }
    }

    private void grid(GuiGraphics g, float mx, float my, int acc, float gx, float gw) {
        List<Module> mods = visibleModules();
        String title = viewLabel();
        R2D.title(g, title, gx + PAD, wy + 13, Theme.TEXT);
        int on = 0, total = 0;
        for (Module m : mods) {
            if (!m.toggleable()) continue;
            total++;
            if (m.isEnabled()) on++;
        }
        String sub = !search.isEmpty() ? mods.size() + " results" : mods.size() + " modules" + (total == 0 ? "" : "  \u00B7  " + on + " on");
        R2D.text(g, sub, gx + PAD, wy + 31, Theme.DIM);

        float sw = Math.min(150, gw - 170), sh = 20;
        float sx = gx + gw - PAD - sw, sy = wy + 15;
        if (sw > 40) {
            R2D.rect(g, sx, sy, sw, sh, 10, Theme.SURFACE);
            R2D.outline(g, sx, sy, sw, sh, 10, R2D.px(), search.isEmpty() ? Theme.BORDER : Col.alpha(acc, 0.7f));
            R2D.ring(g, sx + 11, sy + 9.3f, 3, 1.1f, Theme.DIM);
            R2D.rect(g, sx + 12.8f, sy + 11.3f, 2.8f, 1.1f, 0.5f, Theme.DIM);
            if (search.isEmpty()) {
                R2D.text(g, "Search", sx + 20, sy + 6, Theme.DIM);
                R2D.text(g, "type", sx + sw - 10 - R2D.width("type"), sy + 6, 0xFF4A4A50);
            } else {
                String shown = SettingsView.fit(search, sw - 30);
                R2D.text(g, shown, sx + 20, sy + 6, Theme.TEXT);
                if ((System.currentTimeMillis() / 500) % 2 == 0) R2D.rect(g, sx + 21 + R2D.width(shown), sy + 5, R2D.px() * 1.5f, 10, 0, acc);
            }
            hit(sx, sy, sw, sh, "search", null);
        }

        float top = wy + HEAD, bottom = wy + wh - 8;
        int cols = gw >= 300 ? 2 : 1;
        float cw = (gw - PAD * 2 - GAP * (cols - 1)) / cols;
        int rows = (mods.size() + cols - 1) / cols;
        float content = rows * (CARD_H + GAP) - GAP + 4;
        gridMax = Math.max(0, content - (bottom - top));
        gridTarget = Math.max(0, Math.min(gridMax, gridTarget));
        float scroll = gridScroll.update(gridTarget, 16);

        R2D.pushScissor(g, gx, top, gw, bottom - top);
        long now = System.currentTimeMillis();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int col = i % cols, row = i / cols;
            float cx = gx + PAD + col * (cw + GAP);
            float cy = top + row * (CARD_H + GAP) - scroll;
            if (cy + CARD_H < top || cy > bottom) continue;
            float in = Ease.outCubic((now - switchTime - i * 16) / 260f);
            float prev = R2D.alpha;
            R2D.alpha = prev * in;
            card(g, m, cx, cy + (1 - in) * 6, cw, mx, my, acc, top, bottom);
            R2D.alpha = prev;
        }
        if (mods.isEmpty()) {
            String msg = !search.isEmpty() ? "Nothing matches \"" + search + "\""
                    : view.equals("FAVORITES") ? "Click the diamond on any module to pin it here"
                    : view.equals("ACTIVE") ? "Nothing is turned on yet" : "Empty";
            R2D.textCentered(g, msg, gx + gw / 2, top + 34, Theme.DIM);
        }
        R2D.popScissor(g);
        if (gridMax > 0) {
            float trackH = bottom - top - 8;
            float thumb = Math.max(18, trackH * (bottom - top) / content);
            float ty = top + 4 + (trackH - thumb) * (scroll / gridMax);
            R2D.rect(g, gx + gw - 5, ty, 2, thumb, 1, 0x30FFFFFF);
        }
    }

    private void card(GuiGraphics g, Module m, float x, float y, float w, float mx, float my, int acc, float top, float bottom) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + CARD_H && my >= top && my < bottom;
        float hv = m.hoverAnim.update(hov ? 1 : 0, 18);
        float tv = m.toggleAnim.update(m.isEnabled() ? 1 : 0, 14);
        float fv = m.favAnim.update(m.favorite ? 1 : 0, 16);
        boolean sel = m == selected;
        float lift = hv * 1f;
        y -= lift;
        if (hv > 0.01f) R2D.glow(g, x, y + 3, w, CARD_H, 7, 10 * hv, Col.alpha(0xFF000000, 0.5f * hv));
        int base = Col.lerp(Theme.SURFACE, 0xFF1D1D21, hv);
        R2D.rect(g, x, y, w, CARD_H, 7, base);
        if (tv > 0.01f) {
            R2D.hGradient(g, x, y, w, CARD_H, 7, Col.alpha(acc, 0.10f * tv), Col.alpha(acc, 0.0f));
            R2D.rect(g, x + 1, y + 9, 2, CARD_H - 18, 1, Col.alpha(acc, tv));
        }
        if (sel) R2D.outline(g, x, y, w, CARD_H, 7, R2D.px() * 1.5f, acc);
        else R2D.outline(g, x, y, w, CARD_H, 7, R2D.px(), Col.alpha(0xFFFFFFFF, 0.04f + 0.05f * hv));

        float clipY = Math.max(y, top), clipH = Math.min(y + CARD_H, bottom) - clipY;
        float right = x + w - 10;
        float ctrlW;
        if (m instanceof HudEditorModule) {
            String t = "Open ›";
            ctrlW = R2D.width(t);
            R2D.text(g, t, right - ctrlW, y + CARD_H / 2 - 5, hv > 0.5f ? Theme.TEXT : Theme.MUTED);
        } else if (!m.toggleable()) {
            String t = "Settings ›";
            ctrlW = R2D.width(t);
            R2D.text(g, t, right - ctrlW, y + CARD_H / 2 - 5, hv > 0.5f ? Theme.TEXT : Theme.MUTED);
        } else {
            float tw = 19, th = 10, tx = right - tw, ty = y + CARD_H / 2 - th / 2;
            R2D.rect(g, tx, ty, tw, th, th / 2, Col.lerp(Theme.TRACK, acc, tv));
            R2D.circle(g, tx + 5 + 9 * tv, ty + th / 2, 3.4f, Col.lerp(0xFF8B8B91, Theme.onAccent(), tv));
            ctrlW = tw;
            boolean arrowHov = hov && mx >= right - tw - 17 && mx < right - tw - 3;
            R2D.text(g, "›", right - tw - 11, y + CARD_H / 2 - 5, arrowHov || sel ? Theme.TEXT : Theme.DIM);
            ctrlW += 15;
            hit(right - tw - 17, clipY, 14, clipH, "settings", m);
        }
        if (m.key >= 0) {
            String k = SettingsView.keyName(m.key).toUpperCase(Locale.ROOT);
            if (k.length() > 5) k = k.substring(0, 5);
            float kw = R2D.width(k) + 6;
            float kx = right - ctrlW - 6 - kw;
            R2D.rect(g, kx, y + CARD_H / 2 - 6, kw, 12, 4, Theme.TRACK);
            R2D.text(g, k, kx + 3, y + CARD_H / 2 - 4.5f, Theme.MUTED);
            ctrlW += kw + 6;
        }
        // favorite diamond: always shown when pinned, otherwise on hover
        float sa = Math.max(fv, hv * 0.8f);
        if (sa > 0.02f) {
            float fx = x + w - 8, fy = y + 6;
            boolean fh = hov && mx >= fx - 5 && mx < fx + 5 && my >= fy - 5 && my < fy + 5;
            Icons.star(g, fx, fy, 4.2f, Col.alpha(m.favorite ? acc : fh ? Theme.TEXT : Theme.DIM, sa), m.favorite);
            hit(fx - 6, fy - 5, 12, 10, "fav", m);
        }
        float textW = w - 20 - ctrlW - 6;
        R2D.bold(g, SettingsView.fit(m.name, textW), x + 12, y + 8, Col.lerp(0xFFC9C9CE, Theme.TEXT, Math.max(tv, hv)));
        R2D.text(g, SettingsView.fit(m.description, textW), x + 12, y + 22, Theme.DIM);
        hit(x, clipY, w, clipH, "card", m);
    }

    private void pane(GuiGraphics g, float mx, float my, int acc, float px0, float pa) {
        Module m = selected;
        if (m == null) return;
        float prev = R2D.alpha;
        R2D.alpha = prev * pa;
        float px = R2D.px();
        R2D.rect(g, px0, wy, SP, wh, 10, Theme.SURFACE);
        R2D.rect(g, px0, wy, 10, wh, 0, Theme.SURFACE);
        R2D.rect(g, px0, wy, px, wh, 0, 0x10FFFFFF);
        float x = px0 + 13, w = SP - 26;

        // header: name, favorite, close
        String nm = m.name;
        while (nm.length() > 1 && R2D.titleWidth(nm) > w - 26) nm = nm.substring(0, nm.length() - 1);
        R2D.title(g, nm, x, wy + 13, Theme.TEXT);
        float cx = px0 + SP - 20, cy = wy + 10;
        boolean ch = mx >= cx && mx < cx + 12 && my >= cy && my < cy + 14;
        float cv = closeBtn.update(ch ? 1 : 0, 16);
        R2D.text(g, "×", cx + 3, cy + 3.5f, Col.lerp(Theme.DIM, Theme.TEXT, cv));
        hit(cx, cy, 12, 14, "close", null);
        float fx = cx - 9, fy = wy + 18;
        Icons.star(g, fx, fy, 4.2f, m.favorite ? acc : Theme.DIM, m.favorite);
        hit(fx - 6, fy - 6, 12, 12, "fav", m);

        List<String> lines = wrap(m.description, w);
        float dy = wy + 35;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            R2D.text(g, lines.get(i), x, dy, Theme.MUTED);
            dy += 10;
        }
        // big enable switch
        if (m.toggleable()) {
            dy += 4;
            float tv = paneSwitch.update(m.isEnabled() ? 1 : 0, 14);
            boolean sh = mx >= x && mx < x + w && my >= dy && my < dy + 20;
            R2D.rect(g, x, dy, w, 20, 6, sh ? 0xFF1F1F23 : Theme.RAISED);
            R2D.text(g, m.isEnabled() ? "Enabled" : "Disabled", x + 8, dy + 6, m.isEnabled() ? Theme.TEXT : Theme.MUTED);
            float tw = 19, th = 10, tx = x + w - tw - 6, ty = dy + 5;
            R2D.rect(g, tx, ty, tw, th, th / 2, Col.lerp(Theme.TRACK, acc, tv));
            R2D.circle(g, tx + 5 + 9 * tv, ty + th / 2, 3.4f, Col.lerp(0xFF8B8B91, Theme.onAccent(), tv));
            hit(x, dy, w, 20, "toggle", m);
            dy += 20;
        }
        float top = dy + 8, bottom = wy + wh - 8;
        R2D.rect(g, x, top - 4, w, px, 0, 0x14FFFFFF);
        float content = SettingsView.contentHeight(m, w) + 4;
        paneMax = Math.max(0, content - (bottom - top));
        paneTarget = Math.max(0, Math.min(paneMax, paneTarget));
        float sc = paneScroll.update(paneTarget, 16);
        R2D.pushScissor(g, px0, top, SP, bottom - top);
        if (m.settings.isEmpty() && !m.toggleable()) R2D.text(g, "No settings", x, top + 4, Theme.DIM);
        view0.draw(this, g, m, x, top + 2 - sc, w, mx, my, top, bottom);
        R2D.popScissor(g);
        if (paneMax > 0) {
            float trackH = bottom - top - 4;
            float thumb = Math.max(16, trackH * (bottom - top) / content);
            float ty = top + 2 + (trackH - thumb) * (sc / paneMax);
            R2D.rect(g, px0 + SP - 5, ty, 2, thumb, 1, 0x30FFFFFF);
        }
        R2D.alpha = prev;
    }

    private static List<String> wrap(String text, float w) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (R2D.width(test) > w && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    // ------------------------------------------------------------------ input

    private void openSettings(Module m) {
        selected = selected == m ? null : m;
        paneTarget = 0;
        paneScroll.set(0);
        click(true);
    }

    private static boolean inside(float mx, float my, float[] r) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    private static boolean hasPicker(Module m) {
        for (var st : m.settings) if (st instanceof MultiSetting) return true;
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        float mx = (float) mouseX / s, my = (float) mouseY / s;
        if (selected != null && view0.click(this, selected, mx, my, button)) return true;
        // later hits were drawn on top; check them first, smallest targets win
        for (String kind : new String[]{"close", "fav", "toggle", "settings", "accent", "hud", "nav", "search", "card"}) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Object[] h = hits.get(i);
                if (!kind.equals(h[1]) || !inside(mx, my, (float[]) h[0])) continue;
                Object d = h[2];
                switch (kind) {
                    case "close" -> { selected = null; click(false); }
                    case "fav" -> { Module m = (Module) d; m.favorite = !m.favorite; click(m.favorite); }
                    case "toggle" -> { Module m = (Module) d; m.toggle(); click(m.isEnabled()); }
                    case "settings" -> openSettings((Module) d);
                    case "accent" -> { cfg().theme.index = (int) d; click(true); }
                    case "hud" -> openHudEditor();
                    case "nav" -> setView((String) d);
                    case "search" -> { }
                    case "card" -> {
                        Module m = (Module) d;
                        if (button == 2) {
                            selected = m;
                            listening = m;
                            click(true);
                        } else if (m instanceof HudEditorModule && button == 0) {
                            openHudEditor();
                        } else if (button == 0 && m.toggleable()) {
                            m.toggle();
                            if (m.isEnabled() && hasPicker(m) && selected != m) openSettings(m);
                            else click(m.isEnabled());
                        } else {
                            openSettings(m);
                        }
                    }
                }
                return true;
            }
        }
        boolean inWindow = mx >= wx && mx < wx + ww && my >= wy && my < wy + wh;
        boolean onHeader = my < wy + HEAD - 4 || mx < wx + SB;
        if (button == 0 && inWindow && onHeader) {
            draggingWindow = true;
            dragDX = mx - wx;
            dragDY = my - wy;
            return true;
        }
        if (!inWindow && button == 0) {
            if (selected != null) selected = null;
            else startClose();
        }
        listening = null;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean changed = slider != null || hue != null || draggingWindow;
        slider = null;
        hue = null;
        draggingWindow = false;
        if (changed) ConfigIO.save();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float mx = (float) mouseX / s;
        float paneX = wx + ww - SP * paneAnim.get();
        if (selected != null && mx >= paneX) paneTarget = Math.max(0, Math.min(paneMax, paneTarget - (float) delta * 24));
        else gridTarget = Math.max(0, Math.min(gridMax, gridTarget - (float) delta * 28));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (closing) return true;
        if (listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) listening.key = -1;
            else listening.key = key;
            listening = null;
            click(true);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (!search.isEmpty()) search = "";
            else if (selected != null) selected = null;
            else startClose();
            return true;
        }
        if (NebulaClient.OPEN_GUI.matches(key, scan)) {
            startClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            gridTarget = 0;
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (listening == null && !closing && ch >= 32 && ch != 127 && search.length() < 24) {
            search += ch;
            gridTarget = 0;
            switchTime = System.currentTimeMillis();
        }
        return true;
    }

    @Override
    public void onClose() {
        startClose();
    }

    private void startClose() {
        if (closing) return;
        closing = true;
        closeTime = System.currentTimeMillis();
        listening = null;
        ConfigIO.save();
    }

    @Override
    public void tick() {
        if (closing && progress() <= 0) minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        ConfigIO.save();
    }
}
