package dev.nebula.util;

import net.minecraft.util.Mth;

/** ARGB int color helpers. */
public final class Col {
    private Col() {}

    public static int alpha(int c, float a) {
        int al = Mth.clamp((int) (((c >>> 24) & 0xFF) * a), 0, 255);
        return (al << 24) | (c & 0xFFFFFF);
    }

    public static int withAlpha(int c, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (c & 0xFFFFFF);
    }

    public static int lerp(int x, int y, float t) {
        t = Ease.clamp01(t);
        int a = (int) (((x >>> 24) & 0xFF) + (((y >>> 24) & 0xFF) - ((x >>> 24) & 0xFF)) * t);
        int r = (int) (((x >> 16) & 0xFF) + (((y >> 16) & 0xFF) - ((x >> 16) & 0xFF)) * t);
        int g = (int) (((x >> 8) & 0xFF) + (((y >> 8) & 0xFF) - ((x >> 8) & 0xFF)) * t);
        int b = (int) ((x & 0xFF) + ((y & 0xFF) - (x & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int hsv(float h, float s, float v) {
        return 0xFF000000 | Mth.hsvToRgb(((h % 1f) + 1f) % 1f, s, v);
    }

    public static float r(int c) { return ((c >> 16) & 0xFF) / 255f; }
    public static float g(int c) { return ((c >> 8) & 0xFF) / 255f; }
    public static float b(int c) { return (c & 0xFF) / 255f; }
    public static float a(int c) { return ((c >>> 24) & 0xFF) / 255f; }

    /** Red -> yellow -> green for a 0..1 fraction (health, durability). */
    public static int health(float f) {
        f = Ease.clamp01(f);
        if (f < 0.5f) return lerp(0xFFFF4B4B, 0xFFFFD43B, f * 2);
        return lerp(0xFFFFD43B, 0xFF5BE37D, (f - 0.5f) * 2);
    }
}
