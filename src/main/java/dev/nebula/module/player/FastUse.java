package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Reflect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;

/** Removes the right-click delay for using items (snowballs, eggs, pearls, rockets...). Blocks are Fast Place's job. */
public final class FastUse extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 4, 0, 1, "t"));

    public FastUse() {
        super("Fast Use", "No right-click delay for items.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc().player.getMainHandItem().getItem() instanceof BlockItem) return;
        Object cur = Reflect.get(Minecraft.class, mc(), "f_91011_");
        if (cur instanceof Integer i && i > delay.i()) Reflect.setInt(Minecraft.class, mc(), "f_91011_", delay.i());
    }
}
