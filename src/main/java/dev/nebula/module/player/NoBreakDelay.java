package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.util.Reflect;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

/** Removes the 5-tick pause between breaking one block and starting the next. */
public final class NoBreakDelay extends Module {
    public NoBreakDelay() {
        super("No Break Delay", "Start the next block instantly.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc().gameMode != null) Reflect.setInt(MultiPlayerGameMode.class, mc().gameMode, "f_105195_", 0); // destroyDelay
    }
}
