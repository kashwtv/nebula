package dev.nebula.module.hud;

import dev.nebula.render.Theme;

/** Shows how long the server has stopped sending world time (lag spike detector). */
public final class LagHud extends StatHud {
    private long lastGt = -1, lastChange;

    public LagHud() {
        super("Lag Detector", "Warns when the server stops responding.", 0.5f, 0.1f);
        valueColor.index = 2;
    }

    private double stall() {
        if (mc().level == null) return 0;
        long gt = mc().level.getGameTime(), now = System.currentTimeMillis();
        if (gt != lastGt) { lastGt = gt; lastChange = now; }
        return (now - lastChange) / 1000.0;
    }

    @Override
    protected String label() {
        return "Server";
    }

    @Override
    protected String value(boolean editor) {
        double s = stall();
        return s > 1 ? String.format("Lagging %.1fs", s) : "OK";
    }

    @Override
    protected int statusColor() {
        return stall() > 1 ? Theme.RED : Theme.GREEN;
    }
}
