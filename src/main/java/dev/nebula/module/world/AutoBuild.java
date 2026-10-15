package dev.nebula.module.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.gui.Notifications;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import dev.nebula.schem.Schematic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.*;
import java.util.*;

/**
 * Survival-legit schematic builder. Places from your inventory (swapping into the hotbar), within your real reach,
 * looking at the face it clicks, and picks the look direction / click spot that gives exactly the block state the
 * schematic asks for (stair facing + half, slab half, log axis, door hinge, trapdoor half...). Bottom-up, never
 * places a block without something to click on, waits for supports, pauses when out of materials and resumes when
 * you pick more up. No anticheat bypass: it only does what a player clicking could do.
 */
public final class AutoBuild extends Module {
    public enum Status { IDLE, BUILDING, PAUSED, MOVE_CLOSER, MISSING, WAITING, BREAKING, DONE }

    public final NumberSetting perTick = add(new NumberSetting("Blocks Per Tick", 1, 4, 1, 1));
    public final NumberSetting delay = add(new NumberSetting("Delay", 0, 20, 2, 1, "t"));
    public final NumberSetting hotbarSlot = add(new NumberSetting("Swap Slot", 1, 9, 9, 1));
    public final BoolSetting clearObstructions = add(new BoolSetting("Clear Obstructions", false));
    public final BoolSetting fixStates = add(new BoolSetting("Fix States", true));
    public final BoolSetting preview = add(new BoolSetting("Ghost Preview", true));
    public final BoolSetting hud = add(new BoolSetting("Build HUD", true));
    public final BoolSetting pauseInMenus = add(new BoolSetting("Pause In Menus", true));
    public final dev.nebula.module.setting.ActionSetting openUi = add(new dev.nebula.module.setting.ActionSetting("Open UI",
            () -> mc().tell(() -> mc().setScreen(new dev.nebula.gui.AutoBuildScreen()))));

    public String loadedKey;
    public Schematic loaded, active;
    public BlockPos origin;
    public Rotation rotation = Rotation.NONE;
    public Mirror mirror = Mirror.NONE;
    /** Ghost preview stays visible after ".build load" even before you start building. */
    public boolean showWhileOff = true;

    public boolean paused;
    public Status status = Status.IDLE;
    public String statusText = "";
    /** Relative positions the user skipped. */
    public final Set<BlockPos> skipped = new HashSet<>();
    /** Relative position the builder is currently working on (Skip button skips this). */
    public BlockPos focus;
    public final Set<Item> missingNow = new LinkedHashSet<>();
    /** Blocks the builder gave up on: nothing to click that gives the right state (floating, or orientation impossible). */
    public final Set<BlockPos> unbuildable = new LinkedHashSet<>();
    /** How many ticks a still-failing block has been tried while in reach. */
    private final Map<BlockPos, Integer> failTicks = new HashMap<>();
    public int giveUpTicks = 400;
    private int lastProgress;
    /** Temporary blocks put down only to have something to click on; removed again afterwards. */
    public final Set<BlockPos> helpers = new LinkedHashSet<>();
    public final BoolSetting helperBlocks = add(new BoolSetting("Helper Blocks", true));
    public int placedTotal;
    private final ArrayDeque<Long> recent = new ArrayDeque<>();
    private List<BlockPos> order = List.of();
    private final Map<BlockPos, Integer> retryAt = new HashMap<>();
    private int wait, tickNo, msgCooldown;
    private BlockPos breaking;
    private Direction breakFace;
    private final Set<Item> missingWarned = new HashSet<>();

    public AutoBuild() {
        super("Auto Build", "Builds a schematic (.litematic/.schem/.nbt) from your inventory, survival-legit.", Category.WORLD);
    }

    public static AutoBuild get() {
        return (AutoBuild) dev.nebula.module.ModuleManager.MODULES.stream().filter(m -> m instanceof AutoBuild).findFirst().orElseThrow();
    }

    public static Path folder() {
        Path p = mc().gameDirectory.toPath().resolve("schematics");
        try { Files.createDirectories(p); } catch (Exception ignored) {}
        return p;
    }

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

    /** What the Add schematic picker does after a file is chosen: copy into the schematics folder and load it. */
    public String importFile(Path src) {
        try {
            Path dst = folder().resolve(src.getFileName().toString());
            if (!src.toAbsolutePath().normalize().equals(dst.toAbsolutePath().normalize())) Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            return load(dst.getFileName().toString());
        } catch (Exception ex) {
            return "Copy failed: " + ex.getMessage();
        }
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
        if (isEnabled()) setEnabled(false);
        loaded = s;
        loadedKey = arg;
        rotation = Rotation.NONE;
        mirror = Mirror.NONE;
        retransform();
        status = Status.IDLE;
        statusText = "";
        if (origin == null && mc().player != null) origin = mc().player.blockPosition().relative(mc().player.getDirection(), 2);
        String unk = s.unknown.isEmpty() ? "" : "  Unknown blocks left out: " + s.unknown.keySet();
        return "Loaded " + s.name + " (" + s.sx + "x" + s.sy + "x" + s.sz + ", " + s.blocks.size() + " blocks) at " + fmt(origin) + unk;
    }

