package dev.nebula.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Small movement helpers shared by the movement modules. */
public final class Move {
    /** Vanilla horizontal walk/sprint speeds per tick on flat ground. */
    public static final double WALK = 0.2158, SPRINT = 0.2806;

    private Move() {}

    public static boolean moving(LocalPlayer p) {
        return p.input.forwardImpulse != 0 || p.input.leftImpulse != 0;
    }

    /** Unit vector (x, z) of where the movement keys point, or {0, 0} when idle. */
    public static double[] dir(LocalPlayer p) {
        float fwd = p.input.forwardImpulse, str = p.input.leftImpulse;
        double len = Math.sqrt(fwd * fwd + str * str);
        if (len < 1e-4) return new double[]{0, 0};
        double yaw = Math.toRadians(p.getYRot());
        return new double[]{(-Math.sin(yaw) * fwd + Math.cos(yaw) * str) / len, (Math.cos(yaw) * fwd + Math.sin(yaw) * str) / len};
    }

    public static double hSpeed(LocalPlayer p) {
        Vec3 v = p.getDeltaMovement();
        return Math.sqrt(v.x * v.x + v.z * v.z);
    }

    /** Sets horizontal velocity to {@code speed} along the input direction, keeping vertical motion. */
    public static void setSpeed(LocalPlayer p, double speed) {
        double[] d = dir(p);
        p.setDeltaMovement(d[0] * speed, p.getDeltaMovement().y, d[1] * speed);
    }

    /** Eases horizontal velocity toward {@code speed} along the input direction. k = 0..1 blend per tick. */
    public static void easeSpeed(LocalPlayer p, double speed, double k) {
        double[] d = dir(p);
        Vec3 v = p.getDeltaMovement();
        p.setDeltaMovement(v.x + (d[0] * speed - v.x) * k, v.y, v.z + (d[1] * speed - v.z) * k);
    }

    /** Base speed accounting for sprint and the Speed potion. */
    public static double base(LocalPlayer p) {
        double b = p.isSprinting() ? SPRINT : WALK;
        var eff = p.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED);
        if (eff != null) b *= 1 + 0.2 * (eff.getAmplifier() + 1);
        return b;
    }
}
