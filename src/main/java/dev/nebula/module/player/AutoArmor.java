package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/** Puts on the best armor you're carrying. */
public final class AutoArmor extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 10, 3, 1, "t"));
    public final BoolSetting keepElytra = add(new BoolSetting("Keep Elytra", true));

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private int cooldown;

    public AutoArmor() {
        super("Auto Armor", "Equips your best armor automatically.", Category.PLAYER);
    }

    private static int armorMenuSlot(EquipmentSlot s) {
        return switch (s) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            default -> 8;
        };
    }

    private static double score(ItemStack st) {
        if (!(st.getItem() instanceof ArmorItem a)) return -1;
        double s = a.getDefense() * 10 + a.getToughness() * 4;
        s += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, st) * 3;
        s += st.isDamageableItem() ? (1 - (double) st.getDamageValue() / st.getMaxDamage()) : 1;
        if (EnchantmentHelper.hasBindingCurse(st)) s -= 1000;
        return s;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null || p.isCreative()) return;
        if (mc().screen != null && !(mc().screen instanceof InventoryScreen)) return;
        if (cooldown > 0) { cooldown--; return; }
        var inv = p.getInventory();
        int menuId = p.inventoryMenu.containerId;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack worn = p.getItemBySlot(slot);
            if (slot == EquipmentSlot.CHEST && keepElytra.get() && worn.getItem() instanceof ElytraItem) continue;
            if (EnchantmentHelper.hasBindingCurse(worn)) continue;
            double cur = worn.isEmpty() ? -1 : score(worn);
            int best = -1;
            double bestScore = cur;
            for (int i = 0; i < 36; i++) {
                ItemStack st = inv.getItem(i);
                if (!(st.getItem() instanceof ArmorItem a) || a.getEquipmentSlot() != slot) continue;
                double s = score(st);
                if (s > bestScore + 0.01) { bestScore = s; best = i; }
            }
            if (best < 0) continue;
            int from = best < 9 ? 36 + best : best;
            if (worn.isEmpty()) {
                mc().gameMode.handleInventoryMouseClick(menuId, from, 0, ClickType.QUICK_MOVE, p);
            } else {
                // pick up new piece, swap it into the armor slot, drop the old one where the new one was
                mc().gameMode.handleInventoryMouseClick(menuId, from, 0, ClickType.PICKUP, p);
                mc().gameMode.handleInventoryMouseClick(menuId, armorMenuSlot(slot), 0, ClickType.PICKUP, p);
                mc().gameMode.handleInventoryMouseClick(menuId, from, 0, ClickType.PICKUP, p);
            }
            cooldown = delay.i();
            return;
        }
    }
}
