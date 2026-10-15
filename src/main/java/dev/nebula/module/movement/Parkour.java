package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;

/** Jumps automatically at the very last moment before you walk off an edge. */
public final class Parkour extends Module {
    public Parkour() {
        super("Parkour", "Auto-jumps at the edge of blocks.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || !p.onGround() || p.isShiftKeyDown() || mc().options.keyJump.isDown()) return;
        var v = p.getDeltaMovement();
        if (v.x * v.x + v.z * v.z < 0.0025) return;
        var probe = p.getBoundingBox().move(v.x, -0.5, v.z).deflate(0.001, 0, 0.001);
        if (p.level().noCollision(p, probe)) p.jumpFromGround();
    }
}
