package dev.nebula.module.hud;

import dev.nebula.util.Col;
import dev.nebula.util.Combat;

public final class ReachHud extends StatHud {
    public ReachHud() {
        super("Reach Display", "Distance of your last hit.", 0.0f, 0.54f);
        valueColor.index = 2; // Status
    }

    @Override
    protected String label() {
        return "Reach";
    }

    @Override
    protected String value(boolean editor) {
        return Combat.hits == 0 ? "-" : String.format("%.2fm", Combat.lastReach);
    }

    @Override
    protected int statusColor() {
        return Col.health((float) Math.min(1, Combat.lastReach / 3.0));
    }
}
