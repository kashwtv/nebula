package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.ModeSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Targets;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Swings for you when your crosshair is on a target, but only once the weapon's cooldown is
 * charged to the level you set for that weapon type, so every hit lands at full damage.
 */
public final class Triggerbot extends Module {
    public final NumberSetting swordCharge = add(new NumberSetting("Sword Charge", 0.5, 1, 0.95, 0.01, ""));
    public final NumberSetting axeCharge = add(new NumberSetting("Axe Charge", 0.5, 1, 1.0, 0.01, ""));
    public final NumberSetting otherCharge = add(new NumberSetting("Other Charge", 0.3, 1, 0.9, 0.01, ""));
    public final ModeSetting crits = add(new ModeSetting("Crits", "Smart", "Off", "Smart", "Only Crits"));
    public final NumberSetting minDelay = add(new NumberSetting("Min Delay", 0, 300, 20, 5, "ms"));
    public final NumberSetting maxDelay = add(new NumberSetting("Max Delay", 0, 400, 70, 5, "ms"));
    public final BoolSetting weaponOnly = add(new BoolSetting("Weapon Only", false));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting passives = add(new BoolSetting("Passive Mobs", true));
    public final BoolSetting invisibles = add(new BoolSetting("Invisibles", false));
    public final BoolSetting pauseWhileUsing = add(new BoolSetting("Pause While Using", true));

    private long fireAt;

    public Triggerbot() {
        super("Triggerbot", "Auto-hits at perfect cooldown timing per weapon. No spam.", Category.COMBAT);
    }

    @Override
    public String suffix() {
        return crits.is("Off") ? null : crits.get();
    }

    private enum Weapon { SWORD, AXE, OTHER }

    private static Weapon weapon(ItemStack st) {
        if (st.getItem() instanceof SwordItem) return Weapon.SWORD;
        if (st.getItem() instanceof AxeItem) return Weapon.AXE;
        return Weapon.OTHER;
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null || mc().screen != null) { fireAt = 0; return; }
        HitResult hr = mc().hitResult;
        if (!(hr instanceof EntityHitResult ehr)) { fireAt = 0; return; }
        Entity target = ehr.getEntity();
        if (!Targets.valid(target, players.get(), hostiles.get(), passives.get(), invisibles.get())) { fireAt = 0; return; }
        if (pauseWhileUsing.get() && p.isUsingItem()) return;

        ItemStack held = p.getMainHandItem();
        Weapon w = weapon(held);
        boolean isWeapon = w != Weapon.OTHER || held.getItem() instanceof TridentItem;
        if (weaponOnly.get() && !isWeapon) return;

        double need = switch (w) {
            case SWORD -> swordCharge.value;
            case AXE -> axeCharge.value;
            default -> otherCharge.value;
        };
        if (p.getAttackStrengthScale(0.5f) < need) return;

        // Crit timing: a crit needs falling, not on ground, not in water/ladder, not blind.
        boolean canCrit = !p.onClimbable() && !p.isInWater() && !p.isPassenger() && !p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        boolean falling = !p.onGround() && p.fallDistance > 0.0f && p.getDeltaMovement().y < 0;
        boolean rising = !p.onGround() && p.getDeltaMovement().y >= 0;
        if (crits.is("Only Crits") && canCrit && !falling) return;
        if (crits.is("Smart") && canCrit && rising) return; // mid-jump: wait for the fall

        long now = System.currentTimeMillis();
        if (fireAt == 0) {
            long lo = (long) Math.min(minDelay.value, maxDelay.value), hi = (long) Math.max(minDelay.value, maxDelay.value);
            fireAt = now + (hi > lo ? ThreadLocalRandom.current().nextLong(lo, hi + 1) : lo);
        }
        if (now < fireAt) return;
        dev.nebula.module.ModuleManager.get(AutoWeapon.class).select();
        dev.nebula.module.ModuleManager.get(Criticals.class).beforeAttack();
        mc().gameMode.attack(p, target);
        p.swing(InteractionHand.MAIN_HAND);
        fireAt = 0;
    }
}
