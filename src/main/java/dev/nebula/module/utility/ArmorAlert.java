package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.EnumSet;
import java.util.Set;

/** Warns you (action bar + sound) when a worn armor piece drops below a durability percentage. */
public final class ArmorAlert extends Module {
    public final NumberSetting percent = add(new NumberSetting("Warn Below", 5, 50, 15, 5, "%"));

    private final Set<EquipmentSlot> warned = EnumSet.noneOf(EquipmentSlot.class);

    public ArmorAlert() {
        super("Armor Alert", "Warns when armor is about to break.", Category.CLIENT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var st = p.getItemBySlot(s);
            if (!st.isDamageableItem()) { warned.remove(s); continue; }
            double left = 100.0 * (st.getMaxDamage() - st.getDamageValue()) / st.getMaxDamage();
            if (left < percent.value && warned.add(s)) {
                p.displayClientMessage(Component.literal(st.getHoverName().getString() + " is at " + (int) left + "%!").withStyle(ChatFormatting.RED), true);
                p.playSound(net.minecraft.sounds.SoundEvents.ITEM_BREAK, 1f, 1f);
            } else if (left >= percent.value) warned.remove(s);
        }
    }
}
