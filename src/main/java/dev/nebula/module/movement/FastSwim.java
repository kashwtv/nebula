package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;

/** Swim faster in water and lava, and rise/sink quicker with jump/sneak. */
public final class FastSwim extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed", 0.1, 1, 0.3, 0.05));
    public final NumberSetting vertical = add(new NumberSetting("Vertical", 0.05, 0.5, 0.15, 0.05));

    public FastSwim() {
        super("Fast Swim", "Swim faster.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (!p.isInWater() && !p.isInLava()) return;
        if (dev.nebula.util.Move.moving(p)) dev.nebula.util.Move.easeSpeed(p, speed.value, 0.3);
        var v = p.getDeltaMovement();
        if (p.input.jumping) p.setDeltaMovement(v.x, vertical.value, v.z);
        else if (p.input.shiftKeyDown) p.setDeltaMovement(v.x, -vertical.value, v.z);
    }
}
