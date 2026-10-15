package dev.nebula;

import dev.nebula.gui.StructureFinderScreen;
import dev.nebula.module.world.StructureFinder;
import dev.nebula.structure.SeedWorld;
import dev.nebula.structure.StructureTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.stream.Collectors;

/** Local client commands starting with "." (never sent to the server). */
public final class ChatCommands {
    private ChatCommands() {}

    private static void say(String s) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.displayClientMessage(Component.literal("[Nebula] " + s).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    @SubscribeEvent
    public static void onChat(ClientChatEvent e) {
        String msg = e.getMessage().trim();
        if (!msg.startsWith(".")) return;
        String[] a = msg.substring(1).split("\\s+");
        String c = a[0].toLowerCase();
        if (!c.equals("seed") && !c.equals("find") && !c.equals("structures") && !c.equals("sf")) return;
        e.setCanceled(true);
        Minecraft.getInstance().gui.getChat().addRecentChat(msg);
        var sf = StructureFinder.get();
        var mc = Minecraft.getInstance();
        switch (c) {
            case "seed" -> {
                if (a.length < 2) {
                    say(mc.hasSingleplayerServer() ? "Singleplayer: the world's own generator is used, no seed needed."
                            : StructureFinder.seed == null ? "No seed set. Use .seed <seed>" : "Seed: " + StructureFinder.seed);
                    return;
                }
                StructureFinder.seed = SeedWorld.parseSeed(msg.substring(msg.indexOf(' ') + 1));
                sf.results.clear();
                say("Seed set to " + StructureFinder.seed + ". Assumes a vanilla 'Default' world type.");
            }
            case "find" -> {
                if (mc.player == null) return;
                if (a.length < 2 || a[1].equalsIgnoreCase("list")) {
                    say("Types: " + StructureTypes.ALL.stream().map(StructureTypes.Type::id).collect(Collectors.joining(", ")) + ", all");
                    return;
                }
                Integer n = null;
                if (a.length > 2) try { n = Math.max(1, Math.min(20, Integer.parseInt(a[2]))); } catch (NumberFormatException ignored) {}
                if (a[1].equalsIgnoreCase("all")) {
                    sf.search(mc.player.level().dimension(), null, n == null ? sf.count.i() : n);
                    return;
                }
                var t = StructureTypes.byId(a[1]);
                if (t == null) { say("Unknown type " + a[1] + ". Try .find list"); return; }
                say("Searching " + t.name() + "...");
                sf.search(t.dim(), t, n == null ? sf.count.i() : n);
            }
            default -> mc.tell(() -> mc.setScreen(new StructureFinderScreen()));
        }
    }
}
