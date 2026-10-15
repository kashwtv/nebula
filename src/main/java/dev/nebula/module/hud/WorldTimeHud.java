package dev.nebula.module.hud;

/** The in-game clock. */
public final class WorldTimeHud extends StatHud {
    public WorldTimeHud() {
        super("World Time", "The in-game clock.", 0.0f, 0.76f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Time";
    }

    @Override
    protected String value(boolean editor) {
        if (mc().level == null) return "12:00"; long t = (mc().level.getDayTime() + 6000) % 24000; return String.format("%02d:%02d", t / 1000, (t % 1000) * 60 / 1000);
    }
}
