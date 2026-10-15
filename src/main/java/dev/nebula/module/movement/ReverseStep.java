package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;

/** Snaps you down short drops instead of floating off them. */
public final class ReverseStep extends Module {
    public final NumberSetting height = add(new NumberSetting("Max Drop", 0.5, 3, 1.5, 0.5, "m"));

    private boolean wasGround;

    public ReverseStep() {
        super("Reverse Step", "Drop down ledges instantly.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (wasGround && !p.onGround() && p.getDeltaMovement().y <= 0 && !p.input.jumping && !p.isInWater() && !p.getAbilities().flying) {
            var probe = p.getBoundingBox().move(0, -height.value, 0);
            if (!p.level().noCollision(p, probe.setMinY(p.getY() - height.value))) {
                var v = p.getDeltaMovement();
                p.setDeltaMovement(v.x, -1.0, v.z);
            }
        }
        wasGround = p.onGround();
    }
}
