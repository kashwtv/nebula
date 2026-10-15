package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

/** Shows "+N Item" on your action bar whenever items enter your inventory. */
public final class ItemPickupNotify extends Module {
    private Map<Item, Integer> last = new HashMap<>();

    public ItemPickupNotify() {
        super("Pickup Notify", "Shows what you just picked up.", Category.CLIENT);
    }

    private Map<Item, Integer> count() {
        Map<Item, Integer> m = new HashMap<>();
        for (var st : mc().player.getInventory().items) if (!st.isEmpty()) m.merge(st.getItem(), st.getCount(), Integer::sum);
        return m;
    }

    @Override
    public void onEnable() {
        if (mc().player != null) last = count();
    }

    @Override
    public void onTick() {
        if (mc().screen != null) { last = count(); return; } // moving things around in menus isn't a pickup
        Map<Item, Integer> now = count();
        for (var en : now.entrySet()) {
            int diff = en.getValue() - last.getOrDefault(en.getKey(), 0);
            if (diff > 0) {
                mc().player.displayClientMessage(Component.literal("+" + diff + " " + en.getKey().getDescription().getString()).withStyle(ChatFormatting.GREEN), true);
                break;
            }
        }
        last = now;
    }
}
