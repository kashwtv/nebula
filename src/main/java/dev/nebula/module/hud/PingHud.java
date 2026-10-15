package dev.nebula.module.hud;

import dev.nebula.util.Col;

public final class PingHud extends StatHud {
    public PingHud() {
        super("Ping", "Your latency to the server.", 0.0f, 0.42f);
        valueColor.index = 2; // Status
    }

    private int ping() {
        var c = mc().getConnection();
        var p = mc().player;
        if (c == null || p == null) return 0;
        var info = c.getPlayerInfo(p.getUUID());
        return info == null ? 0 : info.getLatency();
    }

    @Override
    protected String label() {
        return "Ping";
    }

    @Override
    protected String value(boolean editor) {
        return ping() + "ms";
    }

    @Override
    protected int statusColor() {
        return Col.health(1 - Math.min(1, ping() / 250f));
    }
}
