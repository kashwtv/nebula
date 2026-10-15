package dev.nebula.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ColorSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.MultiSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.module.setting.Setting;
import dev.nebula.render.R2D;
import dev.nebula.render.Theme;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Draws and handles a module's settings as a vertical list of rows. */
final class SettingsView {
    static final float H_BOOL = 17, H_MODE = 17, H_NUM = 24, H_COLOR = 28, H_BIND = 18;

    enum Kind { BOOL, NUMBER, MODE_PREV, MODE_NEXT, HUE, THEME, RGB, BIND, BIND_MODE, CHIP, RESET }

    record Hit(float x, float y, float w, float h, Kind kind, Setting setting, String opt) {
        boolean contains(float mx, float my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private float clipTop, clipBottom;
    private float sliderX, sliderW;

    static final float CHIP_H = 13, CHIP_GAP = 3, RESET_H = 24;

    /** Rows of chips needed to fit a multi-select into a width. */
    private static int chipRows(MultiSetting m, float w) {
        int rows = 1;
        float x = 0;
        for (String o : m.options) {
            float cw = R2D.width(o) + 10;
            if (x > 0 && x + cw > w) { rows++; x = 0; }
            x += cw + CHIP_GAP;
        }
        return rows;
    }

    static float height(Setting s, float w) {
        if (s instanceof MultiSetting ms) return 15 + chipRows(ms, w) * (CHIP_H + CHIP_GAP) + 3;
        if (s instanceof NumberSetting) return H_NUM;
        if (s instanceof ColorSetting) return H_COLOR;
        if (s instanceof ModeSetting) return H_MODE;
        return H_BOOL;
    }

    static float contentHeight(Module m, float w) {
        float h = 0;
        for (Setting s : m.settings) if (s.visible()) h += height(s, w);
        if (m.toggleable()) h += H_BIND + 6;
        if (!m.settings.isEmpty()) h += RESET_H;
        return h;
    }

    static String keyName(int key) {
        if (key < 0) return "None";
        String n = InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
        return n.length() > 10 ? n.substring(0, 10) : n;
    }

    static String fit(String s, float maxW) {
        if (R2D.width(s) <= maxW) return s;
        while (s.length() > 1 && R2D.width(s + "..") > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    private void hit(float x, float y, float w, float h, Kind k, Setting s) {
        hit(x, y, w, h, k, s, null);
    }

    private void hit(float x, float y, float w, float h, Kind k, Setting s, String opt) {
        float top = Math.max(y, clipTop), bottom = Math.min(y + h, clipBottom);
        if (bottom > top) hits.add(new Hit(x, top, w, bottom - top, k, s, opt));
    }

    void draw(ClickGui gui, GuiGraphics g, Module m, float x, float y, float w, float mx, float my, float clipTop, float clipBottom) {
        hits.clear();
        this.clipTop = clipTop;
        this.clipBottom = clipBottom;
        int acc = Theme.accent();
        float right = x + w;
        float sy = y;
        for (Setting s : m.settings) {
            if (!s.visible()) continue;
            float sh = height(s, w);
            if (sy + sh >= clipTop && sy <= clipBottom) row(gui, g, s, x, sy, w, right, mx, my, acc);
            sy += sh;
        }
        if (m.toggleable()) {
            sy += 6;
            R2D.rect(g, x, sy - 3, w, R2D.px(), 0, 0x14FFFFFF);
            bind(gui, g, m, x, sy, right, acc);
            sy += H_BIND;
        }
        if (!m.settings.isEmpty()) {
            float bw = R2D.width("Reset to defaults") + 14, by = sy + 6;
            boolean hov = mx >= x && mx < x + bw && my >= by && my < by + 14;
            R2D.rect(g, x, by, bw, 14, 4, hov ? Theme.RAISED : Theme.TRACK);
            R2D.text(g, "Reset to defaults", x + 7, by + 3.5f, hov ? Theme.TEXT : Theme.MUTED);
            hit(x, by, bw, 14, Kind.RESET, null);
        }
    }

    private void row(ClickGui gui, GuiGraphics g, Setting s, float x, float sy, float w, float right, float mx, float my, int acc) {
        boolean hov = mx >= x - 4 && mx < right + 4 && my >= sy && my < sy + height(s, w) && my >= clipTop && my < clipBottom;
        int label = hov ? Theme.TEXT : 0xFFA9A9AF;

        if (s instanceof MultiSetting ms) {
            R2D.text(g, ms.name, x, sy + 2, label);
            String count = ms.selected.size() + " selected";
            R2D.text(g, count, right - R2D.width(count), sy + 2, Theme.DIM);
            float cx = x, cy = sy + 14;
            for (String o : ms.options) {
                float cw = R2D.width(o) + 10;
                if (cx > x && cx + cw > right) { cx = x; cy += CHIP_H + CHIP_GAP; }
                boolean on = ms.has(o);
                boolean ch = mx >= cx && mx < cx + cw && my >= cy && my < cy + CHIP_H && my >= clipTop && my < clipBottom;
                R2D.rect(g, cx, cy, cw, CHIP_H, 4, on ? acc : ch ? Theme.RAISED : Theme.TRACK);
                R2D.text(g, o, cx + 5, cy + 2.7f, on ? Theme.onAccent() : ch ? Theme.TEXT : Theme.MUTED);
                hit(cx, cy, cw, CHIP_H, Kind.CHIP, s, o);
                cx += cw + CHIP_GAP;
            }
            return;
        }
        if (s instanceof BoolSetting b) {
            float v = b.anim.update(b.value ? 1 : 0, 16);
            R2D.text(g, fit(b.name, w - 24), x, sy + 4.5f, label);
            float tw = 15, th = 8, tx = right - tw, ty = sy + 4.5f;
            R2D.rect(g, tx, ty, tw, th, th / 2, Col.lerp(Theme.TRACK, acc, v));
            R2D.circle(g, tx + 4 + 7 * v, ty + th / 2, 2.8f, Col.lerp(0xFF8B8B91, Theme.onAccent(), v));
            hit(x - 4, sy, w + 8, H_BOOL, Kind.BOOL, s);
        } else if (s instanceof NumberSetting n) {
            float f = n.anim.update((float) n.fraction(), 20);
            boolean active = gui.slider == n;
            String val = n.display();
            float vw = R2D.width(val);
            R2D.text(g, fit(n.name, w - vw - 6), x, sy + 2, label);
            R2D.text(g, val, right - vw, sy + 2, active || hov ? Theme.TEXT : Theme.MUTED);
            float ty = sy + 15;
            R2D.rect(g, x, ty, w, 2, 1, Theme.TRACK);
            if (f > 0) R2D.rect(g, x, ty, Math.max(2, w * f), 2, 1, acc);
            float kr = active ? 3.6f : hov ? 3.2f : 2.8f;
            R2D.circle(g, x + w * f, ty + 1, kr, acc);
            hit(x - 4, sy, w + 8, H_NUM, Kind.NUMBER, s);
            if (gui.slider == n) { gui.sliderX = x; gui.sliderW = w; }
            sliderX = x;
            sliderW = w;
        } else if (s instanceof ModeSetting md) {
            String v = md.get();
            float vw = R2D.width(v);
            float aw = 7;
            R2D.text(g, fit(md.name, w - vw - aw * 2 - 8), x, sy + 4.5f, label);
            float vx = right - aw - vw;
            boolean lh = hov && mx < vx + vw / 2, rh = hov && mx >= vx + vw / 2;
            R2D.text(g, "‹", vx - aw, sy + 4.5f, lh ? Theme.TEXT : Theme.DIM);
            R2D.text(g, v, vx, sy + 4.5f, Theme.TEXT);
            R2D.text(g, "›", right - aw + 3, sy + 4.5f, rh ? Theme.TEXT : Theme.DIM);
            float mid = vx + vw / 2;
            hit(x - 4, sy, mid - x + 4, H_MODE, Kind.MODE_PREV, s);
            hit(mid, sy, right + 4 - mid, H_MODE, Kind.MODE_NEXT, s);
        } else if (s instanceof ColorSetting c) {
            int col = c.get();
            R2D.rect(g, right - 10, sy + 3, 10, 9, 2, col);
            R2D.outline(g, right - 10, sy + 3, 10, 9, 2, R2D.px(), Theme.BORDER);
            float rgbW = R2D.width("RGB") + 8, thW = R2D.width("Theme") + 8;
            float rgbX = right - 14 - rgbW, thX = rgbX - 3 - thW;
            chip(g, "RGB", rgbX, sy + 2, rgbW, c.rainbow && !c.theme, acc);
            chip(g, "Theme", thX, sy + 2, thW, c.theme, acc);
            R2D.text(g, fit(c.name, thX - x - 4), x, sy + 4, label);
            float hy = sy + 19, hh = 4;
            float dim = c.theme ? 0.25f : 1f;
            int seg = 6;
            for (int i = 0; i < seg; i++) {
                R2D.hGradient(g, x + w * i / seg, hy, w / seg + 0.3f, hh, 0,
                        Col.alpha(Col.hsv(i / (float) seg, c.sat, 1), dim), Col.alpha(Col.hsv((i + 1) / (float) seg, c.sat, 1), dim));
            }
            if (!c.theme) {
                float kx = x + w * c.hue;
                R2D.circle(g, kx, hy + hh / 2, 3.6f, Theme.TEXT);
                R2D.circle(g, kx, hy + hh / 2, 2.3f, col);
            }
            hit(x, hy - 3, w, hh + 6, Kind.HUE, s);
            hit(thX, sy + 1, thW, 13, Kind.THEME, s);
            hit(rgbX, sy + 1, rgbW, 13, Kind.RGB, s);
        }
    }

    private static void chip(GuiGraphics g, String t, float cx, float cy, float w, boolean on, int acc) {
        float h = 11;
        R2D.rect(g, cx, cy, w, h, 3, on ? acc : Theme.TRACK);
        R2D.text(g, t, cx + (w - R2D.width(t)) / 2, cy + 1.7f, on ? Theme.onAccent() : Theme.MUTED);
    }

    private void bind(ClickGui gui, GuiGraphics g, Module m, float x, float sy, float right, int acc) {
        String mode = Module.BIND_MODES[Math.max(0, Math.min(2, m.bindMode))];
        float cw = R2D.width(mode) + 8;
        chip(g, mode, right - cw, sy + 3, cw, m.bindMode != 0, acc);
        boolean listening = gui.listening == m;
        String k = listening ? "Press a key..." : keyName(m.key);
        int kc = listening ? Col.alpha(acc, 0.5f + 0.5f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 260.0)))
                : m.key >= 0 ? Theme.TEXT : Theme.DIM;
        float kw = R2D.width(k) + 8;
        float kx = right - cw - 4 - kw;
        R2D.rect(g, kx, sy + 3, kw, 11, 3, listening ? Col.alpha(acc, 0.15f) : Theme.TRACK);
        R2D.text(g, k, kx + 4, sy + 4.7f, kc);
        R2D.text(g, "Keybind", x, sy + 4.7f, 0xFFA9A9AF);
        hit(kx, sy + 2, kw, 13, Kind.BIND, null);
        hit(right - cw, sy + 2, cw, 13, Kind.BIND_MODE, null);
    }

    boolean click(ClickGui gui, Module m, float mx, float my, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(mx, my)) continue;
            switch (h.kind()) {
                case BOOL -> {
                    BoolSetting b = (BoolSetting) h.setting();
                    b.value = !b.value;
                    gui.click(b.value);
                }
                case NUMBER -> {
                    gui.slider = (NumberSetting) h.setting();
                    gui.sliderX = h.x() + 4;
                    gui.sliderW = h.w() - 8;
                    gui.dragSlider(mx);
                }
                case MODE_PREV -> {
                    ((ModeSetting) h.setting()).cycle(button == 1 ? 1 : -1);
                    gui.click(true);
                }
                case MODE_NEXT -> {
                    ((ModeSetting) h.setting()).cycle(button == 1 ? -1 : 1);
                    gui.click(true);
                }
                case HUE -> {
                    ColorSetting c = (ColorSetting) h.setting();
                    c.theme = false;
                    gui.hue = c;
                    gui.hueX = h.x();
                    gui.hueW = h.w();
                    gui.dragHue(mx);
                }
                case THEME -> {
                    ColorSetting c = (ColorSetting) h.setting();
                    c.theme = !c.theme;
                    gui.click(c.theme);
                }
                case RGB -> {
                    ColorSetting c = (ColorSetting) h.setting();
                    c.rainbow = c.theme || !c.rainbow;
                    c.theme = false;
                    gui.click(c.rainbow);
                }
                case BIND -> {
                    gui.listening = gui.listening == m ? null : m;
                    gui.click(true);
                }
                case BIND_MODE -> {
                    m.bindMode = (m.bindMode + 1) % 3;
                    if (m.bindMode == 2) m.setEnabled(true);
                    if (m.bindMode == 1) m.setEnabled(false);
                    gui.click(m.bindMode != 0);
                }
                case CHIP -> {
                    MultiSetting ms = (MultiSetting) h.setting();
                    ms.toggle(h.opt());
                    gui.click(ms.has(h.opt()));
                }
                case RESET -> {
                    m.resetSettings();
                    gui.click(false);
                }
            }
            return true;
        }
        return false;
    }
}
