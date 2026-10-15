package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import net.minecraft.world.phys.Vec3;

/** Saves you from falling into the void: bounces you back up, or stops you in place. */
public final class AntiVoid extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Bounce", "Bounce", "Freeze"));

    private Vec3 lastSafe;

    public AntiVoid() {
        super("Anti Void", "Stops you falling into the void.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p.onGround()) lastSafe = p.position();
        if (p.getY() > p.level().getMinBuildHeight() - 2 || p.getAbilities().flying) return;
        if (mode.is("Freeze")) { p.setDeltaMovement(0, 0, 0); p.fallDistance = 0; return; }
        if (lastSafe != null) {
            var d = lastSafe.subtract(p.position());
            p.setDeltaMovement(Math.max(-1, Math.min(1, d.x * 0.2)), 1.2, Math.max(-1, Math.min(1, d.z * 0.2)));
        } else p.setDeltaMovement(0, 1.2, 0);
        p.fallDistance = 0;
    }
}
