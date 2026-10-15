package dev.nebula.schem;

import dev.nebula.Nebula;
import dev.nebula.module.world.AutoBuild;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Dev only: run/nebula_abtest -> survival world, then builds presets, a state-heavy litematic (plain and
 * rotated+mirrored), Kash's own files, an obstruction case and an out-of-materials case with AutoBuild, and checks
 * the world block-for-block. The only "cheat" the harness itself does is teleporting the player when AutoBuild
 * says "move closer" (stands in for the user walking). Results in nebula_abtest_results.txt.
 */
public final class AbHarness {
    private AbHarness() {}
    private static final List<String> OUT = new ArrayList<>();
    private static final ArrayDeque<BooleanSupplier> STEPS = new ArrayDeque<>();
    private static boolean started;
    private static int t, idle;
    private static final BlockPos O = new BlockPos(0, 201, 0);
    private static final Map<Item, Integer> STASH = new LinkedHashMap<>();
    private static int refills;

    private static Minecraft mc() { return Minecraft.getInstance(); }
    private static Path dir() { return mc().gameDirectory.toPath(); }
    static void log(String s) { OUT.add(s); Nebula.LOG.info("ABTEST " + s); try { Files.write(dir().resolve("nebula_abtest_results.txt"), OUT); } catch (Exception ignored) {} }

    private static void step(BooleanSupplier s) { STEPS.add(s); }
    private static void waitTicks(int n) { int[] c = {0}; step(() -> ++c[0] >= n); }
    private static void run(Runnable r) { step(() -> { r.run(); return true; }); }
    private static void shot(String name) {
        waitTicks(4);
        run(() -> Screenshot.grab(mc().gameDirectory, name + ".png", mc().getMainRenderTarget(), c -> {
            try { Files.copy(dir().resolve("screenshots").resolve(name + ".png"), Path.of("/workspace/nebula-ui-shots/" + name + ".png"), StandardCopyOption.REPLACE_EXISTING); } catch (Exception e) { Nebula.LOG.warn("copy shot", e); }
        }));
        waitTicks(3);
    }

