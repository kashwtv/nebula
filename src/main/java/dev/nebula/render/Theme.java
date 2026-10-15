package dev.nebula.render;

import dev.nebula.module.ModuleManager;
import dev.nebula.module.client.ClickGuiModule;
import dev.nebula.util.Col;

/**
 * Neutral near-black surfaces, off-white type, and ONE solid accent used sparingly
 * (enabled state, sliders, focus). No drifting gradients.
 */
public final class Theme {
    public static final String[] NAMES = {"Crimson", "Ice", "Mono", "Lime", "Sun", "Rose", "Rainbow"};
    private static final int[] ACCENTS = {
            0xFFF0414E,   // Crimson
            0xFF8ED3FF,   // Ice
            0xFFEDEDEF,   // Mono
            0xFFC6F135,   // Lime
            0xFFFFD43B,   // Sun
            0xFFFF7AA2,   // Rose
            0xFFF0414E,   // Rainbow (unused)
    };

    /** Swatch color for the accent picker. */
    public static int swatch(int i) {
        return ACCENTS[i];
    }

    /** Text/knob color that stays readable on top of the accent. */
    public static int onAccent() {
        int c = accent();
        double lum = 0.2126 * Col.r(c) + 0.7152 * Col.g(c) + 0.0722 * Col.b(c);
        return lum > 0.6 ? 0xFF0C0C0E : 0xFFFFFFFF;
    }

    // surfaces
    public static final int BG = 0xF20C0C0E;
    public static final int SURFACE = 0xFF131315;
    public static final int RAISED = 0xFF1A1A1D;
    public static final int BORDER = 0x16FFFFFF;
    public static final int TRACK = 0xFF2A2A2E;
    // type
    public static final int TEXT = 0xFFEDEDEF;
    public static final int MUTED = 0xFF9A9AA1;
    public static final int DIM = 0xFF6C6C73;
    public static final int ON_ACCENT = 0xFF0C0C0E;
    // states
    public static final int RED = 0xFFFF4B4B;
    public static final int GREEN = 0xFF5BE37D;
    public static final int YELLOW = 0xFFFFD43B;

    private Theme() {}

    private static int index() {
        ClickGuiModule gui = ModuleManager.get(ClickGuiModule.class);
        return gui == null ? 0 : gui.theme.index;
    }

    public static boolean rainbow() {
        return index() == NAMES.length - 1;
    }

    /** The accent. Offset only matters for the Rainbow theme. */
    public static int accent(float offset) {
        if (rainbow()) {
            double t = System.currentTimeMillis() / 1000.0;
            return Col.hsv((float) (t * 0.06 + offset * 0.08), 0.55f, 1f);
        }
        return ACCENTS[index()];
    }

    public static int accent() {
        return accent(0);
    }
}
