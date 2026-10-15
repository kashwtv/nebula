package dev.nebula.module.hud;

/** How long you've been playing this session. */
public final class PlaytimeHud extends StatHud {
    public PlaytimeHud() {
        super("Playtime", "How long you've been playing this session.", 1.0f, 0.74f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "Playtime";
    }

    @Override
    protected String value(boolean editor) {
        long s = (System.currentTimeMillis() - dev.nebula.util.Combat.SESSION_START) / 1000; return String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60);
    }
}
