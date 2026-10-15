package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/** Keeps a totem in your offhand. */
public final class AutoTotem extends Module {
    public final ModeSetting mode = add(new ModeSetting("Mode", "Always", "Always", "Low Health"));
    public final NumberSetting health = add(new NumberSetting("Health", 2, 20, 10, 1, " hp"));
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 10, 1, 1, "t"));

    private int cooldown;

    public AutoTotem() {
        super("Auto Totem", "Puts a totem in your offhand automatically.", Category.COMBAT);
        health.visibleWhen(() -> mode.is("Low Health"));
    }

    @Override
    public String suffix() {
        var p = mc().player;
        if (p == null) return null;
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) n += p.getInventory().getItem(i).getCount();
        return String.valueOf(n);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null || p.isCreative()) return;
        if (cooldown > 0) { cooldown--; return; }
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (mode.is("Low Health") && p.getHealth() + p.getAbsorptionAmount() > health.value) return;
        if (mc().screen != null && !(mc().screen instanceof InventoryScreen)) return;
        var inv = p.getInventory();
        for (int i = 0; i < 36; i++) {
            if (!inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;
            int menuSlot = i < 9 ? 36 + i : i;
            mc().gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, menuSlot, 40, ClickType.SWAP, p);
            cooldown = delay.i();
            return;
        }
    }
}
