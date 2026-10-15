package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.world.phys.HitResult;

/** Holds left click on whatever block you're looking at, for long mining or AFK digging. */
public final class AutoMine extends Module {
    public AutoMine() {
        super("Auto Mine", "Keeps mining the block you look at.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        boolean block = mc().hitResult != null && mc().hitResult.getType() == HitResult.Type.BLOCK && mc().screen == null;
        mc().options.keyAttack.setDown(block);
    }

    @Override
    public void onDisable() {
        mc().options.keyAttack.setDown(false);
    }
}
