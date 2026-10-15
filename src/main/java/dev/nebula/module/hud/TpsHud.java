package dev.nebula.module.hud;

/** Estimated server ticks per second from world time. */
public final class TpsHud extends StatHud {
    private long lastMs, lastGt;
    private double tps = 20;

    public TpsHud() {
        super("TPS", "Estimated server ticks per second from world time.", 0.0f, 0.52f);
    }

    private static String pretty(String s) {
        StringBuilder b = new StringBuilder();
        for (String w : s.split("_")) if (!w.isEmpty()) b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        return b.toString();
    }

    @Override
    protected String label() {
        return "TPS";
    }

    @Override
    protected String value(boolean editor) {
        if (mc().level == null) return "20.0"; long now = System.currentTimeMillis(), gt = mc().level.getGameTime(); if (lastMs == 0) { lastMs = now; lastGt = gt; } if (now - lastMs >= 2000) { tps = Math.min(20, (gt - lastGt) * 1000.0 / (now - lastMs)); lastMs = now; lastGt = gt; } return String.format("%.1f", tps);
    }
}
