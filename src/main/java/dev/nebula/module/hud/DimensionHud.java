package dev.nebula.module.hud;

/** Which dimension you're in. */
public final class DimensionHud extends StatHud {
    public DimensionHud() {
        super("Dimension", "Which dimension you're in.", 0.0f, 0.56f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Dim";
    }

    @Override
    protected String value(boolean editor) {
        if (mc().level == null) return "Overworld"; return pretty(mc().level.dimension().location().getPath());
    }
}
