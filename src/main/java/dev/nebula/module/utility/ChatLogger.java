package dev.nebula.module.utility;

import dev.nebula.Nebula;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;

/** Saves all chat to .minecraft/nebula/chatlogs/<date>.txt so you can look back later. */
public final class ChatLogger extends Module {
    public ChatLogger() {
        super("Chat Logger", "Saves chat to a text file.", Category.CLIENT);
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent e) {
        if (!isEnabled()) return;
        try {
            Path dir = mc().gameDirectory.toPath().resolve("nebula").resolve("chatlogs");
            Files.createDirectories(dir);
            String line = "[" + LocalTime.now().withNano(0) + "] " + e.getMessage().getString() + System.lineSeparator();
            Files.writeString(dir.resolve(LocalDate.now() + ".txt"), line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) {
            Nebula.LOG.warn("Chat Logger write failed", ex);
        }
    }
}
