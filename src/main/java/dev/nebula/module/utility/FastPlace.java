package dev.nebula.module.utility;

import dev.nebula.Nebula;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/** Cuts the 4-tick right-click delay so you can place and use items faster. */
public final class FastPlace extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 3, 0, 1, "t"));
    public final BoolSetting blocks = add(new BoolSetting("Blocks", true));
    public final BoolSetting crystals = add(new BoolSetting("Crystals", true));
    public final BoolSetting xp = add(new BoolSetting("XP Bottles", true));
    public final BoolSetting everything = add(new BoolSetting("Everything", false));

    private static Field rightClickDelay;

    public FastPlace() {
        super("Fast Place", "Removes the right-click cooldown for placing blocks.", Category.PLAYER);
    }

    @Override
    public String suffix() {
        return delay.i() + "t";
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null) return;
        var item = p.getMainHandItem().getItem();
        boolean ok = everything.get()
                || (blocks.get() && item instanceof BlockItem)
                || (crystals.get() && item instanceof EndCrystalItem)
                || (xp.get() && item instanceof ExperienceBottleItem);
        if (!ok) return;
        try {
            if (rightClickDelay == null) rightClickDelay = ObfuscationReflectionHelper.findField(Minecraft.class, "f_91011_");
            int cur = rightClickDelay.getInt(mc());
            if (cur > delay.i()) rightClickDelay.setInt(mc(), delay.i());
        } catch (Throwable t) {
            Nebula.LOG.warn("Fast Place failed", t);
            setEnabled(false);
        }
    }
}
