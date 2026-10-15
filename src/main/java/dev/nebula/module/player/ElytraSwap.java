package dev.nebula.module.player;

import dev.nebula.gui.Notifications;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;

/** One-shot: swaps your chestplate and elytra (whichever you're not wearing) then turns itself off. Bind it to a key. */
public final class ElytraSwap extends Module {
    public ElytraSwap() {
        super("Elytra Swap", "Swap between elytra and chestplate.", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        var p = mc().player;
        if (p != null && mc().gameMode != null) {
            boolean wearingElytra = p.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElytraItem;
            for (int i = 0; i < 36; i++) {
                var st = p.getInventory().getItem(i);
                boolean match = wearingElytra ? st.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.CHEST : st.getItem() instanceof ElytraItem;
                if (!match) continue;
                int menu = i < 9 ? 36 + i : i;
                int id = p.inventoryMenu.containerId;
                mc().gameMode.handleInventoryMouseClick(id, menu, 0, ClickType.PICKUP, p);
                mc().gameMode.handleInventoryMouseClick(id, 6, 0, ClickType.PICKUP, p); // chest armor slot
                mc().gameMode.handleInventoryMouseClick(id, menu, 0, ClickType.PICKUP, p);
                break;
            }
        }
        setEnabled(false);
    }

    @Override
    public boolean listed() {
        return false;
    }
}
