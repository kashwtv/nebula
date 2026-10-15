package dev.nebula.module.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.schem.Schematic;
import dev.nebula.gui.Notifications;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import dev.nebula.util.Place;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.*;
import java.util.*;

/**
 * Builds a loaded schematic (.litematic, .schem, vanilla .nbt or a built-in preset) block by block from your
 * inventory. Load with ".build load <file|preset>", anchor with ".build here", then turn the module on (or ".build start").
 */
public final class AutoBuild extends Module {
    public final NumberSetting perTick = add(new NumberSetting("Blocks Per Tick", 1, 4, 1, 1));
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 20, 2, 1, "t"));
    public final NumberSetting hotbarSlot = add(new NumberSetting("Swap Slot", 1, 9, 9, 1));
    public final BoolSetting preview = add(new BoolSetting("Ghost Preview", true));
    public final BoolSetting airPlace = add(new BoolSetting("Air Place", false));
    public final BoolSetting pauseInMenus = add(new BoolSetting("Pause In Menus", true));
    public final dev.nebula.module.setting.ActionSetting openUi = add(new dev.nebula.module.setting.ActionSetting("Open UI",
            () -> mc().tell(() -> mc().setScreen(new dev.nebula.gui.AutoBuildScreen()))));
    /** Name of the file/preset currently loaded (for the UI). */
    public String loadedKey;

    public Schematic loaded;   // as loaded
    public Schematic active;   // after rotate/mirror
    public BlockPos origin;
    public Rotation rotation = Rotation.NONE;
    public Mirror mirror = Mirror.NONE;
    private int wait;
    private final Set<Item> missingWarned = new HashSet<>();

    public AutoBuild() {
        super("Auto Build", "Builds a schematic (.litematic/.schem/.nbt) from your inventory.", Category.WORLD);
    }

    public static AutoBuild get() {
        return (AutoBuild) dev.nebula.module.ModuleManager.MODULES.stream().filter(m -> m instanceof AutoBuild).findFirst().orElseThrow();
    }

    public static Path folder() {
        Path p = mc().gameDirectory.toPath().resolve("schematics");
        try { Files.createDirectories(p); } catch (Exception ignored) {}
        return p;
    }

    /** Files in .minecraft/schematics (and Litematica's own folder, same place). */
    public static List<String> files() {
        List<String> out = new ArrayList<>();
        try (var s = Files.walk(folder(), 3)) {
            s.filter(Files::isRegularFile).forEach(f -> {
                String n = f.getFileName().toString().toLowerCase(Locale.ROOT);
                if (n.endsWith(".litematic") || n.endsWith(".schem") || n.endsWith(".schematic") || n.endsWith(".nbt"))
                    out.add(folder().relativize(f).toString().replace('\\', '/'));
            });
        } catch (Exception ignored) {}
        Collections.sort(out);
        return out;
    }

    private static void say(String s) {
        var p = mc().player;
        if (p != null) p.displayClientMessage(Component.literal("[Auto Build] " + s).withStyle(ChatFormatting.AQUA), false);
    }

    public String load(String arg) {
        Schematic s;
        if (arg.startsWith("preset:") || Schematic.PRESETS.contains(arg)) {
            s = Schematic.preset(arg.replace("preset:", ""));
            if (s == null) return "Unknown preset. Presets: " + String.join(", ", Schematic.PRESETS);
        } else {
            Path f = folder().resolve(arg);
            if (!Files.exists(f)) {
                Path alt = Paths.get(arg);
                if (Files.exists(alt)) f = alt;
                else {
                    var match = files().stream().filter(n -> n.toLowerCase(Locale.ROOT).startsWith(arg.toLowerCase(Locale.ROOT))).findFirst();
                    if (match.isEmpty()) return "No file " + arg + " in " + folder();
                    f = folder().resolve(match.get());
                }
            }
            try { s = Schematic.load(f); } catch (Exception e) { return "Couldn't read " + f.getFileName() + ": " + e.getMessage(); }
        }
        loaded = s;
        loadedKey = arg;
        rotation = Rotation.NONE;
        mirror = Mirror.NONE;
        active = s;
        if (origin == null && mc().player != null) origin = mc().player.blockPosition().relative(mc().player.getDirection(), 2);
        return "Loaded " + s.name + " (" + s.sx + "x" + s.sy + "x" + s.sz + ", " + s.blocks.size() + " blocks) at " + fmt(origin);
    }

    public void retransform() {
        if (loaded != null) active = loaded.transform(rotation, mirror);
    }

    static String fmt(BlockPos p) {
        return p == null ? "-" : p.getX() + " " + p.getY() + " " + p.getZ();
    }

    /** {placed, total} for the progress bar. */
    public int[] progress() {
        if (active == null) return new int[]{0, 0};
        int total = active.blocks.size(), done = 0;
        if (origin != null && mc().level != null)
            for (var e : active.blocks.entrySet()) if (mc().level.getBlockState(origin.offset(e.getKey())).is(e.getValue().getBlock())) done++;
        return new int[]{done, total};
    }

    /** Every item the build uses, with the total count. */
    public Map<Item, Integer> totals() {
        Map<Item, Integer> m = new LinkedHashMap<>();
        if (active == null) return m;
        for (var st : active.blocks.values()) { Item it = st.getBlock().asItem(); if (it != Items.AIR) m.merge(it, 1, Integer::sum); }
        return m;
    }

    /** Remaining blocks still to place, with how many of each item you need. */
    public Map<Item, Integer> materials() {
        Map<Item, Integer> m = new LinkedHashMap<>();
        if (active == null || origin == null || mc().level == null) return m;
        for (var e : active.blocks.entrySet()) {
            BlockPos wp = origin.offset(e.getKey());
            if (mc().level.getBlockState(wp).is(e.getValue().getBlock())) continue;
            m.merge(e.getValue().getBlock().asItem(), 1, Integer::sum);
        }
        return m;
    }

    @Override
    public String suffix() {
        return active == null ? "no build" : active.name;
    }

    @Override
    public void onEnable() {
        wait = 0;
        missingWarned.clear();
        if (active == null) say("No build loaded. .build load <file|preset>  (.build list)");
        else if (origin == null && mc().player != null) origin = mc().player.blockPosition();
    }

    @Override
    public void onTick() {
        var p = mc().player;
        var lvl = mc().level;
        if (p == null || lvl == null || active == null || origin == null) return;
        if (pauseInMenus.get() && mc().screen != null && !(mc().screen instanceof dev.nebula.gui.AutoBuildScreen)) return;
        if (wait-- > 0) return;
        wait = delay.i();
        Vec3 eye = p.getEyePosition();
        double reach = Math.min(4.5, p.getBlockReach());
        // bottom-up, nearest first so supports exist before what sits on them
        List<Map.Entry<BlockPos, BlockState>> todo = new ArrayList<>();
        boolean anyLeft = false;
        for (var e : active.blocks.entrySet()) {
            BlockPos wp = origin.offset(e.getKey());
            var cur = lvl.getBlockState(wp);
            if (cur.is(e.getValue().getBlock())) continue;
            anyLeft = true;
            if (!cur.canBeReplaced()) continue; // something else is there; leave it
            if (Vec3.atCenterOf(wp).distanceTo(eye) > reach) continue;
            todo.add(Map.entry(wp, e.getValue()));
        }
        if (!anyLeft) {
            say("Done: " + active.name + " is built.");
            setEnabled(false);
            Notifications.push(this);
            return;
        }
        todo.sort(Comparator.<Map.Entry<BlockPos, BlockState>>comparingInt(e -> e.getKey().getY())
                .thenComparingDouble(e -> Vec3.atCenterOf(e.getKey()).distanceToSqr(eye)));
        int placed = 0;
        for (var e : todo) {
            if (placed >= perTick.i()) break;
            Item item = e.getValue().getBlock().asItem();
            if (item == Items.AIR) continue;
            int slot = ensureHotbar(item);
            if (slot < 0) {
                if (missingWarned.add(item)) say("Out of " + item.getDescription().getString() + ", skipping those blocks.");
                continue;
            }
            if (Place.place(e.getKey(), slot, airPlace.get())) placed++;
        }
    }

    /** Hotbar slot holding the item, moving it from the main inventory into the Swap Slot if needed. */
    private int ensureHotbar(Item item) {
        var p = mc().player;
        int hb = Place.findItem(item);
        if (hb >= 0) return hb;
        var inv = p.getInventory();
        for (int i = 9; i < 36; i++) {
            if (inv.getItem(i).is(item)) {
                if (mc().screen != null || mc().gameMode == null) return -1;
                int target = hotbarSlot.i() - 1;
                // inventory menu slot index == inventory index for 9..35
                mc().gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, i, target, ClickType.SWAP, p);
                return inv.getItem(target).is(item) ? target : -1;
            }
        }
        return -1;
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        if (!preview.get() || active == null || origin == null || mc().level == null || mc().player == null) return;
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!isEnabled() && !showWhileOff) return;
        Vec3 cam = e.getCamera().getPosition();
        var m = e.getPoseStack().last().pose();
        RenderSystem.lineWidth(1.5f);
        var b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, true);
        int n = 0;
        for (var en : active.blocks.entrySet()) {
            BlockPos wp = origin.offset(en.getKey());
            if (wp.distSqr(mc().player.blockPosition()) > 48 * 48) continue;
            var cur = mc().level.getBlockState(wp);
            if (cur.is(en.getValue().getBlock())) continue;
            int c = cur.canBeReplaced() ? 0xFF55FFFF : 0xFFFF5555; // cyan = to place, red = blocked
            World3D.boxEdges(b, m, new AABB(wp).deflate(0.05).move(-cam.x, -cam.y, -cam.z), c, 0.8f);
            if (++n > 4000) break;
        }
        // outline of the whole build
        World3D.boxEdges(b, m, new AABB(origin, origin.offset(active.sx, active.sy, active.sz)).move(-cam.x, -cam.y, -cam.z), 0xFFFFFFFF, 0.5f);
        World3D.end(b);
    }

    /** Ghost preview stays visible after ".build load" even before you start building. */
    public boolean showWhileOff = true;
}
