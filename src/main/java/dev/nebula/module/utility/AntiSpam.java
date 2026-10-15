package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.NumberSetting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/** Hides chat lines that repeat the same text within a short window. */
public final class AntiSpam extends Module {
    public final NumberSetting window = add(new NumberSetting("Window", 1, 60, 10, 1, "s"));

    private final Map<String, Long> seen = new HashMap<>();

    public AntiSpam() {
        super("Anti Spam", "Hides repeated chat messages.", Category.CLIENT);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!isEnabled()) return;
        String s = e.getMessage().getString().trim();
        if (s.isEmpty()) return;
        long now = System.currentTimeMillis();
        seen.values().removeIf(t -> now - t > window.value * 1000);
        if (seen.containsKey(s)) e.setCanceled(true);
        else seen.put(s, now);
    }
}
