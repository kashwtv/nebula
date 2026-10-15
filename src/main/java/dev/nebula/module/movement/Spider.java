package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;

/**
 * Climb straight up walls by walking into them. Climbing eases up to speed, and
 * holding sneak against a wall makes you cling in place like on a ladder.
 */
public final class Spider extends Module {
    public final NumberSetting speed = add(new NumberSetting("Climb Speed", 0.1, 0.6, 0.2, 0.05));
    public final NumberSetting acceleration = add(new NumberSetting("Acceleration", 0.1, 1, 0.5, 0.05));
    public final BoolSetting sneakHold = add(new BoolSetting("Sneak To Cling", true));

    public Spider() {
        super("Spider", "Climb walls like a spider.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || !p.horizontalCollision || p.getAbilities().flying || p.onClimbable()) return;
        var v = p.getDeltaMovement();
        if (sneakHold.get() && p.isShiftKeyDown()) {
            p.setDeltaMovement(v.x, 0, v.z);
            p.fallDistance = 0;
            return;
        }
        if (p.input.forwardImpulse == 0 && p.input.leftImpulse == 0) return;
        double vy = Math.max(v.y, 0) + (speed.value - Math.max(v.y, 0)) * acceleration.value;
        p.setDeltaMovement(v.x, vy, v.z);
        p.fallDistance = 0;
    }
}
