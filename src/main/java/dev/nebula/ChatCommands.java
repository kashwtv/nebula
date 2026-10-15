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

    private static void build(String[] a, String msg) {
        var ab = dev.nebula.module.world.AutoBuild.get();
        var mc = Minecraft.getInstance();
        String sub = a.length > 1 ? a[1].toLowerCase() : "help";
        switch (sub) {
            case "list" -> {
                var f = dev.nebula.module.world.AutoBuild.files();
                say("Files in " + dev.nebula.module.world.AutoBuild.folder() + ": " + (f.isEmpty() ? "(none)" : String.join(", ", f)));
                say("Presets: " + String.join(", ", dev.nebula.schem.Schematic.PRESETS));
            }
            case "load" -> {
                if (a.length < 3) { say("Usage: .build load <file|preset>"); return; }
                say(ab.load(msg.substring(msg.toLowerCase().indexOf("load") + 4).trim()));
            }
            case "here" -> {
                if (mc.player == null) return;
                ab.origin = a.length >= 5 ? new net.minecraft.core.BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]))
                        : mc.player.blockPosition().relative(mc.player.getDirection(), 2);
                say("Origin set to " + ab.origin.getX() + " " + ab.origin.getY() + " " + ab.origin.getZ());
            }
            case "rotate" -> {
                ab.rotation = ab.rotation.getRotated(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
                ab.retransform();
                say("Rotation: " + ab.rotation.name().toLowerCase());
            }
            case "mirror" -> {
                var M = net.minecraft.world.level.block.Mirror.values();
                ab.mirror = M[(ab.mirror.ordinal() + 1) % M.length];
                ab.retransform();
                say("Mirror: " + ab.mirror.name().toLowerCase());
            }
            case "start" -> { if (!ab.isEnabled()) ab.toggle(); }
            case "stop" -> { if (ab.isEnabled()) ab.toggle(); }
            case "clear" -> { if (ab.isEnabled()) ab.toggle(); ab.loaded = ab.active = null; say("Cleared."); }
            case "mats", "materials" -> {
                var m = ab.materials();
                if (m.isEmpty()) { say("Nothing left to place."); return; }
                say("Still needed: " + m.entrySet().stream().map(en -> en.getValue() + "x " + en.getKey().getDescription().getString()).collect(Collectors.joining(", ")));
            }
            case "preview" -> { ab.showWhileOff = !ab.showWhileOff; say("Preview while off: " + ab.showWhileOff); }
            default -> say(".build list | load <file|preset> | here [x y z] | rotate | mirror | start | stop | mats | preview | clear");
        }
    }

    @SubscribeEvent
    public static void onChat(ClientChatEvent e) {
        String msg = e.getMessage().trim();
        if (!msg.startsWith(".")) return;
        String[] a = msg.substring(1).split("\\s+");
        String c = a[0].toLowerCase();
        if (!c.equals("seed") && !c.equals("find") && !c.equals("structures") && !c.equals("sf") && !c.equals("build") && !c.equals("portal")) return;
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
                    sf.search(mc.player.level().dimension(), (StructureTypes.Type) null, n == null ? sf.count.i() : n);
                    return;
                }
                if (a[1].contains(",")) { // .find village,stronghold,monument
                    var list = new java.util.ArrayList<StructureTypes.Type>();
                    for (String id : a[1].split(",")) { var tt = StructureTypes.byId(id); if (tt == null) { say("Unknown type " + id); return; } list.add(tt); }
                    var d0 = list.get(0).dim();
                    if (list.stream().anyMatch(tt -> tt.dim() != d0)) { say("Pick types from one dimension at a time."); return; }
                    sf.search(d0, list, n == null ? sf.count.i() : n, true);
                    return;
                }
                var t = StructureTypes.byId(a[1]);
                if (t == null) { say("Unknown type " + a[1] + ". Try .find list"); return; }
                say("Searching " + t.name() + "...");
                sf.search(t.dim(), t, n == null ? sf.count.i() : n);
            }
            case "portal" -> {
                if (mc.player == null) return;
                sf.findPortalRoom();
            }
            case "build" -> build(a, msg);
            default -> mc.tell(() -> mc.setScreen(new StructureFinderScreen()));
        }
    }
}
