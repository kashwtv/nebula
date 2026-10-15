package dev.nebula.util;

public final class Ease {
    private Ease() {}

    public static float clamp01(float t) {
        return t < 0 ? 0 : (t > 1 ? 1 : t);
    }

    public static float outCubic(float t) {
        t = clamp01(t);
        float f = 1 - t;
        return 1 - f * f * f;
    }

    public static float inCubic(float t) {
        t = clamp01(t);
        return t * t * t;
    }

    public static float outExpo(float t) {
        t = clamp01(t);
        return t >= 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
    }

    public static float outBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1;
        float f = t - 1;
        return 1 + c3 * f * f * f + c1 * f * f;
    }

    public static float inOutSine(float t) {
        t = clamp01(t);
        return (float) (-(Math.cos(Math.PI * t) - 1) / 2);
    }
}
