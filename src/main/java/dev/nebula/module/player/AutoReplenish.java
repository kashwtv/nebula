package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/** Refills hotbar stacks from your inventory when they run low. */
public final class AutoReplenish extends Module {
    public final NumberSetting threshold = add(new NumberSetting("Threshold", 1, 32, 8, 1));

    private int cooldown;

    public AutoReplenish() {
        super("Auto Replenish", "Refills hotbar stacks from inventory.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (cooldown-- > 0 || mc().screen != null && !(mc().screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        var inv = p.getInventory();
        for (int hb = 0; hb < 9; hb++) {
            ItemStack st = inv.getItem(hb);
            if (st.isEmpty() || !st.isStackable() || st.getCount() > threshold.value) continue;
            for (int i = 9; i < 36; i++) {
                ItemStack src = inv.getItem(i);
                if (!ItemStack.isSameItemSameTags(st, src)) continue;
                int menuSlot = i; // inventory slots 9-35 match the player menu's slot ids
                mc().gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, menuSlot, 0, ClickType.PICKUP, p);
                mc().gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, 36 + hb, 0, ClickType.PICKUP, p);
                if (!p.inventoryMenu.getCarried().isEmpty())
                    mc().gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, menuSlot, 0, ClickType.PICKUP, p);
                cooldown = 4;
                return;
            }
        }
    }
}
