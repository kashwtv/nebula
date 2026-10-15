package dev.nebula.gui;

import dev.nebula.NebulaClient;
import dev.nebula.config.ConfigIO;
import dev.nebula.module.HudModule;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.client.HudEditorModule;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.util.Anim;
import dev.nebula.util.Col;
import dev.nebula.util.Ease;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Drag, resize and show/hide every HUD element. */
public final class HudEditor extends Screen {
    private static final float SNAP = 5, MARGIN = 4, HANDLE = 6;

    private final long openTime = System.currentTimeMillis();
    private float s, vw, vh;
    private HudModule dragging, resizing, stretching, hoveredHud;
    private float offX, offY, resizeX, resizeY;
    private Float guideX, guideY;
    private final Map<HudModule, Anim> hover = new HashMap<>();
    private final List<float[]> chipRects = new ArrayList<>();
    private final List<HudModule> chipMods = new ArrayList<>();
    private float[] bgButton;
    private HudModule bgTarget;

    public HudEditor() {
        super(Component.literal("Nebula HUD Editor"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private HudEditorModule cfg() {
        return ModuleManager.get(HudEditorModule.class);
    }

    private float sx(HudModule h) { return h.x(vw); }
    private float sy(HudModule h) { return h.y(vh); }
    private float sw(HudModule h) { return h.w * h.scale; }
    private float sh(HudModule h) { return h.h * h.scale; }

    private boolean inside(HudModule h, float mx, float my) {
        return mx >= sx(h) && mx < sx(h) + sw(h) && my >= sy(h) && my < sy(h) + sh(h);
    }

    private boolean onSide(HudModule h, float mx, float my) {
        if (h.lengthSetting() == null) return false;
        float hx = sx(h) + sw(h) + 2, hy = sy(h) + sh(h) / 2;
        return Math.abs(mx - hx) <= 4 && Math.abs(my - hy) <= Math.max(5, sh(h) / 3);
    }

    private boolean onHandle(HudModule h, float mx, float my) {
        float hx = sx(h) + sw(h), hy = sy(h) + sh(h);
        return Math.abs(mx - hx) <= HANDLE && Math.abs(my - hy) <= HANDLE;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        float ep = Ease.outCubic((System.currentTimeMillis() - openTime) / 350f);
        s = R2D.uiScale();
        vw = width / s;
        vh = height / s;
        float mx = mouseX / s, my = mouseY / s;
        R2D.smooth = 0.6f / R2D.ppu();
        g.pose().pushPose();
        g.pose().scale(s, s, 1);
        R2D.alpha = ep;
        int acc = Theme.accent();

        R2D.rect(g, 0, 0, vw, vh, 0, 0x60000000);
        if (cfg().grid.get()) {
            int cols = (int) (vw / 16) + 1, rows = (int) (vh / 16) + 1;
            float[] pts = new float[(cols + rows + 2) * 4];
            int[] cl = new int[cols + rows + 2];
            int n = 0;
            for (int i = 0; i < cols; i++) { pts[n * 4] = i * 16; pts[n * 4 + 1] = 0; pts[n * 4 + 2] = i * 16; pts[n * 4 + 3] = vh; cl[n++] = 0x0AFFFFFF; }
            for (int i = 0; i < rows; i++) { pts[n * 4] = 0; pts[n * 4 + 1] = i * 16; pts[n * 4 + 2] = vw; pts[n * 4 + 3] = i * 16; cl[n++] = 0x0AFFFFFF; }
            pts[n * 4] = vw / 2; pts[n * 4 + 1] = 0; pts[n * 4 + 2] = vw / 2; pts[n * 4 + 3] = vh; cl[n++] = 0x1CFFFFFF;
            pts[n * 4] = 0; pts[n * 4 + 1] = vh / 2; pts[n * 4 + 2] = vw; pts[n * 4 + 3] = vh / 2; cl[n++] = 0x1CFFFFFF;
            R2D.lines(g, pts, cl, n);
        }

        List<HudModule> huds = ModuleManager.huds();
        updateDrag(mx, my, huds);

        hoveredHud = null;
        // keep the BG button clickable while the cursor moves onto it
        if (bgButton != null && mx >= bgButton[0] - 2 && mx < bgButton[0] + bgButton[2] + 2 && my >= bgButton[1] - 2 && my < bgButton[1] + bgButton[3] + 4) {
            hoveredHud = bgTarget;
        }
        bgButton = null;
        if (hoveredHud == null)
            for (HudModule h : huds) if (h.isEnabled() && (inside(h, mx, my) || onHandle(h, mx, my) || onSide(h, mx, my))) hoveredHud = h;

        for (HudModule h : huds) {
            if (!h.isEnabled()) continue;
            h.render(g, vw, vh, true);
            boolean active = h == dragging || h == resizing || h == stretching;
            float hv = hover.computeIfAbsent(h, k -> new Anim(0)).update(h == hoveredHud || active ? 1 : 0, 14);
            float x = sx(h), y = sy(h), w = sw(h), hh = sh(h);
            float px = R2D.px();
            R2D.outline(g, x - 2, y - 2, w + 4, hh + 4, 3, px, Col.lerp(0x30FFFFFF, acc, hv));
            // corner handles: only exist here, in the editor
            float hs = 3.5f;
            int hc = Col.lerp(0x60FFFFFF, acc, hv);
            R2D.rect(g, x - 2 - hs / 2, y - 2 - hs / 2, hs, hs, 1, hc);
            R2D.rect(g, x + w + 2 - hs / 2, y - 2 - hs / 2, hs, hs, 1, hc);
            R2D.rect(g, x - 2 - hs / 2, y + hh + 2 - hs / 2, hs, hs, 1, hc);
            R2D.rect(g, x + w + 2 - hs / 2 - 0.5f, y + hh + 2 - hs / 2 - 0.5f, hs + 1, hs + 1, 1, Col.lerp(0xA0FFFFFF, acc, hv));
            if (h.lengthSetting() != null) {
                float sxp = x + w + 2, syp = y + hh / 2;
                R2D.rect(g, sxp - 1.5f, syp - 5, 3, 10, 1.5f, Col.lerp(0x80FFFFFF, acc, hv));
            }
            if (hv > 0.01f) {
                String tag = h.name + "  " + Math.round(h.scale * 100) + "%";
                float tw = R2D.width(tag) + 8;
                float ty = y - 15 < 0 ? y + hh + 5 : y - 15;
                R2D.rect(g, x - 2, ty, tw, 11, 2, Col.alpha(Theme.RAISED, hv));
                R2D.outline(g, x - 2, ty, tw, 11, 2, px, Col.alpha(acc, 0.6f * hv));
                R2D.text(g, tag, x + 2, ty + 2, Col.alpha(Theme.TEXT, hv));
                var bgs = h.backgroundSetting();
                if (bgs != null && h == hoveredHud) {
                    boolean on = bgs.get();
                    float bw = R2D.width("BG") + 8, bx = x + w + 2 - bw;
                    R2D.rect(g, bx, ty, bw, 11, 2, on ? acc : Theme.RAISED);
                    R2D.outline(g, bx, ty, bw, 11, 2, px, on ? acc : Theme.BORDER);
                    R2D.text(g, "BG", bx + 4, ty + 2, on ? Theme.onAccent() : Theme.MUTED);
                    bgButton = new float[]{bx, ty, bw, 11};
                    bgTarget = h;
                }
            }
        }

        if (guideX != null) {
            R2D.rect(g, guideX - R2D.px() / 2, 0, R2D.px(), vh, 0, acc);
        }
        if (guideY != null) {
            R2D.rect(g, 0, guideY - R2D.px() / 2, vw, R2D.px(), 0, acc);
        }

        dock(g, huds, mx, my, acc);

        String hint = "Drag to move · Corner = size · Side = length · Right click hide · Esc done";
        float hw = R2D.width(hint) + 16;
        R2D.glow(g, vw / 2 - hw / 2, vh - 21, hw, 15, 4, 8, 0x60000000);
        R2D.rect(g, vw / 2 - hw / 2, vh - 22, hw, 15, 4, Theme.SURFACE);
        R2D.outline(g, vw / 2 - hw / 2, vh - 22, hw, 15, 4, R2D.px(), Theme.BORDER);
        R2D.text(g, hint, vw / 2 - hw / 2 + 8, vh - 18, Theme.MUTED);

        R2D.alpha = 1;
        g.pose().popPose();
    }

    /** Hidden HUD elements as chips you can click to bring back. */
    private void dock(GuiGraphics g, List<HudModule> huds, float mx, float my, int acc) {
        chipRects.clear();
        chipMods.clear();
        List<HudModule> hidden = new ArrayList<>();
        for (HudModule h : huds) if (!h.isEnabled()) hidden.add(h);
        if (hidden.isEmpty()) return;
        String title = "Hidden - click to add";
        float tw = R2D.width(title);
        float maxRow = Math.min(vw - 60, 360);
        List<List<HudModule>> rows = new ArrayList<>();
        List<Float> widths = new ArrayList<>();
        List<HudModule> cur = new ArrayList<>();
        float cw = 0;
        for (HudModule h : hidden) {
            float w = R2D.width(h.name) + 16;
            if (cw + w > maxRow && !cur.isEmpty()) { rows.add(cur); widths.add(cw - 4); cur = new ArrayList<>(); cw = 0; }
            cur.add(h);
            cw += w + 4;
        }
        rows.add(cur);
        widths.add(cw - 4);
        float y = 24;
        R2D.text(g, title, vw / 2 - tw / 2, y - 12, Theme.MUTED);
        for (int r = 0; r < rows.size(); r++) {
            float x = vw / 2 - widths.get(r) / 2;
            for (HudModule h : rows.get(r)) {
                float w = R2D.width(h.name) + 16;
                boolean hov = mx >= x && mx < x + w && my >= y && my < y + 12;
                R2D.rect(g, x, y, w, 12, 3, hov ? acc : Theme.SURFACE);
                R2D.outline(g, x, y, w, 12, 3, R2D.px(), hov ? acc : Theme.BORDER);
                R2D.text(g, "+ " + h.name, x + 4, y + 2.5f, hov ? Theme.onAccent() : Theme.TEXT);
                chipRects.add(new float[]{x, y, w, 12});
                chipMods.add(h);
                x += w + 4;
            }
            y += 15;
        }
    }

    private void updateDrag(float mx, float my, List<HudModule> huds) {
        guideX = null;
        guideY = null;
        boolean snap = cfg().snap.get();
        if (dragging != null) {
            float w = sw(dragging), h = sh(dragging);
            float nx = mx - offX, ny = my - offY;
            if (snap) {
                // screen center
                if (Math.abs(nx + w / 2 - vw / 2) < SNAP) { nx = vw / 2 - w / 2; guideX = vw / 2; }
                if (Math.abs(ny + h / 2 - vh / 2) < SNAP) { ny = vh / 2 - h / 2; guideY = vh / 2; }
                // screen edges
                if (Math.abs(nx - MARGIN) < SNAP) { nx = MARGIN; guideX = MARGIN; }
                if (Math.abs(nx + w - (vw - MARGIN)) < SNAP) { nx = vw - MARGIN - w; guideX = vw - MARGIN; }
                if (Math.abs(ny - MARGIN) < SNAP) { ny = MARGIN; guideY = MARGIN; }
                if (Math.abs(ny + h - (vh - MARGIN)) < SNAP) { ny = vh - MARGIN - h; guideY = vh - MARGIN; }
                // other elements' edges
                for (HudModule o : huds) {
                    if (o == dragging || !o.isEnabled()) continue;
                    float ox = sx(o), oy = sy(o), ow = sw(o), oh = sh(o);
                    float[][] xs = {{nx, ox}, {nx, ox + ow + 2}, {nx + w, ox + ow}, {nx + w, ox - 2}};
                    for (float[] p : xs) if (guideX == null && Math.abs(p[0] - p[1]) < SNAP - 2) { nx += p[1] - p[0]; guideX = p[1]; }
                    float[][] ys = {{ny, oy}, {ny, oy + oh + 2}, {ny + h, oy + oh}, {ny + h, oy - 2}};
                    for (float[] p : ys) if (guideY == null && Math.abs(p[0] - p[1]) < SNAP - 2) { ny += p[1] - p[0]; guideY = p[1]; }
                }
            }
            nx = Math.max(0, Math.min(vw - w, nx));
            ny = Math.max(0, Math.min(vh - h, ny));
            dragging.setPos(nx, ny, vw, vh);
        }
        if (stretching != null) {
            var ls = stretching.lengthSetting();
            float x = sx(stretching), y = sy(stretching);
            ls.set((mx - x) / stretching.scale);
            stretching.setPos(x, y, vw, vh);
        }
        if (resizing != null) {
            float base = Math.max(1, resizing.w + resizing.h);
            float scale = ((mx - resizeX) + (my - resizeY)) / base;
            scale = Math.round(Math.max(0.4f, Math.min(3f, scale)) * 20) / 20f;
            float x = resizeX, y = resizeY;
            resizing.scale = scale;
            resizing.setPos(x, y, vw, vh);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float mx = (float) mouseX / s, my = (float) mouseY / s;
        if (bgButton != null && bgTarget != null && mx >= bgButton[0] && mx < bgButton[0] + bgButton[2] && my >= bgButton[1] && my < bgButton[1] + bgButton[3]) {
            var bgs = bgTarget.backgroundSetting();
            bgs.value = !bgs.value;
            ConfigIO.save();
            return true;
        }
        for (int i = 0; i < chipRects.size(); i++) {
            float[] r = chipRects.get(i);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                chipMods.get(i).setEnabled(true);
                ConfigIO.save();
                return true;
            }
        }
        List<HudModule> huds = ModuleManager.huds();
        for (int i = huds.size() - 1; i >= 0; i--) {
            HudModule h = huds.get(i);
            if (!h.isEnabled()) continue;
            if (button == 0 && onSide(h, mx, my)) {
                stretching = h;
                return true;
            }
            if (button == 0 && onHandle(h, mx, my)) {
                resizing = h;
                resizeX = sx(h);
                resizeY = sy(h);
                return true;
            }
            if (!inside(h, mx, my)) continue;
            if (button == 0) {
                dragging = h;
                offX = mx - sx(h);
                offY = my - sy(h);
            } else if (button == 1) {
                h.setEnabled(false);
                ConfigIO.save();
            } else if (button == 2) {
                h.scale = 1;
                ConfigIO.save();
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null || resizing != null || stretching != null) ConfigIO.save();
        dragging = null;
        resizing = null;
        stretching = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (hoveredHud != null) {
            float x = sx(hoveredHud), y = sy(hoveredHud);
            hoveredHud.scale = Math.round(Math.max(0.4f, Math.min(3f, hoveredHud.scale + (float) delta * 0.05f)) * 20) / 20f;
            hoveredHud.setPos(x, y, vw, vh);
            ConfigIO.save();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (NebulaClient.OPEN_GUI.matches(key, scan)) {
            ConfigIO.save();
            minecraft.setScreen(new ClickGui());
            return true;
        }
        return true;
    }

    @Override
    public void removed() {
        ConfigIO.save();
    }
}
