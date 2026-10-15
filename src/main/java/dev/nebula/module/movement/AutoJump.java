package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;

/** Jumps whenever you're on the ground (optionally only while moving). Holding jump also skips the jump delay. */
public final class AutoJump extends Module {
    public final BoolSetting onlyMoving = add(new BoolSetting("Only While Moving", true));

    public AutoJump() {
        super("Auto Jump", "Keeps jumping for you.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (!p.onGround() || p.isInWater() || p.isShiftKeyDown() || mc().screen != null) return;
        if (onlyMoving.get() && !dev.nebula.util.Move.moving(p)) return;
        p.jumpFromGround();
    }
}
