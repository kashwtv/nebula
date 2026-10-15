package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Throws XP bottles at your feet to mend damaged armor. */
public final class AutoXp extends Module {
    public final NumberSetting below = add(new NumberSetting("Repair Below", 10, 100, 80, 5, "%"));
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 10, 2, 1, "t"));
    public final BoolSetting onlySneaking = add(new BoolSetting("Only While Sneaking", true));

    private int cooldown;

    public AutoXp() {
        super("Auto XP", "Throws XP bottles to mend your armor.", Category.COMBAT);
    }

    private boolean needsRepair() {
        var p = mc().player;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = p.getItemBySlot(s);
            if (st.isDamageableItem() && 100.0 * (st.getMaxDamage() - st.getDamageValue()) / st.getMaxDamage() < below.value) return true;
        }
        return false;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null || mc().getConnection() == null || mc().screen != null) return;
        if (cooldown > 0) { cooldown--; return; }
        if (onlySneaking.get() && !p.isShiftKeyDown()) return;
        if (!needsRepair()) return;
        int slot = -1;
        for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).is(Items.EXPERIENCE_BOTTLE)) slot = i;
        if (slot < 0) return;
        int prev = p.getInventory().selected;
        p.getInventory().selected = slot;
        // face straight down for the throw only (server-side), your camera doesn't move
        mc().getConnection().send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), 90f, p.onGround()));
        mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
        mc().getConnection().send(new ServerboundMovePlayerPacket.Rot(p.getYRot(), p.getXRot(), p.onGround()));
        p.getInventory().selected = prev;
        cooldown = delay.i();
    }
}