    public void retransform() {
        if (loaded == null) return;
        active = loaded.transform(rotation, mirror);
        skipped.clear();
        unbuildable.clear();
        failTicks.clear();
        helpers.clear();
        retryAt.clear();
        // bottom-up; solid blocks before things that hang on them; then a stable sweep
        order = new ArrayList<>(active.blocks.keySet());
        order.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingInt(p -> attaches(active.blocks.get(p)) ? 1 : 0)
                .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
    }

    static String fmt(BlockPos p) { return p == null ? "-" : p.getX() + " " + p.getY() + " " + p.getZ(); }

    // ------------------------------------------------------------------ block rules

    private static final Set<Block> TECH = Set.of(Blocks.BARRIER, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.STRUCTURE_BLOCK, Blocks.STRUCTURE_VOID, Blocks.JIGSAW, Blocks.LIGHT, Blocks.MOVING_PISTON, Blocks.PISTON_HEAD,
            Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_GATEWAY, Blocks.BUBBLE_COLUMN);

    /** Properties that follow from neighbours / world state, not from how you click. */
    private static final Set<Property<?>> IGNORE = Set.of(BlockStateProperties.STAIRS_SHAPE, BlockStateProperties.NORTH, BlockStateProperties.EAST,
            BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.UP, BlockStateProperties.DOWN,
            BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL, BlockStateProperties.SOUTH_WALL, BlockStateProperties.WEST_WALL,
            BlockStateProperties.NORTH_REDSTONE, BlockStateProperties.EAST_REDSTONE, BlockStateProperties.SOUTH_REDSTONE, BlockStateProperties.WEST_REDSTONE,
            BlockStateProperties.WATERLOGGED, BlockStateProperties.POWERED, BlockStateProperties.PERSISTENT, BlockStateProperties.DISTANCE,
            BlockStateProperties.SNOWY, BlockStateProperties.NOTE, BlockStateProperties.NOTEBLOCK_INSTRUMENT, BlockStateProperties.IN_WALL,
            BlockStateProperties.TRIGGERED, BlockStateProperties.SIGNAL_FIRE, BlockStateProperties.LEVEL_COMPOSTER, BlockStateProperties.HAS_BOOK,
            BlockStateProperties.ATTACHED, BlockStateProperties.OCCUPIED, BlockStateProperties.POWER, BlockStateProperties.AGE_25, BlockStateProperties.AGE_7,
            BlockStateProperties.STAGE, BlockStateProperties.BELL_ATTACHMENT, BlockStateProperties.HAS_BOTTLE_0, BlockStateProperties.HAS_BOTTLE_1, BlockStateProperties.HAS_BOTTLE_2);
    /** Fixed by right-clicking after placing. */
    private static final Set<Property<?>> INTERACT = Set.of(BlockStateProperties.OPEN, BlockStateProperties.LIT, BlockStateProperties.DELAY, BlockStateProperties.MODE_COMPARATOR);

