package dev.nebula.module.combat;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Eats a golden apple from your hotbar when your health drops. */
public final class AutoGap extends Module {
    public final NumberSetting health = add(new NumberSetting("Below Health", 2, 19, 10, 1, " hp"));
    public final BoolSetting preferEnchanted = add(new BoolSetting("Prefer Enchanted", false));

    private boolean eating;
    private int previous = -1, ticks;

    public AutoGap() {
        super("Auto Gap", "Eats a golden apple when you're low.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || mc().gameMode == null) return;
        if (eating) {
            ticks++;
            if (!p.isUsingItem() && ticks > 4) stop();
            else mc().options.keyUse.setDown(true);
            return;
        }
        if (mc().screen != null || p.getHealth() + p.getAbsorptionAmount() > health.value) return;
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack st = p.getInventory().getItem(i);
            boolean ench = st.is(Items.ENCHANTED_GOLDEN_APPLE), gap = st.is(Items.GOLDEN_APPLE);
            if (!ench && !gap) continue;
            if (slot < 0 || (preferEnchanted.get() && ench) || (!preferEnchanted.get() && gap)) slot = i;
        }
        if (slot < 0) return;
        previous = p.getInventory().selected;
        p.getInventory().selected = slot;
        mc().gameMode.useItem(p, InteractionHand.MAIN_HAND);
        mc().options.keyUse.setDown(true);
        eating = true;
        ticks = 0;
    }

    private void stop() {
        mc().options.keyUse.setDown(false);
        var p = mc().player;
        if (p != null && previous >= 0) p.getInventory().selected = previous;
        previous = -1;
        eating = false;
    }

    @Override
    public void onDisable() {
        if (eating) stop();
    }
}
