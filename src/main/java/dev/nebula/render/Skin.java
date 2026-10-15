package dev.nebula.render;

import dev.nebula.module.ModuleManager;
import dev.nebula.module.client.HudStyleModule;
import dev.nebula.util.Col;
import net.minecraft.client.gui.GuiGraphics;

/** Shared look for HUD elements: flat plate, hairline border, soft dark shadow. */
public final class Skin {
    /** Turned off while drawing a HUD element whose background is disabled. */
    public static boolean platesOn = true;

    private Skin() {}

    private static HudStyleModule style() {
        return ModuleManager.get(HudStyleModule.class);
    }

    public static float radius() {
        HudStyleModule s = style();
        return s == null ? 3 : s.radius.f();
    }

    public static void plate(GuiGraphics g, float x, float y, float w, float h) {
        if (!platesOn) return;
        HudStyleModule s = style();
        float r = radius();
        if (s == null || s.shadow.get()) R2D.glow(g, x, y + 1, w, h, r, 6, 0x50000000);
        float op = s == null ? 0.72f : s.opacity.f();
        R2D.rect(g, x, y, w, h, r, Col.alpha(0xFF0C0C0E, op));
        if (s == null || s.border.get()) R2D.outline(g, x, y, w, h, r, R2D.px(), Theme.BORDER);
        if (s != null && s.accentLine.get()) R2D.rect(g, x + r, y, w - r * 2, R2D.px() * 1.5f, 0, Theme.accent());
    }

    /** L-shaped marks on the corners (used by ESP boxes). */
    public static void brackets(GuiGraphics g, float x, float y, float w, float h, float len, float th, int color) {
        len = Math.min(len, Math.min(w, h) / 2f);
        R2D.rect(g, x, y, len, th, 0, color);
        R2D.rect(g, x, y, th, len, 0, color);
        R2D.rect(g, x + w - len, y, len, th, 0, color);
        R2D.rect(g, x + w - th, y, th, len, 0, color);
        R2D.rect(g, x, y + h - th, len, th, 0, color);
        R2D.rect(g, x, y + h - len, th, len, 0, color);
        R2D.rect(g, x + w - len, y + h - th, len, th, 0, color);
        R2D.rect(g, x + w - th, y + h - len, th, len, 0, color);
    }

    /** Thin flat bar. */
    public static void bar(GuiGraphics g, float x, float y, float w, float h, float frac, int color) {
        frac = Math.max(0, Math.min(1, frac));
        R2D.rect(g, x, y, w, h, h / 2, Theme.TRACK);
        if (frac > 0) R2D.rect(g, x, y, Math.max(h, w * frac), h, h / 2, color);
    }

    /** Small grey label + white value pair; returns the width used. */
    public static float labelValue(GuiGraphics g, String label, String value, float x, float y, int valueColor) {
        R2D.text(g, label, x, y, Theme.MUTED);
        float lw = R2D.width(label) + 4;
        R2D.text(g, value, x + lw, y, valueColor);
        return lw + R2D.width(value);
    }
}
