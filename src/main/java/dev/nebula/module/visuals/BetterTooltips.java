package dev.nebula.module.visuals;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Adds durability numbers, food values and stack totals to item tooltips. */
public final class BetterTooltips extends Module {
    public final BoolSetting durability = add(new BoolSetting("Durability", true));
    public final BoolSetting food = add(new BoolSetting("Food Values", true));
    public final BoolSetting total = add(new BoolSetting("Inventory Total", true));

    public BetterTooltips() {
        super("Better Tooltips", "More info on item tooltips.", Category.VISUALS);
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null) return;
        var st = e.getItemStack();
        var t = e.getToolTip();
        if (durability.get() && st.isDamageableItem()) {
            int left = st.getMaxDamage() - st.getDamageValue();
            t.add(Component.literal("Durability: " + left + " / " + st.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        }
        var props = st.getItem().getFoodProperties(st, p);
        if (food.get() && props != null)
            t.add(Component.literal(String.format("Food: %d  Saturation: %.1f", props.getNutrition(), props.getNutrition() * props.getSaturationModifier() * 2)).withStyle(ChatFormatting.GOLD));
        if (total.get() && !st.isEmpty()) {
            int n = 0;
            for (var s : p.getInventory().items) if (s.is(st.getItem())) n += s.getCount();
            if (n > st.getCount()) t.add(Component.literal("In inventory: " + n).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
