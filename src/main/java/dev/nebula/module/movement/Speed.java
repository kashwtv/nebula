package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Move;

/**
 * Faster movement. "Ground" boosts walking, "Hop" auto-jumps and keeps air speed,
 * "Strafe Hop" hops and lets you turn freely mid-air. Speed ramps up with Acceleration
 * instead of snapping, respects the Speed potion, and pauses while sneaking or swimming.
 */
public final class Speed extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Ground", "Ground", "Hop", "Strafe Hop"));
    public final NumberSetting multiplier = add(new NumberSetting("Speed", 1.05, 2.5, 1.3, 0.05, "x"));
    public final NumberSetting acceleration = add(new NumberSetting("Acceleration", 0.1, 1, 0.5, 0.05));
    public final NumberSetting airFriction = add(new NumberSetting("Air Friction", 0, 0.1, 0.02, 0.005));
    public final BoolSetting pauseSneak = add(new BoolSetting("Pause While Sneaking", true));
    public final BoolSetting stopOnRelease = add(new BoolSetting("Stop On Release", true));

    private double hopSpeed;

    public Speed() {
        super("Speed", "Move faster than normal.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    @Override
    public void onEnable() {
        hopSpeed = 0;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || p.isInWater() || p.isInLava() || p.onClimbable() || p.isPassenger() || p.getAbilities().flying || p.isFallFlying()) return;
        if (pauseSneak.get() && p.isShiftKeyDown()) return;
        if (!Move.moving(p)) {
            if (stopOnRelease.get() && p.onGround()) {
                var v = p.getDeltaMovement();
                p.setDeltaMovement(v.x * 0.5, v.y, v.z * 0.5);
            }
            hopSpeed = 0;
            return;
        }
        double target = Move.base(p) * multiplier.value;
        if (mode.is("Ground")) {
            if (p.onGround()) Move.easeSpeed(p, target, acceleration.value);
            return;
        }
        // Hop / Strafe Hop
        if (p.onGround()) {
            p.jumpFromGround();
            hopSpeed = Math.max(Move.hSpeed(p), hopSpeed) + (target - hopSpeed) * acceleration.value;
            Move.setSpeed(p, hopSpeed);
        } else {
            hopSpeed = Math.max(Move.base(p), hopSpeed * (1 - airFriction.value));
            if (mode.is("Strafe Hop")) Move.setSpeed(p, hopSpeed);
            else Move.easeSpeed(p, hopSpeed, 0.25);
        }
    }
}
