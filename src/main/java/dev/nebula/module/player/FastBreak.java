package dev.nebula.module.player;

import dev.nebula.Nebula;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/** Removes the 5-tick pause between breaking one block and starting the next. */
public final class FastBreak extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 4, 0, 1, "t"));

    private static Field destroyDelay;

    public FastBreak() {
        super("Fast Break", "No cooldown between breaking blocks.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc().gameMode == null) return;
        try {
            if (destroyDelay == null) destroyDelay = ObfuscationReflectionHelper.findField(MultiPlayerGameMode.class, "f_105195_");
            int cur = destroyDelay.getInt(mc().gameMode);
            if (cur > delay.i()) destroyDelay.setInt(mc().gameMode, delay.i());
        } catch (Throwable t) {
            Nebula.LOG.warn("Fast Break failed", t);
            setEnabled(false);
        }
    }
}
