package dev.nebula.module.hud;

/** Kills, deaths and ratio this session. */
public final class KdHud extends StatHud {
    public KdHud() {
        super("K/D", "Kills, deaths and ratio this session.", 1.0f, 0.54f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "K/D";
    }

    @Override
    protected String value(boolean editor) {
        int k = dev.nebula.util.Combat.kills, d = dev.nebula.util.Combat.deaths; return k + "/" + d + String.format(" (%.2f)", d == 0 ? (double) k : (double) k / d);
    }
}
