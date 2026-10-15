package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;

/** Climb straight up walls by walking into them. */
public final class Spider extends Module {
    public final NumberSetting speed = add(new NumberSetting("Climb Speed", 0.1, 0.6, 0.2, 0.05));

    public Spider() {
        super("Spider", "Climb walls like a spider.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || !p.horizontalCollision) return;
        if (p.input.forwardImpulse == 0 && p.input.leftImpulse == 0) return;
        var v = p.getDeltaMovement();
        p.setDeltaMovement(v.x, speed.value, v.z);
        p.fallDistance = 0;
    }
}
