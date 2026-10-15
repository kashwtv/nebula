package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;

/** Jumps automatically at the very last moment before you walk off an edge. */
public final class Parkour extends Module {
    public final NumberSetting edge = add(new NumberSetting("Edge Lead", 0.5, 2, 1, 0.1, "x"));
    public final NumberSetting minDrop = add(new NumberSetting("Min Drop", 0.5, 3, 0.6, 0.1, "m"));
    public final BoolSetting onlySprint = add(new BoolSetting("Only Sprinting", false));

    public Parkour() {
        super("Parkour", "Auto-jumps at the edge of blocks.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || !p.onGround() || p.isShiftKeyDown() || mc().options.keyJump.isDown() || p.isInWater()) return;
        if (!dev.nebula.util.Move.moving(p) || (onlySprint.get() && !p.isSprinting())) return;
        var v = p.getDeltaMovement();
        if (v.x * v.x + v.z * v.z < 0.0025) return;
        // where we'll be next tick (scaled by Edge Lead): nothing below within Min Drop -> jump now
        var probe = p.getBoundingBox().move(v.x * edge.value, -minDrop.value, v.z * edge.value).deflate(0.001, 0, 0.001)
                .setMinY(p.getY() - minDrop.value).setMaxY(p.getY() - 0.01);
        if (p.level().noCollision(p, probe)) p.jumpFromGround();
    }
}
