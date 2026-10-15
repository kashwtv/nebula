package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.ModuleManager;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Move;

/** Circles around Kill Aura's target at a set distance; A/D switch direction. */
public final class TargetStrafe extends Module {
    public final NumberSetting radius = add(new NumberSetting("Radius", 1, 5, 2.5, 0.25, "m"));
    public final BoolSetting onlyJumping = add(new BoolSetting("Only While Jump Held", false));

    private int dir = 1;

    public TargetStrafe() {
        super("Target Strafe", "Circle around your Kill Aura target.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var t = ModuleManager.get(KillAura.class).target();
        if (t == null || (onlyJumping.get() && !mc().options.keyJump.isDown()) || p.isInWater()) return;
        if (mc().options.keyLeft.isDown()) dir = 1;
        if (mc().options.keyRight.isDown()) dir = -1;
        if (p.horizontalCollision) dir = -dir;
        double dx = p.getX() - t.getX(), dz = p.getZ() - t.getZ();
        double ang = Math.atan2(dz, dx) + dir * 0.35;
        double tx = t.getX() + Math.cos(ang) * radius.value, tz = t.getZ() + Math.sin(ang) * radius.value;
        double mx = tx - p.getX(), mz = tz - p.getZ(), len = Math.sqrt(mx * mx + mz * mz);
        if (len < 1e-3) return;
        double s = Math.min(Move.base(p), len);
        var v = p.getDeltaMovement();
        p.setDeltaMovement(mx / len * s, v.y, mz / len * s);
    }
}
