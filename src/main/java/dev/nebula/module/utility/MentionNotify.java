package dev.nebula.module.utility;

import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;

/** Pings and highlights chat messages that mention your name. */
public final class MentionNotify extends Module {
    public MentionNotify() {
        super("Mention Notify", "Ping when someone says your name.", Category.CLIENT);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null) return;
        String me = p.getGameProfile().getName().toLowerCase(Locale.ROOT);
        String msg = e.getMessage().getString().toLowerCase(Locale.ROOT);
        if (!msg.contains(me) || msg.startsWith("<" + me + ">")) return;
        p.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 1.6f);
        e.setMessage(Component.literal("» ").withStyle(ChatFormatting.GOLD).append(e.getMessage()));
    }
}
