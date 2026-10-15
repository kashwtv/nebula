package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Adds the time in front of every chat message. */
public final class ChatTimestamps extends Module {
    public final BoolSetting seconds = add(new BoolSetting("Seconds", false));

    public ChatTimestamps() {
        super("Chat Timestamps", "Shows the time on chat messages.", Category.CLIENT);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!isEnabled() || e.isSystem() && e.getMessage().getString().isEmpty()) return;
        String t = LocalTime.now().format(DateTimeFormatter.ofPattern(seconds.get() ? "HH:mm:ss" : "HH:mm"));
        e.setMessage(Component.literal("[" + t + "] ").withStyle(ChatFormatting.GRAY).append(e.getMessage()));
    }
}
