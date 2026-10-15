package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Prints where you died in your own chat (only you see it). */
public final class DeathCoords extends Module {
    private boolean wasDead;

    public DeathCoords() {
        super("Death Coords", "Tells you where you died.", Category.CLIENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        boolean dead = p.isDeadOrDying();
        if (dead && !wasDead) {
            p.displayClientMessage(Component.literal(String.format("You died at %d, %d, %d (%s)", p.getBlockX(), p.getBlockY(), p.getBlockZ(),
                    p.level().dimension().location().getPath())).withStyle(ChatFormatting.RED), false);
        }
        wasDead = dead;
    }
}
