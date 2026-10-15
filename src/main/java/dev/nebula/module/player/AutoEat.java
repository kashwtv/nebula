package dev.nebula.module.player;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.util.Combat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Eats the best food in your hotbar when you get hungry. */
public final class AutoEat extends Module {
    public final NumberSetting hunger = add(new NumberSetting("Eat Below", 2, 19, 14, 1));
    public final BoolSetting gapples = add(new BoolSetting("Allow Gapples", false));
    public final BoolSetting pauseInCombat = add(new BoolSetting("Pause In Combat", true));

    private boolean eating;
    private int previous = -1;
    private int ticks;

    public AutoEat() {
        super("Auto Eat", "Eats for you when you're hungry.", Category.PLAYER);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null || p.isCreative()) return;
        if (eating) {
            ticks++;
            if (!p.isUsingItem() && ticks > 4) {
                stop();
            } else {
                mc().options.keyUse.setDown(true);
            }
            return;
        }
        if (mc().screen != null || p.getFoodData().getFoodLevel() > hunger.value) return;
        if (pauseInCombat.get() && Combat.sinceHit() < 3000) return;
        int slot = bestFood();
        if (slot < 0) return;
        previous = p.getInventory().selected;
        p.getInventory().selected = slot;
        mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
        mc().options.keyUse.setDown(true);
        eating = true;
        ticks = 0;
    }

    private void stop() {
        var p = mc().player;
        mc().options.keyUse.setDown(false);
        if (p != null && previous >= 0) p.getInventory().selected = previous;
        previous = -1;
        eating = false;
    }

    @Override
    public void onDisable() {
        if (eating) stop();
    }

    private int bestFood() {
        var p = mc().player;
        int best = -1;
        float bestScore = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack st = p.getInventory().getItem(i);
            FoodProperties f = st.getFoodProperties(p);
            if (f == null) continue;
            if (!gapples.get() && (st.is(Items.GOLDEN_APPLE) || st.is(Items.ENCHANTED_GOLDEN_APPLE))) continue;
            if (st.is(Items.ROTTEN_FLESH) || st.is(Items.SPIDER_EYE) || st.is(Items.POISONOUS_POTATO)
                    || st.is(Items.PUFFERFISH) || st.is(Items.CHORUS_FRUIT) || st.is(Items.SUSPICIOUS_STEW)) continue;
            float score = f.getNutrition() + f.getSaturationModifier() * f.getNutrition() * 2;
            if (score > bestScore) { bestScore = score; best = i; }
        }
        return best;
    }
}
