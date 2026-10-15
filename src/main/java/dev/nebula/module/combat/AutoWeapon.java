package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.ModeSetting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Switches to your best weapon in the hotbar the moment you swing at something. */
public final class AutoWeapon extends Module {
    public final ModeSetting prefer = add(new ModeSetting("Prefer", "Damage", "Damage", "Sword", "Axe"));

    public AutoWeapon() {
        super("Auto Weapon", "Swaps to your best hotbar weapon when you attack.", Category.COMBAT);
    }

    @SubscribeEvent
    public void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (isEnabled() && e.isAttack() && mc().hitResult instanceof EntityHitResult) select();
    }

    /** Also called by Triggerbot right before it swings. */
    public void select() {
        var p = mc().player;
        if (!isEnabled() || p == null) return;
        int best = -1;
        double bestScore = score(p.getMainHandItem());
        for (int i = 0; i < 9; i++) {
            double s = score(p.getInventory().getItem(i));
            if (s > bestScore + 0.01) { bestScore = s; best = i; }
        }
        if (best >= 0) p.getInventory().selected = best;
    }

    private double score(ItemStack st) {
        if (st.isEmpty()) return 0;
        double dmg = 0;
        for (AttributeModifier m : st.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) dmg += m.getAmount();
        dmg += EnchantmentHelper.getDamageBonus(st, MobType.UNDEFINED);
        if (prefer.is("Sword") && st.getItem() instanceof SwordItem) dmg += 100;
        if (prefer.is("Axe") && st.getItem() instanceof AxeItem) dmg += 100;
        if (st.isDamageableItem() && st.getMaxDamage() - st.getDamageValue() < 3) dmg -= 50;
        return dmg;
    }
}
