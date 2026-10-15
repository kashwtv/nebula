package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;

/** Faster movement. "Ground" boosts walking; "Hop" auto-jumps and keeps air speed. */
public final class Speed extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Ground", "Ground", "Hop"));
    public final NumberSetting multiplier = add(new NumberSetting("Speed", 1.05, 2.5, 1.3, 0.05, "x"));

    public Speed() {
        super("Speed", "Move faster than normal.", Category.MOVEMENT);
    }

    @Override
    public String suffix() {
        return mode.get();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || p.isInWater() || p.isInLava() || p.onClimbable() || p.isPassenger() || p.getAbilities().flying) return;
        var in = p.input;
        float fwd = in.forwardImpulse, str = in.leftImpulse;
        if (fwd == 0 && str == 0) return;
        double yaw = Math.toRadians(p.getYRot());
        double len = Math.sqrt(fwd * fwd + str * str);
        double dx = (-Math.sin(yaw) * fwd + Math.cos(yaw) * str) / len;
        double dz = (Math.cos(yaw) * fwd + Math.sin(yaw) * str) / len;
        double base = p.isSprinting() ? 0.2806 : 0.2158;
        var v = p.getDeltaMovement();
        if (mode.is("Hop")) {
            if (p.onGround()) p.jumpFromGround();
            double s = base * multiplier.value;
            p.setDeltaMovement(dx * s, p.getDeltaMovement().y, dz * s);
        } else if (p.onGround()) {
            double s = base * multiplier.value;
            p.setDeltaMovement(dx * s, v.y, dz * s);
        }
    }
}
