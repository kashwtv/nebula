package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;

/** Caps your fall speed so long drops become a gentle float (like slow falling). */
public final class Parachute extends Module {
    public final NumberSetting maxFall = add(new NumberSetting("Max Fall Speed", 0.05, 1, 0.2, 0.05));
    public final NumberSetting after = add(new NumberSetting("Open After", 0, 10, 3, 0.5, "m"));

    public Parachute() {
        super("Parachute", "Fall slowly.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var v = p.getDeltaMovement();
        if (p.onGround() || p.isFallFlying() || p.getAbilities().flying || p.isShiftKeyDown() || p.fallDistance < after.value) return;
        if (v.y < -maxFall.value) p.setDeltaMovement(v.x, -maxFall.value, v.z);
    }
}
