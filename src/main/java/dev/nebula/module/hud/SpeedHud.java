package dev.nebula.module.hud;

import dev.nebula.module.setting.ModeSetting;

public final class SpeedHud extends StatHud {
    public final ModeSetting unit = add(new ModeSetting("Unit", "b/s", "b/s", "km/h"));

    public SpeedHud() {
        super("Speed", "How fast you're moving.", 0.0f, 0.48f);
    }

    @Override
    protected String label() {
        return "Speed";
    }

    @Override
    protected String value(boolean editor) {
        var p = mc().player;
        if (p == null) return "0";
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        double bps = Math.sqrt(dx * dx + dz * dz) * 20;
        return unit.is("km/h") ? String.format("%.1f km/h", bps * 3.6) : String.format("%.2f b/s", bps);
    }
}
