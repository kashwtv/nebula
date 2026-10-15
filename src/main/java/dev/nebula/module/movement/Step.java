package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;

/** Walk straight up full blocks (or higher) without jumping. */
public final class Step extends Module {
    public final NumberSetting height = add(new NumberSetting("Height", 1, 2.5, 1, 0.5, "m"));

    public Step() {
        super("Step", "Walk up blocks without jumping.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return height.display();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        p.setMaxUpStep(p.isShiftKeyDown() ? 0.6f : height.f());
    }

    @Override
    public void onDisable() {
        if (mc().player != null) mc().player.setMaxUpStep(0.6f);
    }
}
