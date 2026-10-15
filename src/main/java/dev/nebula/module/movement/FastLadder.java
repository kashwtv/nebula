package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;

/** Climb ladders, vines and scaffolding faster, up and down. Sneak still holds you in place. */
public final class FastLadder extends Module {
    public final NumberSetting up = add(new NumberSetting("Up Speed", 0.12, 0.6, 0.3, 0.02));
    public final NumberSetting down = add(new NumberSetting("Down Speed", 0.15, 0.8, 0.3, 0.02));

    public FastLadder() {
        super("Fast Ladder", "Climb ladders faster.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || !p.onClimbable() || p.getAbilities().flying || p.isShiftKeyDown()) return;
        var v = p.getDeltaMovement();
        if (p.horizontalCollision || p.input.jumping) p.setDeltaMovement(v.x, up.value, v.z);
        else if (!p.onGround() && v.y < 0 && !dev.nebula.util.Move.moving(p)) p.setDeltaMovement(v.x, -down.value, v.z);
        p.fallDistance = 0;
    }
}
