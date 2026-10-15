package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Move;

/**
 * Full air control: your current speed is turned toward whatever direction you're pressing,
 * so you can change direction mid-jump without losing momentum.
 */
public final class Strafe extends Module {
    public final NumberSetting control = add(new NumberSetting("Control", 0.1, 1, 1, 0.05));
    public final BoolSetting onlyAir = add(new BoolSetting("Only In Air", true));
    public final BoolSetting stopOnRelease = add(new BoolSetting("Stop On Release", false));

    public Strafe() {
        super("Strafe", "Change direction freely in the air.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || p.isInWater() || p.isInLava() || p.getAbilities().flying || p.isFallFlying() || p.isPassenger() || p.onClimbable()) return;
        if (onlyAir.get() && p.onGround()) return;
        if (!Move.moving(p)) {
            if (stopOnRelease.get() && !p.onGround()) {
                var v = p.getDeltaMovement();
                p.setDeltaMovement(v.x * 0.5, v.y, v.z * 0.5);
            }
            return;
        }
        Move.easeSpeed(p, Move.hSpeed(p), control.value);
    }
}
