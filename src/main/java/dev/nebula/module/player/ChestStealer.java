package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;

import java.util.concurrent.ThreadLocalRandom;

/** Shift-clicks everything out of chests, barrels and shulkers you open. */
public final class ChestStealer extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 10, 2, 1, "t"));
    public final BoolSetting randomize = add(new BoolSetting("Random Delay", true));
    public final BoolSetting autoClose = add(new BoolSetting("Close When Done", true));

    private int cooldown;

    public ChestStealer() {
        super("Chest Stealer", "Takes everything out of containers you open.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null) return;
        if (!(mc().screen instanceof AbstractContainerScreen<?> scr)) return;
        var menu = scr.getMenu();
        if (!(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu || menu instanceof HopperMenu)) return;
        if (cooldown > 0) { cooldown--; return; }
        for (Slot s : menu.slots) {
            if (s.container == p.getInventory() || !s.hasItem()) continue;
            mc().gameMode.handleInventoryMouseClick(menu.containerId, s.index, 0, ClickType.QUICK_MOVE, p);
            cooldown = delay.i() + (randomize.get() ? ThreadLocalRandom.current().nextInt(0, 2) : 0);
            return;
        }
        if (autoClose.get()) p.closeContainer();
    }
}