    /** Placed automatically together with another block (door/plant top, bed head). */
    public static boolean derived(BlockState s) {
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) return true;
        return s.hasProperty(BlockStateProperties.BED_PART) && s.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
    }

    /** Can't be placed by a survival player (fluids, technical blocks, no item). */
    public static boolean unplaceable(BlockState s) {
        Block b = s.getBlock();
        if (b instanceof LiquidBlock || TECH.contains(b)) return true;
        return !(b.asItem() instanceof BlockItem);
    }

    static boolean attaches(BlockState s) {
        return !s.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    static int count(BlockState s) {
        return s.hasProperty(BlockStateProperties.SLAB_TYPE) && s.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? 2 : 1;
    }

    private static boolean same(BlockState a, BlockState t, Set<Property<?>> extra, boolean ignoreChest) {
        if (a == null || a.getBlock() != t.getBlock()) return false;
        for (Property<?> p : t.getProperties()) {
            if (IGNORE.contains(p) || (extra != null && extra.contains(p)) || (ignoreChest && p == BlockStateProperties.CHEST_TYPE)) continue;
            if (!a.hasProperty(p) || !a.getValue(p).equals(t.getValue(p))) return false;
        }
        return true;
    }

    /** World block counts as built: same block and every click-determined property matches. */
    public static boolean built(BlockState world, BlockState target) { return same(world, target, null, false); }

    /** Strict: the full state is identical. */
    public static boolean exact(BlockState world, BlockState target) { return world == target; }

    // ------------------------------------------------------------------ progress / materials

    /** {built, total} over the blocks you can place (derived halves, skipped and unplaceable left out). */
    public int[] progress() {
        if (active == null) return new int[]{0, 0};
        int total = 0, done = 0;
        var lvl = mc().level;
        for (var e : active.blocks.entrySet()) {
            BlockState t = e.getValue();
            if (derived(t) || unplaceable(t) || skipped.contains(e.getKey())) continue;
            total++;
            if (origin != null && lvl != null && built(lvl.getBlockState(origin.offset(e.getKey())), t)) done++;
        }
        return new int[]{done, total};
    }

    /** Every item the build uses, with the total count (double slabs count 2). */
    public Map<Item, Integer> totals() {
        Map<Item, Integer> m = new LinkedHashMap<>();
        if (active == null) return m;
        for (var st : active.blocks.values()) if (!derived(st) && !unplaceable(st)) m.merge(st.getBlock().asItem(), count(st), Integer::sum);
        return m;
    }

    /** Blocks in the schematic that can't be placed (by name, with count). */
    public Map<String, Integer> unplaceableCounts() {
        Map<String, Integer> m = new TreeMap<>();
        if (active == null) return m;
        for (var st : active.blocks.values()) if (!derived(st) && unplaceable(st)) m.merge(st.getBlock().getName().getString(), 1, Integer::sum);
        return m;
    }

    /** Items still needed for the blocks that aren't built yet. */
    public Map<Item, Integer> materials() {
        Map<Item, Integer> m = new LinkedHashMap<>();
        if (active == null) return m;
        if (origin == null || mc().level == null) return totals();
        for (var e : active.blocks.entrySet()) {
            BlockState t = e.getValue();
            if (derived(t) || unplaceable(t) || skipped.contains(e.getKey())) continue;
            BlockState cur = mc().level.getBlockState(origin.offset(e.getKey()));
            if (built(cur, t)) continue;
            int n = count(t);
            if (n == 2 && cur.is(t.getBlock())) n = 1; // half a double slab is already down
            m.merge(t.getBlock().asItem(), n, Integer::sum);
        }
        return m;
    }

    public static int have(Item it) {
        var p = mc().player;
        return p == null ? 0 : p.getInventory().countItem(it);
    }

    /** One row per item: item, total, still needed, have, missing. Sorted by missing desc, then needed, then name. */
    public record Mat(Item item, int total, int need, int have, int missing) {}

    public List<Mat> materialRows() {
        var tot = totals();
        var left = materials();
        List<Mat> out = new ArrayList<>();
        for (var e : tot.entrySet()) {
            int need = left.getOrDefault(e.getKey(), 0), h = have(e.getKey());
            out.add(new Mat(e.getKey(), e.getValue(), need, h, Math.max(0, need - h)));
        }
        out.sort(Comparator.comparingInt((Mat r) -> -r.missing).thenComparingInt(r -> -r.need).thenComparing(r -> r.item.getDescription().getString()));
        return out;
    }

    public String materialsText() {
        StringBuilder b = new StringBuilder();
        b.append(active == null ? "Nothing loaded" : "Materials for " + active.name + " (" + totals().values().stream().mapToInt(i -> i).sum() + " blocks)").append('\n');
        for (Mat r : materialRows())
            b.append(String.format("%-28s need %4d  have %4d  missing %4d  (total %d)%n", r.item.getDescription().getString(), r.need, r.have, r.missing, r.total));
        for (var e : unplaceableCounts().entrySet()) b.append(String.format("%-28s %d (can't be placed, skipped)%n", e.getKey(), e.getValue()));
        return b.toString();
    }

    /** Seconds left at the recent placing rate, or -1 if unknown. */
    public int eta() {
        int[] pr = progress();
        if (recent.size() < 3) return -1;
        double secs = (recent.peekLast() - recent.peekFirst()) / 1000.0;
        if (secs <= 0) return -1;
        double rate = (recent.size() - 1) / secs;
        return (int) Math.ceil((pr[1] - pr[0]) / rate);
    }

    @Override
    public String suffix() { return active == null ? "no build" : active.name; }

    // ------------------------------------------------------------------ controls

    public void start() {
        if (active == null) { say("No build loaded. .build load <file|preset>  (.build list)"); return; }
        paused = false;
        if (!isEnabled()) setEnabled(true);
    }

    public void pause(boolean p) { paused = p; if (p) { status = Status.PAUSED; statusText = "Paused"; stopBreaking(); } }

    public void stop() { paused = false; if (isEnabled()) setEnabled(false); status = Status.IDLE; statusText = "Stopped"; }

    /** Skips the block the builder is stuck on / working on. */
    public String skipFocus() {
        if (focus == null || active == null || !active.blocks.containsKey(focus)) return "Nothing to skip";
        skipped.add(focus);
        String s = "Skipped " + active.blocks.get(focus).getBlock().getName().getString() + " at " + fmt(origin.offset(focus));
        focus = null;
        return s;
    }

    @Override
    public void onEnable() {
        wait = 0;
        missingWarned.clear();
        retryAt.clear();
        recent.clear();
        breaking = null;
        if (active == null) { say("No build loaded. .build load <file|preset>  (.build list)"); setEnabled(false); return; }
        if (origin == null && mc().player != null) origin = mc().player.blockPosition();
        status = paused ? Status.PAUSED : Status.BUILDING;
        statusText = "";
    }

    @Override
    public void onDisable() { stopBreaking(); }

    private void stopBreaking() {
        if (breaking != null && mc().gameMode != null) mc().gameMode.stopDestroyBlock();
        breaking = null;
    }

    private void set(Status s, String text) {
        status = s;
        statusText = text;
        var p = mc().player;
        if (p != null && (s == Status.MOVE_CLOSER || s == Status.MISSING) && msgCooldown <= 0) {
            p.displayClientMessage(Component.literal("[Auto Build] " + text).withStyle(s == Status.MISSING ? ChatFormatting.RED : ChatFormatting.YELLOW), true);
            msgCooldown = 20;
        }
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void onTick() {
        var p = mc().player;
        var lvl = mc().level;
        tickNo++;
        msgCooldown--;
        if (p == null || lvl == null || active == null || origin == null) return;
        if (paused) { set(Status.PAUSED, "Paused"); return; }
        if (pauseInMenus.get() && mc().screen != null && !(mc().screen instanceof dev.nebula.gui.AutoBuildScreen)) return;
        if (breaking != null) { tickBreak(); return; }
        if (wait-- > 0) return;
        wait = delay.i();

        Vec3 eye = p.getEyePosition();
        double reach = p.getBlockReach();
        int remaining = 0;
        double nearest = Double.MAX_VALUE;
        BlockPos nearestRel = null;
        List<BlockPos> place = new ArrayList<>(), obstruct = new ArrayList<>(), fix = new ArrayList<>();
        for (BlockPos rel : order) {
            BlockState t = active.blocks.get(rel);
            if (derived(t) || unplaceable(t) || skipped.contains(rel)) continue;
            BlockPos wp = origin.offset(rel);
            BlockState cur = lvl.getBlockState(wp);
            if (built(cur, t)) continue;
            remaining++;
            double d = Vec3.atCenterOf(wp).distanceTo(eye);
            if (d < nearest) { nearest = d; nearestRel = rel; }
            if (cur.getBlock() == t.getBlock() && same(cur, t, INTERACT, false)) fix.add(rel);
            else if (cur.canBeReplaced() || (cur.getBlock() == t.getBlock() && count(t) == 2 && cur.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE)) place.add(rel);
            else obstruct.add(rel);
        }
        if (remaining == 0 && !helpers.isEmpty()) {
            for (BlockPos h : new ArrayList<>(helpers)) {
                if (lvl.getBlockState(h).canBeReplaced()) { helpers.remove(h); continue; }
                if (Vec3.atCenterOf(h).distanceTo(eye) <= reach && startBreak(h)) { set(Status.BREAKING, "Removing helper block"); return; }
            }
            if (!helpers.isEmpty()) { set(Status.MOVE_CLOSER, "Move closer to remove " + helpers.size() + " helper block(s)"); return; }
        }
        if (remaining == 0) {
            int sk = skipped.size();
            var un = unplaceableCounts();
            say("Done: " + active.name + " is built." + (sk > 0 ? " " + sk + " skipped" + (unbuildable.isEmpty() ? "." : " (" + unbuildable.size() + " couldn't be placed legitimately).") : "") + (un.isEmpty() ? "" : " Not placeable: " + un));
            set(Status.DONE, "Done");
            focus = null;
            setEnabled(false);
            Notifications.push(this);
            return;
        }

        // helper blocks we put down earlier: take them away once nothing next to them still needs them
        for (Iterator<BlockPos> it = helpers.iterator(); it.hasNext(); ) {
            BlockPos h = it.next();
            if (lvl.getBlockState(h).canBeReplaced()) { it.remove(); continue; }
        }
        int budget = perTick.i(), did = 0;
        missingNow.clear();
        boolean anyInReach = false, waiting = false;
        // 1) right-click fixes (trapdoor/door open, campfire lit)
        if (fixStates.get()) for (BlockPos rel : fix) {
            if (did >= budget) break;
            BlockPos wp = origin.offset(rel);
            if (Vec3.atCenterOf(wp).distanceTo(eye) > reach) continue;
            anyInReach = true;
            if (interact(wp, active.blocks.get(rel))) { did++; focus = rel; lastProgress = tickNo; }
        }
        // 2) placing
        int planned = 0;
        for (BlockPos rel : place) {
            if (did >= budget || planned >= 24) break;
            BlockPos wp = origin.offset(rel);
            if (Vec3.atCenterOf(wp).distanceTo(eye) > reach + 0.9) continue;
            anyInReach = true;
            BlockState t = active.blocks.get(rel);
            Item item = t.getBlock().asItem();
            if (have(item) == 0) { missingNow.add(item); if (focus == null) focus = rel; continue; }
            Integer ra = retryAt.get(rel);
            if (ra != null && ra > tickNo) { waiting = true; continue; }
            planned++;
            int slot = ensureHotbar(item);
            if (slot < 0) continue;
            ItemStack stack = p.getInventory().getItem(slot);
            Plan plan = plan(wp, t, stack, reach, null, false);
            if (plan == null) {
                retryAt.put(rel, tickNo + 20);
                waiting = true;
                focus = rel;
                int ft = failTicks.merge(rel, 20, Integer::sum);
                if (helperBlocks.get() && ft >= 60 && did == 0 && placeHelper(wp, t, stack, reach)) { did++; retryAt.remove(rel); lastProgress = tickNo; continue; }
                if (ft >= giveUpTicks && did == 0 && tickNo - lastProgress > 200) {
                    unbuildable.add(rel);
                    skipped.add(rel);
                    say("Can't place " + t.getBlock().getName().getString() + " at " + fmt(wp) + ": nothing to click that gives that state (floating, or needs a temporary block). Skipped.");
                }
                continue;
            }
            failTicks.remove(rel);
            if (doPlace(slot, plan)) {
                did++;
                lastProgress = tickNo;
                focus = rel;
                placedTotal++;
                recent.addLast(System.currentTimeMillis());
                while (recent.size() > 40) recent.removeFirst();
            }
        }
        // 3) helper blocks that are no longer needed, then obstructions
        if (did == 0) for (BlockPos h : helpers) {
            if (Vec3.atCenterOf(h).distanceTo(eye) > reach) continue;
            boolean needed = false;
            for (Direction d : Direction.values()) {
                BlockPos r = h.relative(d).subtract(origin);
                BlockState t = active.blocks.get(r);
                if (t != null && !skipped.contains(r) && !derived(t) && !unplaceable(t) && !built(lvl.getBlockState(h.relative(d)), t)) { needed = true; break; }
            }
            if (!needed && startBreak(h)) { lastProgress = tickNo; set(Status.BREAKING, "Removing helper block"); return; }
        }
        for (BlockPos rel : obstruct) if (Vec3.atCenterOf(origin.offset(rel)).distanceTo(eye) <= reach) { anyInReach = true; focus = focus == null ? rel : focus; }
        if (did == 0 && !obstruct.isEmpty()) {
            if (clearObstructions.get()) {
                obstruct.sort(Comparator.comparingInt(r -> -r.getY()));
                for (BlockPos rel : obstruct) {
                    BlockPos wp = origin.offset(rel);
                    if (Vec3.atCenterOf(wp).distanceTo(eye) > reach) continue;
                    anyInReach = true;
                    if (startBreak(wp)) { focus = rel; lastProgress = tickNo; set(Status.BREAKING, "Clearing " + lvl.getBlockState(wp).getBlock().getName().getString()); return; }
                }
            } else waiting = true;
        }
        if (did > 0) { set(Status.BUILDING, "Building"); return; }
        if (!missingNow.isEmpty()) {
            Item first = missingNow.iterator().next();
            if (missingWarned.add(first)) say("Out of " + first.getDescription().getString() + ". Waiting on those blocks; it carries on when you pick more up.");
            set(Status.MISSING, "Missing " + first.getDescription().getString() + (missingNow.size() > 1 ? " +" + (missingNow.size() - 1) : ""));
        } else if (!anyInReach) {
            focus = nearestRel;
            set(Status.MOVE_CLOSER, String.format("Move closer: nearest block %.1fm away (reach %.1f)", nearest, reach));
        } else if (!obstruct.isEmpty() && !clearObstructions.get() && !waitingPlace(place, eye, reach)) {
            set(Status.WAITING, obstruct.size() + " blocked spots (red). Turn on Clear Obstructions or break them.");
        } else if (waiting) {
            set(Status.WAITING, "Waiting for support / can't orient from here");
        }
    }

    // ------------------------------------------------------------------ placing

    public record Plan(BlockHitResult hit, float yaw, float pitch, boolean sneak) {}

    private static float[] lookAt(Vec3 eye, Vec3 at) {
        double dx = at.x - eye.x, dy = at.y - eye.y, dz = at.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        return new float[]{Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90, 90)};
    }

    /** 0 = can't click, 1 = click, 2 = must sneak-click (block reacts to right-click). */
    private static int clickable(BlockState s) {
        if (s.canBeReplaced() || s.isAir()) return 0;
        Block b = s.getBlock();
        // these do something on right-click; clicking them without sneaking would open/toggle instead of placing
        return (b instanceof BaseEntityBlock || b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock
                || b instanceof ButtonBlock || b instanceof LeverBlock || b instanceof CraftingTableBlock || b instanceof DiodeBlock
                || b instanceof NoteBlock || b instanceof AnvilBlock || b instanceof CartographyTableBlock || b instanceof GrindstoneBlock
                || b instanceof LoomBlock || b instanceof StonecutterBlock || b instanceof ComposterBlock || b instanceof CakeBlock
                || b instanceof BedBlock || b instanceof RespawnAnchorBlock || b instanceof FlowerPotBlock || b instanceof RedStoneWireBlock
                || b instanceof CandleBlock || b instanceof SmithingTableBlock || b instanceof EnchantmentTableBlock) ? 2 : 1;
    }

    private static void sneak(boolean on) {
        var p = mc().player;
        if (p.input != null) p.input.shiftKeyDown = on;
        p.setShiftKeyDown(on);
    }

    /** Finds a face, click spot and look direction that places exactly the target state. */
    public Plan plan(BlockPos wp, BlockState target, ItemStack stack, double reach) { return plan(wp, target, stack, reach, null, false); }

    private boolean waitingPlace(List<BlockPos> place, Vec3 eye, double reach) {
        for (BlockPos r : place) if (Vec3.atCenterOf(origin.offset(r)).distanceTo(eye) <= reach + 0.9) return true;
        return false;
    }

    /** Blocks we may use as throwaway helpers, cheapest first. */
    private static final List<Item> HELPER_ITEMS = List.of(Items.COBBLESTONE, Items.DIRT, Items.NETHERRACK, Items.COBBLED_DEEPSLATE, Items.STONE, Items.SAND);

    /**
     * When a block can't get its state from any real neighbour (a sideways log on the floor, a top trapdoor with
     * nothing beside it) put a throwaway block where clicking it would give the right state. Removed afterwards.
     */
    private boolean placeHelper(BlockPos wp, BlockState target, ItemStack stack, double reach) {
        var p = mc().player;
        var lvl = mc().level;
        for (Direction d : Direction.values()) {
            BlockPos n = wp.relative(d);
            if (!lvl.getBlockState(n).canBeReplaced() || active.blocks.containsKey(n.subtract(origin))) continue;
            if (new AABB(n).intersects(p.getBoundingBox())) continue;
            if (plan(wp, target, stack, reach, n, false) == null) continue;
            // a helper item: something cheap, or a full block we have spare
            Item helper = null;
            var need = materials();
            for (Item it : HELPER_ITEMS) if (have(it) > need.getOrDefault(it, 0)) { helper = it; break; }
            if (helper == null) for (int i = 0; i < 36; i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (st.getItem() instanceof BlockItem bi && bi.getBlock().defaultBlockState().isCollisionShapeFullBlock(lvl, n) && !totals().containsKey(st.getItem())
                        && !(bi.getBlock() instanceof FallingBlock) && !(bi.getBlock() instanceof BaseEntityBlock) && have(st.getItem()) > need.getOrDefault(st.getItem(), 0)) { helper = st.getItem(); break; }
            }
            if (helper == null) return false;
            int slot = ensureHotbar(helper);
            if (slot < 0) return false;
            Plan hp = plan(n, ((BlockItem) helper).getBlock().defaultBlockState(), p.getInventory().getItem(slot), reach, null, true);
            if (hp == null) continue;
            if (doPlace(slot, hp)) {
                helpers.add(n.immutable());
                dev.nebula.Nebula.LOG.info("[Auto Build] helper {} at {} for {} at {}", helper, fmt(n), target, fmt(wp));
                return true;
            }
        }
        return false;
    }

    private Plan plan(BlockPos wp, BlockState target, ItemStack stack, double reach, BlockPos virtualNeighbour, boolean anyState) {
        var p = mc().player;
        var lvl = mc().level;
        if (!(stack.getItem() instanceof BlockItem bi)) return null;
        if (target.getBlock() instanceof FallingBlock && FallingBlock.isFree(lvl.getBlockState(wp.below()))) return null;
        Vec3 eye = p.getEyePosition();
        float oy = p.getYRot(), ox = p.getXRot();
        BlockState cur = lvl.getBlockState(wp);
        boolean selfClick = cur.getBlock() == target.getBlock(); // finishing a double slab
        boolean wasSneak = p.input != null && p.input.shiftKeyDown;
        // a double slab starts as either half
        boolean halfOk = !selfClick && target.hasProperty(BlockStateProperties.SLAB_TYPE) && target.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE;
        try {
            for (int pass = 0; pass < 2; pass++) {
                if (pass == 1 && !target.hasProperty(BlockStateProperties.CHEST_TYPE)) break;
                for (Direction d : Direction.values()) {
                    BlockPos n = selfClick ? wp : wp.relative(d);
                    Direction face = selfClick ? d : d.getOpposite();
                    if (virtualNeighbour != null && !n.equals(virtualNeighbour)) continue;
                    int ck = selfClick ? 1 : n.equals(virtualNeighbour) ? 1 : clickable(lvl.getBlockState(n));
                    if (ck == 0) continue;
                    boolean sn = ck == 2;
                    sneak(sn);
                    Vec3 c = Vec3.atCenterOf(n).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
                    if (selfClick) c = Vec3.atCenterOf(n).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
                    Direction.Axis ax = face.getAxis();
                    Vec3 u = ax == Direction.Axis.X ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
                    Vec3 v = ax == Direction.Axis.Z ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1);
                    double[] offs = {0, -0.3, 0.3};
                    for (double a : offs) for (double b : offs) {
                        Vec3 hv = c.add(u.scale(a)).add(v.scale(b));
                        if (selfClick) {
                            // a slab's real surface: bottom slab top face is at y+0.5
                            var shape = cur.getShape(lvl, wp);
                            if (shape.isEmpty()) continue;
                            var bb = shape.bounds();
                            if (face == Direction.UP) hv = new Vec3(hv.x, wp.getY() + bb.maxY, hv.z);
                            else if (face == Direction.DOWN) hv = new Vec3(hv.x, wp.getY() + bb.minY, hv.z);
                            else continue;
                        }
                        if (hv.distanceTo(eye) > reach) continue;
                        float[] look = lookAt(eye, hv);
                        float[] yaws = {look[0], 0, 90, 180, -90};
                        float[] pitches = {look[1], 0, 60, -60, 89, -89};
                        for (float pi : pitches) for (float yw : yaws) {
                            p.setYRot(yw);
                            p.setXRot(pi);
                            // virtual neighbour isn't in the world yet: simulate the same face/spot/look as a click "into" the target cell
                            var hit = new BlockHitResult(hv, face, n.equals(virtualNeighbour) ? wp : n, false);
                            var ctx = new BlockPlaceContext(p, InteractionHand.MAIN_HAND, stack, hit);
                            if (!ctx.getClickedPos().equals(wp) || !ctx.canPlace()) continue;
                            BlockState s = predict(bi, ctx, target);
                            if (s == null || (virtualNeighbour == null && !s.canSurvive(lvl, wp)) || (virtualNeighbour != null && attaches(target) && !(target.getBlock() instanceof SlabBlock) && !(target.getBlock() instanceof StairBlock) && !(target.getBlock() instanceof TrapDoorBlock)) || !lvl.isUnobstructed(s, wp, CollisionContext.of(p))) continue;
                            if (pass == 1 && s.hasProperty(BlockStateProperties.CHEST_TYPE) && s.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) continue; // never join the wrong chest
                            if (same(s, target, INTERACT, pass == 1) || ((halfOk || anyState) && s.getBlock() == target.getBlock())) return new Plan(hit, yw, pi, sn);
                        }
                    }
                }
            }
            return null;
        } finally {
            p.setYRot(oy);
            p.setXRot(ox);
            sneak(wasSneak);
        }
    }

    /** What the item would place for this click (mirrors BlockItem / StandingAndWallBlockItem). */
    static BlockState predict(BlockItem bi, BlockPlaceContext ctx, BlockState target) {
        if (bi instanceof StandingAndWallBlockItem) {
            Block standing = bi.getBlock();
            Direction attach = bi instanceof HangingSignItem ? Direction.UP : Direction.DOWN;
            BlockState wall = target.getBlock() != standing ? target.getBlock().getStateForPlacement(ctx) : null;
            for (Direction dir : ctx.getNearestLookingDirections()) {
                if (dir == attach.getOpposite()) continue;
                BlockState s = dir == attach ? standing.getStateForPlacement(ctx) : wall;
                if (s != null && s.canSurvive(ctx.getLevel(), ctx.getClickedPos())) return s;
            }
            return null;
        }
        return bi.getBlock().getStateForPlacement(ctx);
    }

    private boolean doPlace(int slot, Plan plan) {
        var p = mc().player;
        var gm = mc().gameMode;
        if (gm == null) return false;
        look(plan.yaw, plan.pitch);
        p.getInventory().selected = slot;
        boolean was = p.input != null && p.input.shiftKeyDown;
        if (plan.sneak && !was) { sneak(true); p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY)); }
        var r = gm.useItemOn(p, InteractionHand.MAIN_HAND, plan.hit);
        if (plan.sneak && !was) { sneak(false); p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY)); }
        if (r.consumesAction()) p.swing(InteractionHand.MAIN_HAND);
        return r.consumesAction();
    }

    private void look(float yaw, float pitch) {
        var p = mc().player;
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.setYHeadRot(yaw);
        p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, p.onGround()));
    }

    /** Right-click to fix open / lit. */
    private boolean interact(BlockPos wp, BlockState target) {
        var p = mc().player;
        var gm = mc().gameMode;
        BlockState cur = mc().level.getBlockState(wp);
        Block b = cur.getBlock();
        if (b == Blocks.IRON_DOOR || b == Blocks.IRON_TRAPDOOR) return false;
        if (b instanceof CampfireBlock) {
            if (!cur.getValue(BlockStateProperties.LIT) || target.getValue(BlockStateProperties.LIT)) return false; // lighting needs flint & steel
            int slot = ensureHotbarMatching(s -> s.getItem() instanceof ShovelItem);
            if (slot < 0) { missingNow.add(Items.WOODEN_SHOVEL); return false; }
            p.getInventory().selected = slot;
        } else if (cur.hasProperty(BlockStateProperties.LIT)) return false;
        Vec3 eye = p.getEyePosition();
        Vec3 c = Vec3.atCenterOf(wp);
        Direction face = Direction.getNearest(eye.x - c.x, eye.y - c.y, eye.z - c.z);
        Vec3 hv = c.add(face.getStepX() * 0.45, face.getStepY() * 0.45, face.getStepZ() * 0.45);
        float[] l = lookAt(eye, hv);
        look(l[0], l[1]);
        var r = gm.useItemOn(p, InteractionHand.MAIN_HAND, new BlockHitResult(hv, face, wp, false));
        if (r.consumesAction()) p.swing(InteractionHand.MAIN_HAND);
        return r.consumesAction();
    }

    // ------------------------------------------------------------------ clearing

    private boolean startBreak(BlockPos wp) {
        var p = mc().player;
        BlockState s = mc().level.getBlockState(wp);
        if (s.getDestroySpeed(mc().level, wp) < 0) return false; // bedrock etc.
        var inv = p.getInventory();
        int best = -1; float bs = 1f;
        for (int i = 0; i < 36; i++) { float sp = inv.getItem(i).getDestroySpeed(s); if (sp > bs) { bs = sp; best = i; } }
        if (best >= 9) best = swapIn(best);
        if (best >= 0) inv.selected = best;
        Vec3 eye = p.getEyePosition(), c = Vec3.atCenterOf(wp);
        breakFace = Direction.getNearest(eye.x - c.x, eye.y - c.y, eye.z - c.z);
        float[] l = lookAt(eye, c.add(breakFace.getStepX() * 0.45, breakFace.getStepY() * 0.45, breakFace.getStepZ() * 0.45));
        look(l[0], l[1]);
        breaking = wp;
        mc().gameMode.startDestroyBlock(wp, breakFace);
        p.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    private void tickBreak() {
        var lvl = mc().level;
        if (lvl.getBlockState(breaking).canBeReplaced() || mc().player.getEyePosition().distanceTo(Vec3.atCenterOf(breaking)) > mc().player.getBlockReach()) { stopBreaking(); return; }
        mc().gameMode.continueDestroyBlock(breaking, breakFace);
        mc().player.swing(InteractionHand.MAIN_HAND);
    }

    // ------------------------------------------------------------------ inventory

    private boolean canClickInv() {
        return mc().gameMode != null && (mc().screen == null || mc().screen instanceof dev.nebula.gui.AutoBuildScreen);
    }

    private int swapIn(int invSlot) {
        var p = mc().player;
        if (!canClickInv()) return -1;
        int target = hotbarSlot.i() - 1;
        mc().gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, invSlot, target, ClickType.SWAP, p);
        return target;
    }

    private int ensureHotbarMatching(java.util.function.Predicate<ItemStack> f) {
        var inv = mc().player.getInventory();
        if (f.test(inv.getItem(inv.selected))) return inv.selected;
        for (int i = 0; i < 9; i++) if (f.test(inv.getItem(i))) return i;
        for (int i = 9; i < 36; i++) if (f.test(inv.getItem(i))) { int t = swapIn(i); return t >= 0 && f.test(inv.getItem(t)) ? t : -1; }
        return -1;
    }

    private int ensureHotbar(Item item) { return ensureHotbarMatching(s -> s.is(item)); }

    // ------------------------------------------------------------------ rendering

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
            BlockState t = en.getValue();
            if (derived(t) || skipped.contains(en.getKey())) continue;
            var cur = mc().level.getBlockState(wp);
            if (built(cur, t)) continue;
            // cyan = to place, yellow = right block wrong state, red = something else in the way, grey = can't be placed
            int c = unplaceable(t) ? 0xFF888888 : cur.canBeReplaced() ? 0xFF55FFFF : cur.getBlock() == t.getBlock() ? 0xFFFFCC33 : 0xFFFF5555;
            World3D.boxEdges(b, m, new AABB(wp).deflate(0.05).move(-cam.x, -cam.y, -cam.z), c, 0.8f);
            if (++n > 4000) break;
        }
        World3D.boxEdges(b, m, new AABB(origin, origin.offset(active.sx, active.sy, active.sz)).move(-cam.x, -cam.y, -cam.z), 0xFFFFFFFF, 0.5f);
        World3D.end(b);
    }

    /** Compact in-game HUD while building: progress, ETA, status, top materials still needed. */
    @SubscribeEvent
    public void onHud(RenderGuiEvent.Post e) {
        if (!isEnabled() || !hud.get() || active == null || mc().options.hideGui || mc().screen instanceof dev.nebula.gui.AutoBuildScreen) return;
        var g = e.getGuiGraphics();
        var f = mc().font;
        int[] pr = progress();
        int pct = pr[0] * 100 / Math.max(1, pr[1]);
        int eta = eta();
        String l1 = "Auto Build  " + pct + "%  " + pr[0] + "/" + pr[1] + (eta >= 0 ? "  ETA " + eta / 60 + ":" + String.format("%02d", eta % 60) : "");
        String l2 = paused ? "Paused" : statusText.isEmpty() ? status.name() : statusText;
        List<String> lines = new ArrayList<>(List.of(l1, l2));
        int shown = 0;
        for (Mat r : materialRows()) {
            if (r.need == 0 || shown >= 5) continue;
            lines.add((r.missing > 0 ? "\u00a7c" : "\u00a7a") + r.need + "x \u00a77" + r.item.getDescription().getString() + (r.missing > 0 ? " \u00a7c(missing " + r.missing + ")" : ""));
            shown++;
        }
        int w = 0;
        for (String s : lines) w = Math.max(w, f.width(s));
        int x = 4, y = e.getWindow().getGuiScaledHeight() / 2 - lines.size() * 5;
        g.fill(x - 2, y - 2, x + w + 4, y + lines.size() * 10 + 4, 0x90000000);
        g.fill(x - 2, y + 9, x - 2 + (w + 6) * pct / 100, y + 10, 0xFF55FFFF);
        for (int i = 0; i < lines.size(); i++) g.drawString(f, lines.get(i), x, y + 1 + i * 10 + (i > 0 ? 2 : 0), i == 1 ? 0xFFAAAAAA : 0xFFFFFFFF);
    }
}