    private static <T> T server(java.util.function.Function<net.minecraft.server.level.ServerPlayer, T> f) {
        var srv = mc().getSingleplayerServer();
        try { return srv.submit(() -> f.apply(srv.getPlayerList().getPlayers().get(0))).get(); } catch (Exception e) { throw new RuntimeException(e); }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !Files.exists(dir().resolve("nebula_abtest"))) return;
        try {
            if (!started) {
                if ((mc().level == null && mc().screen != null || mc().player != null) && ++t > 60) { started = true; plan(); }
                return;
            }
            while (!STEPS.isEmpty()) {
                if (!STEPS.peek().getAsBoolean()) return;
                STEPS.poll();
            }
            log("ALL DONE");
            Files.deleteIfExists(dir().resolve("nebula_abtest"));
            mc().stop();
        } catch (Throwable ex) {
            Nebula.LOG.error("abtest", ex);
            log("ERROR " + ex);
            try { Files.deleteIfExists(dir().resolve("nebula_abtest")); } catch (Exception ignored) {}
            mc().stop();
        }
    }

    // --------------------------------------------------------------------------- plan

    private static void plan() throws Exception {
        if (mc().level == null) {
        var settings = new net.minecraft.world.level.LevelSettings("abtest_" + System.currentTimeMillis(), GameType.SURVIVAL, false,
                net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
        mc().createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new net.minecraft.world.level.levelgen.WorldOptions(12345L, false, false),
                ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
        }
        step(() -> mc().player != null);
        run(() -> { mc().options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE; mc().getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE); mc().getToasts().clear(); });
        waitTicks(60);
        run(() -> server(sp -> { sp.server.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, sp.server); sp.serverLevel().setDayTime(6000); sp.server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true); sp.setGameMode(GameType.SURVIVAL); return 0; }));
        // test schematics
        run(() -> {
            try {
                BuildHarness.writeLitematic(statesSchematic(), AutoBuild.folder().resolve("t_states.litematic"));
                log("survival: " + (mc().gameMode.getPlayerMode() == GameType.SURVIVAL) + ", reach " + mc().player.getBlockReach());
            } catch (Exception ex) { throw new RuntimeException(ex); }
        });
        var ab = AutoBuild.get();
        run(() -> { ab.perTick.value = 2; ab.delay.value = 1; ab.clearObstructions.value = false; });

        if (filter().isEmpty()) guiTest();

        buildCase("nether_portal", () -> ab.load("preset:nether_portal"), null, Map.of(), null);
        buildCase("cobble_hut (runs out of cobble)", () -> ab.load("preset:cobble_hut"), null, Map.of(Items.COBBLESTONE, 30), "starve");
        buildCase("t_states.litematic", () -> ab.load("t_states.litematic"), null, Map.of(), null);
        buildCase("t_states.litematic rotated 90 + mirrored", () -> { String m = ab.load("t_states.litematic"); ab.rotation = Rotation.CLOCKWISE_90; ab.mirror = Mirror.FRONT_BACK; ab.retransform(); return m + " rot90+mirror"; }, null, Map.of(), null);
        buildCase("t_states.litematic rotated 180", () -> { String m = ab.load("t_states.litematic"); ab.rotation = Rotation.CLOCKWISE_180; ab.retransform(); return m + " rot180"; }, null, Map.of(), null);
        buildCase("kash1.litematic", () -> ab.importFile(Path.of("/workspace/nebula-test-schems/kash1.litematic")), "kash1", Map.of(), null);
        buildCase("kash2.schem", () -> ab.importFile(Path.of("/workspace/nebula-test-schems/kash2.schem")), "kash2", Map.of(), null);
        buildCase("kash2.schem + 8 spare cobblestone (helper blocks)", () -> ab.importFile(Path.of("/workspace/nebula-test-schems/kash2.schem")), null, Map.of(Items.COBBLESTONE, 8), null);
        buildCase("bridge with obstructions (Clear Obstructions on)", () -> ab.load("preset:bridge"), null, Map.of(), "obstruct");
    }

    // --------------------------------------------------------------------------- GUI

    private static void guiTest() {
        var ab = AutoBuild.get();
        run(() -> mc().setScreen(new dev.nebula.gui.AutoBuildScreen()));
        waitTicks(10);
        shot("ab_gui_empty");
        clickCheck("row:preset:cobble_hut", () -> ab.active != null && ab.active.name.contains("cobble_hut"));
        shot("ab_gui_loaded");
        clickCheck("Place Here", () -> ab.origin != null);
        for (int i = 1; i <= 4; i++) { int k = i; clickCheck("Rotate", () -> ab.rotation.ordinal() == k % 4); }
        clickCheck("Mirror", () -> ab.mirror == Mirror.LEFT_RIGHT);
        shot("ab_gui_mirrored");
        clickCheck("Mirror", () -> ab.mirror == Mirror.FRONT_BACK);
        clickCheck("Mirror", () -> ab.mirror == Mirror.NONE);
        boolean[] pv = new boolean[1];
        run(() -> pv[0] = ab.showWhileOff && ab.preview.get());
        clickCheck("Preview", () -> (ab.showWhileOff && ab.preview.get()) != pv[0]);
        clickCheck("Preview", () -> (ab.showWhileOff && ab.preview.get()) == pv[0]);
        clickCheck("Copy", () -> mc().keyboardHandler.getClipboard().contains("Cobblestone"));
        clickCheck("Refresh", () -> true);
        run(() -> ab.origin = mc().player.blockPosition().offset(30, 0, 30));
        clickCheck("Start", () -> ab.isEnabled() && !ab.paused);
        waitTicks(5);
        shot("ab_gui_building");
        clickCheck("Pause", () -> ab.isEnabled() && ab.paused);
        shot("ab_gui_paused");
        clickCheck("Resume", () -> ab.isEnabled() && !ab.paused);
        clickCheck("Skip block", () -> true);
        clickCheck("Stop", () -> !ab.isEnabled());
        // state survives closing the GUI
        run(() -> { ab.start(); ab.pause(true); mc().setScreen(null); });
        waitTicks(5);
        run(() -> mc().setScreen(new dev.nebula.gui.AutoBuildScreen()));
        waitTicks(5);
        run(() -> log("gui: state survives close/reopen (paused build still loaded+paused): " + (ab.isEnabled() && ab.paused && ab.active != null ? "PASS" : "FAIL")));
        run(() -> ab.stop());
        // Add schematic: the native picker can't run under xvfb; test what it does with the picked file
        run(() -> {
            String m = ab.importFile(Path.of("/workspace/nebula-test-schems/kash2.schem"));
            boolean ok = Files.exists(AutoBuild.folder().resolve("kash2.schem")) && ab.active != null && ab.active.name.equals("kash2");
            log("gui: Add schematic copy+load path (importFile): " + m + (ok ? " PASS" : " FAIL"));
            m = ab.importFile(Path.of("/nonexistent/x.litematic"));
            log("gui: Add schematic with missing file: " + m + (m.startsWith("Copy failed") ? " PASS" : " FAIL"));
        });
        clickCheck("Refresh", () -> true);
        run(() -> log("gui: labels seen: " + ((dev.nebula.gui.AutoBuildScreen) mc().screen).devLabels()));
        run(() -> { log("gui: Open folder button present (not clicked under xvfb: it launches the OS file manager); Add schematic present (native dialog not run)"); mc().setScreen(null); ab.loaded = ab.active = null; ab.origin = null; });
    }

    private static void clickCheck(String label, BooleanSupplier check) {
        run(() -> {
            if (!(mc().screen instanceof dev.nebula.gui.AutoBuildScreen s)) { log("gui: " + label + " FAIL (screen not open)"); return; }
            boolean clicked = s.devClick(label);
            if (!clicked) log("gui: " + label + " FAIL (button not found)");
        });
        waitTicks(3);
        run(() -> {
            String toast = mc().screen instanceof dev.nebula.gui.AutoBuildScreen s ? s.devToast() : null;
            log("gui: click " + label + (toast != null ? " [" + toast + "]" : "") + (check.getAsBoolean() ? " PASS" : " FAIL"));
        });
    }

    // --------------------------------------------------------------------------- build cases

    private static List<String> filter() {
        try { return Files.readAllLines(dir().resolve("nebula_abtest")).stream().map(String::trim).filter(l -> !l.isEmpty()).toList(); } catch (Exception e) { return List.of(); }
    }

    private static void buildCase(String name, java.util.function.Supplier<String> loader, String shotPrefix, Map<Item, Integer> limit, String mode) {
        var f = filter();
        if (!f.isEmpty() && f.stream().noneMatch(name::contains)) return;
        var ab = AutoBuild.get();
        int[] lastDone = {0}; int[] sinceProgress = {0}; int[] moves = {0}; int[] ticks = {0}; boolean[] starved = {false}; boolean[] refilled = {false};
        long[] t0 = {0};
        run(() -> {
            log("");
            log("=== " + name);
            ab.origin = O;
            log(loader.get());
            var s = ab.active;
            log("size " + s.sx + "x" + s.sy + "x" + s.sz + ", " + s.blocks.size() + " non-air blocks" + (ab.loaded.remapped.isEmpty() ? "" : ", remapped " + ab.loaded.remapped) + (ab.loaded.unknown.isEmpty() ? "" : ", UNKNOWN " + ab.loaded.unknown.keySet()));
            log("materials (total): " + fmtItems(ab.totals()));
            if (!ab.unplaceableCounts().isEmpty()) log("not placeable: " + ab.unplaceableCounts());
            // arena + inventory
            Map<Item, Integer> give = new LinkedHashMap<>(ab.totals());
            for (var e : limit.entrySet()) give.put(e.getKey(), e.getValue());
            int sx = s.sx, sy = s.sy, sz = s.sz;
            server(sp -> {
                var lvl = sp.serverLevel();
                for (int x = -10; x < sx + 10; x++) for (int z = -10; z < sz + 10; z++) {
                    lvl.setBlock(O.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                    for (int y = 0; y < sy + 6; y++) lvl.setBlock(O.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                if ("obstruct".equals(mode)) { lvl.setBlock(O.offset(1, 0, 3), Blocks.DIRT.defaultBlockState(), 3); lvl.setBlock(O.offset(0, 0, 9), Blocks.OAK_PLANKS.defaultBlockState(), 3); lvl.setBlock(O.offset(2, 0, 12), Blocks.STONE.defaultBlockState(), 3); }
                sp.setGameMode(GameType.SURVIVAL);
                var inv = sp.getInventory();
                inv.clearContent();
                inv.setItem(0, new ItemStack(Items.IRON_SHOVEL));
                STASH.clear();
                int slot = 9;
                for (var e : give.entrySet()) {
                    int n = e.getValue(), max = e.getKey().getMaxStackSize();
                    while (n > 0 && slot < 36) { int k = Math.min(n, max); inv.setItem(slot++, new ItemStack(e.getKey(), k)); n -= k; }
                    if (n > 0 && !limit.containsKey(e.getKey())) STASH.put(e.getKey(), n);
                }
                if (!STASH.isEmpty()) log("inventory full: " + STASH.values().stream().mapToInt(i -> i).sum() + " more items wait in a 'stash' and get handed over when AutoBuild says it's missing them (like fetching from a chest)");
                if ("obstruct".equals(mode)) { inv.setItem(1, new ItemStack(Items.IRON_PICKAXE)); inv.setItem(2, new ItemStack(Items.IRON_AXE)); }
                sp.teleportTo(lvl, O.getX() + sx / 2.0 + 0.5, O.getY(), O.getZ() - 2.5, 0, 20);
                return 0;
            });
            if ("obstruct".equals(mode)) ab.clearObstructions.value = true;
        });
        waitTicks(20);
        run(() -> log("materials screen check (need/have/missing): " + ab.materialRows().stream().filter(r -> r.missing() > 0).count() + " types missing"));
        if (shotPrefix != null) {
            run(() -> { mc().getToasts().clear(); mc().setScreen(new dev.nebula.gui.AutoBuildScreen()); });
            waitTicks(10);
            shot(shotPrefix + "_screen_before");
            run(() -> mc().setScreen(null));
        }
        run(() -> { t0[0] = System.currentTimeMillis(); ab.start(); });
        step(() -> {
            ticks[0]++;
            int[] pr = ab.progress();
            if (pr[0] > lastDone[0]) { lastDone[0] = pr[0]; sinceProgress[0] = 0; } else sinceProgress[0]++;
            if (shotPrefix != null && ticks[0] == 160) Screenshot.grab(mc().gameDirectory, shotPrefix + "_hud_building.png", mc().getMainRenderTarget(), c -> copyShot(shotPrefix + "_hud_building"));
            if ("starve".equals(mode) && !refilled[0] && ab.status == AutoBuild.Status.MISSING) {
                if (!starved[0]) { starved[0] = true; sinceProgress[0] = 0; log("ran out: status=" + ab.statusText + " progress " + pr[0] + "/" + pr[1] + ", module still on=" + ab.isEnabled()); Screenshot.grab(mc().gameDirectory, "ab_missing_hud.png", mc().getMainRenderTarget(), c -> copyShot("ab_missing_hud")); }
                if (sinceProgress[0] > 60) {
                    int need = ab.materials().getOrDefault(Items.COBBLESTONE, 0);
                    log("waited 60 ticks with no placing (" + pr[0] + "/" + pr[1] + "), now picking up " + need + " cobblestone");
                    server(sp -> { sp.getInventory().add(new ItemStack(Items.COBBLESTONE, need)); return 0; });
                    refilled[0] = true;
                }
                return false;
            }
            if (!ab.isEnabled()) return true;
            if (ab.status == AutoBuild.Status.MISSING && !STASH.isEmpty() && !"starve".equals(mode)) {
                List<Item> want = new ArrayList<>(ab.missingNow);
                server(sp -> {
                    var inv = sp.getInventory();
                    for (Item it : want) {
                        Integer n = STASH.get(it);
                        if (n == null) continue;
                        // make room: drop nothing, only fill free slots / existing stacks
                        int before = n;
                        while (n > 0) { ItemStack st = new ItemStack(it, Math.min(n, it.getMaxStackSize())); int c = st.getCount(); if (!inv.add(st)) { n -= c - st.getCount(); break; } n -= c; }
                        if (n <= 0) STASH.remove(it); else STASH.put(it, n);
                        if (before != n) refills++;
                    }
                    return 0;
                });
                sinceProgress[0] = 0;
            }
            if ((ab.status == AutoBuild.Status.MOVE_CLOSER && sinceProgress[0] > 5) || sinceProgress[0] > 40 && sinceProgress[0] % 40 == 1) {
                if (moves[0] > 400 || sinceProgress[0] > 900) { log("STUCK: " + ab.statusText); return true; }
                if (walk()) moves[0]++;
            }
            return ticks[0] > 24000;
        });
        run(() -> {
            if (ab.isEnabled()) { log("timed out/stuck, status " + ab.status + " " + ab.statusText); ab.stop(); }
            double secs = (System.currentTimeMillis() - t0[0]) / 1000.0;
            log(String.format("build time %.1fs, %d ticks, %d walk moves (teleports standing in for walking), %d stash refills", secs, ticks[0], moves[0], refills));
            refills = 0;
            if (!ab.unbuildable.isEmpty()) log("AutoBuild gave up on (no legit click): " + ab.unbuildable.stream().map(p -> p.toShortString() + " " + ab.active.blocks.get(p)).toList());
            verify();
            if ("obstruct".equals(mode)) ab.clearObstructions.value = false;
        });
        if (shotPrefix != null) {
            run(() -> server(sp -> { sp.teleportTo(sp.serverLevel(), O.getX() - 9.5, O.getY() + 5, O.getZ() - 9.5, -45, 18); return 0; }));
            waitTicks(30);
            shot(shotPrefix + "_world_built");
            run(() -> mc().setScreen(new dev.nebula.gui.AutoBuildScreen()));
            waitTicks(10);
            shot(shotPrefix + "_screen_after");
            run(() -> mc().setScreen(null));
        } else if (name.startsWith("t_states.litematic rotated 90")) {
            run(() -> server(sp -> { sp.teleportTo(sp.serverLevel(), O.getX() + 5.5, O.getY() + 3, O.getZ() - 5.5, 30, 35); return 0; }));
            waitTicks(30);
            shot("tstates_rot90_world");
        } else if (name.equals("t_states.litematic")) {
            run(() -> server(sp -> { sp.teleportTo(sp.serverLevel(), O.getX() + 6.5, O.getY() + 2, O.getZ() + 9.5, 180, 25); return 0; }));
            waitTicks(30);
            shot("tstates_world");
        }
    }

    private static void copyShot(String n) {
        try { Files.copy(dir().resolve("screenshots").resolve(n + ".png"), Path.of("/workspace/nebula-ui-shots/" + n + ".png"), StandardCopyOption.REPLACE_EXISTING); } catch (Exception ignored) {}
    }

    private static String fmtItems(Map<Item, Integer> m) {
        StringBuilder b = new StringBuilder();
        m.entrySet().stream().sorted((a, c) -> c.getValue() - a.getValue()).forEach(e -> b.append(e.getValue()).append(' ').append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getKey()).getPath()).append(", "));
        return b.toString();
    }

    /** Stands in for the user walking: teleports to the free standing spot that reaches the most unbuilt blocks. */
    private static boolean walk() {
        var ab = AutoBuild.get();
        var lvl = mc().level;
        var s = ab.active;
        List<BlockPos> pending = new ArrayList<>();
        Set<BlockPos> targets = new HashSet<>();
        for (var e : s.blocks.entrySet()) {
            BlockPos wp = ab.origin.offset(e.getKey());
            BlockState t = e.getValue();
            if (!t.getCollisionShape(lvl, wp).isEmpty()) targets.add(wp);
            if (AutoBuild.derived(t) || AutoBuild.unplaceable(t) || ab.skipped.contains(e.getKey())) continue;
            if (!AutoBuild.built(lvl.getBlockState(wp), t)) pending.add(wp);
        }
        if (pending.isEmpty()) return false;
        int lowY = pending.stream().mapToInt(BlockPos::getY).min().getAsInt();
        BlockPos best = null; double bestScore = -1;
        BlockPos cur = mc().player.blockPosition();
        for (int x = -4; x < s.sx + 4; x++) for (int z = -4; z < s.sz + 4; z++) for (int y = 0; y <= s.sy; y++) {
            BlockPos f = ab.origin.offset(x, y, z);
            if (f.equals(cur)) continue;
            if (!lvl.getBlockState(f).getCollisionShape(lvl, f).isEmpty() || !lvl.getBlockState(f.above()).getCollisionShape(lvl, f.above()).isEmpty()) continue;
            var below = lvl.getBlockState(f.below()).getCollisionShape(lvl, f.below());
            if (below.isEmpty() || below.max(net.minecraft.core.Direction.Axis.Y) < 1.0) continue;
            if (targets.contains(f) && !AutoBuild.built(lvl.getBlockState(f), s.blocks.get(f.subtract(ab.origin))) || targets.contains(f.above()) && !AutoBuild.built(lvl.getBlockState(f.above()), s.blocks.get(f.above().subtract(ab.origin)))) continue;
            Vec3 eye = new Vec3(f.getX() + 0.5, f.getY() + 1.62, f.getZ() + 0.5);
            double score = 0;
            for (BlockPos p : pending) {
                if (p.equals(f) || p.equals(f.above())) continue;
                if (Vec3.atCenterOf(p).distanceTo(eye) <= 4.2) score += p.getY() == lowY ? 3 : 1;
            }
            if (score > bestScore) { bestScore = score; best = f; }
        }
        if (best == null || bestScore <= 0) return false;
        BlockPos b = best;
        server(sp -> { sp.teleportTo(sp.serverLevel(), b.getX() + 0.5, b.getY(), b.getZ() + 0.5, sp.getYRot(), sp.getXRot()); return 0; });
        return true;
    }

    private static void verify() {
        var ab = AutoBuild.get();
        var lvl = mc().getSingleplayerServer().overworld();
        int total = 0, exact = 0, relaxed = 0, unpl = 0, skipped = 0;
        Map<String, Integer> diff = new TreeMap<>();
        List<String> ex = new ArrayList<>();
        for (var e : ab.active.blocks.entrySet()) {
            BlockState t = e.getValue();
            BlockPos wp = ab.origin.offset(e.getKey());
            BlockState w = lvl.getBlockState(wp);
            if (AutoBuild.unplaceable(t)) { unpl++; continue; }
            if (ab.skipped.contains(e.getKey())) { skipped++; }
            total++;
            if (w == t) { exact++; relaxed++; continue; }
            if (AutoBuild.built(w, t)) relaxed++;
            String k = describeDiff(w, t);
            diff.merge(k, 1, Integer::sum);
            if (ex.size() < 12) ex.add(e.getKey().toShortString() + " want " + t + " got " + w);
        }
        // anything left in the arena that isn't part of the schematic (helper blocks not cleaned up, misplaced blocks)
        List<String> extra = new ArrayList<>();
        var a = ab.active;
        for (int x = -3; x < a.sx + 3; x++) for (int y = 0; y < a.sy + 3; y++) for (int z = -3; z < a.sz + 3; z++) {
            BlockPos r = new BlockPos(x, y, z);
            if (a.blocks.containsKey(r)) continue;
            BlockState w = lvl.getBlockState(ab.origin.offset(r));
            if (!w.isAir() && !(w.getBlock() instanceof LiquidBlock)) extra.add(r.toShortString() + " " + w);
        }
        log("extra blocks outside the schematic: " + extra.size() + (extra.isEmpty() ? "" : " " + extra.subList(0, Math.min(8, extra.size()))));
        log(String.format("MATCH exact (full state) %d/%d = %.1f%%, placement-relevant %d/%d = %.1f%%; not placeable %d; skipped %d",
                exact, total, 100.0 * exact / Math.max(1, total), relaxed, total, 100.0 * relaxed / Math.max(1, total), unpl, skipped));
        if (!diff.isEmpty()) log("differences: " + diff);
        for (String s : ex) log("  e.g. " + s);
    }

    private static String describeDiff(BlockState w, BlockState t) {
        if (w.getBlock() != t.getBlock()) return t.getBlock().getName().getString() + " missing (got " + w.getBlock().getName().getString() + ")";
        List<String> props = new ArrayList<>();
        for (Property<?> p : t.getProperties()) if (!w.getValue(p).equals(t.getValue(p))) props.add(p.getName());
        return t.getBlock().getName().getString() + " " + props;
    }

    // --------------------------------------------------------------------------- state test schematic

    static Schematic statesSchematic() {
        Schematic s = new Schematic("t_states");
        var cob = Blocks.COBBLESTONE.defaultBlockState();
        for (int x = 0; x < 12; x++) for (int y = 0; y < 3; y++) s.put(x, y, 0, cob);
        Direction[] H = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        var st = Blocks.OAK_STAIRS.defaultBlockState();
        for (int i = 0; i < 4; i++) s.put(i * 2, 0, 1, st.setValue(StairBlock.FACING, H[i]).setValue(StairBlock.HALF, Half.BOTTOM));
        var slab = Blocks.OAK_SLAB.defaultBlockState();
        s.put(1, 0, 1, slab.setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        s.put(3, 0, 1, slab.setValue(SlabBlock.TYPE, SlabType.TOP));
        s.put(5, 0, 1, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        s.put(7, 0, 1, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        s.put(8, 0, 1, st.setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.TOP));
        s.put(9, 0, 1, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        s.put(10, 0, 1, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP));
        s.put(11, 0, 1, slab.setValue(SlabBlock.TYPE, SlabType.DOUBLE));
        s.put(1, 0, 2, cob); // neighbour for the top trapdoor below
        s.put(0, 1, 1, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
        s.put(3, 1, 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        s.put(9, 1, 1, Blocks.WHITE_CARPET.defaultBlockState());
        s.put(6, 2, 1, cob);
        s.put(6, 1, 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        s.put(2, 1, 1, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.TOP));
        var td = Blocks.OAK_TRAPDOOR.defaultBlockState();
        s.put(0, 0, 3, td.setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.HALF, Half.BOTTOM));
        s.put(1, 0, 3, td.setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.HALF, Half.TOP));
        s.put(2, 0, 3, td.setValue(TrapDoorBlock.FACING, Direction.EAST).setValue(TrapDoorBlock.HALF, Half.BOTTOM).setValue(TrapDoorBlock.OPEN, true));
        var door = Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST).setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT);
        s.put(4, 0, 3, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        s.put(4, 1, 3, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        s.put(6, 0, 3, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.EAST));
        s.put(7, 0, 3, Blocks.SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        s.put(8, 0, 3, Blocks.TORCH.defaultBlockState());
        s.put(10, 0, 3, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
        s.put(11, 0, 3, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
        s.put(9, 0, 3, Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.UP));
        s.bounds();
        return s;
    }
}
