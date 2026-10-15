package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;

/**
 * Walk straight up full blocks (or higher) without jumping. Sneak to step normally.
 * Your original step height is remembered and restored when it's turned off.
 */
public final class Step extends Module {
    public final NumberSetting height = add(new NumberSetting("Height", 1, 2.5, 1, 0.5, "m"));
    public final BoolSetting onlyGround = add(new BoolSetting("Only On Ground", true));
    public final BoolSetting sneakNormal = add(new BoolSetting("Sneak Disables", true));

    private float original = 0.6f;

    public Step() {
        super("Step", "Walk up blocks without jumping.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return height.display();
    }

    @Override
    public void onEnable() {
        if (mc().player != null) original = Math.min(mc().player.maxUpStep(), 0.6f);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        boolean off = (sneakNormal.get() && p.isShiftKeyDown()) || (onlyGround.get() && !p.onGround()) || p.isInWater();
        p.setMaxUpStep(off ? original : height.f());
    }

    @Override
    public void onDisable() {
        if (mc().player != null) mc().player.setMaxUpStep(original);
    }
}
