package dev.nebula.module.hud;

/** How many in-game days the world has run. */
public final class DayHud extends StatHud {
    public DayHud() {
        super("Day Counter", "How many in-game days the world has run.", 0.0f, 0.72f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Day";
    }

    @Override
    protected String value(boolean editor) {
        return mc().level == null ? "1" : String.valueOf(mc().level.getDayTime() / 24000L + 1);
    }
}
