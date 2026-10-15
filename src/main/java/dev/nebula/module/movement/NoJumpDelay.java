package dev.nebula.module.movement;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.util.Reflect;
import net.minecraft.world.entity.LivingEntity;

/** Removes the 10-tick delay between jumps while you hold the jump key. */
public final class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super("No Jump Delay", "Jump again the instant you land.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Reflect.setInt(LivingEntity.class, mc().player, "f_20954_", 0); // noJumpDelay
    }
}
