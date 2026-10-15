package dev.nebula.module.hud;

/** Tonight's moon phase. */
public final class MoonHud extends StatHud {
    public MoonHud() {
        super("Moon Phase", "Tonight's moon phase.", 0.0f, 0.8f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Moon";
    }

    @Override
    protected String value(boolean editor) {
        if (mc().level == null) return "Full"; String[] n = {"Full", "Waning Gibbous", "Last Quarter", "Waning Crescent", "New", "Waxing Crescent", "First Quarter", "Waxing Gibbous"}; return n[mc().level.getMoonPhase()];
    }
}
