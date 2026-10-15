package dev.nebula.module.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.nebula.Nebula;
import dev.nebula.gui.Notifications;
import dev.nebula.module.Category;
import dev.nebula.module.Module;
import dev.nebula.module.setting.BoolSetting;
import dev.nebula.module.setting.NumberSetting;
import dev.nebula.render.World3D;
import dev.nebula.structure.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Finds the nearest vanilla structures of every type in the Overworld, Nether and End.
 * Singleplayer: asks the integrated server's own generator (same data /locate uses).
 * Multiplayer: rebuilds the vanilla default world from a seed you enter with ".seed <seed>".
 */
public final class StructureFinder extends Module {
    public final NumberSetting count = add(new NumberSetting("Per Type", 1, 10, 3, 1));
    public final NumberSetting radius = add(new NumberSetting("Search Rings", 8, 100, 40, 1));
    public final BoolSetting beams = add(new BoolSetting("Beams", true));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    public final BoolSetting onlyNearest = add(new BoolSetting("Render Nearest Only", true));
    public final BoolSetting autoRefresh = add(new BoolSetting("Auto Refresh", true));
    public final BoolSetting portalRoom = add(new BoolSetting("Find Portal Room", true));

    /** Last found end portal room (frame centre), separate from the stronghold's own result. */
    public volatile net.minecraft.core.BlockPos portalPos;
    public volatile String portalInfo;

    public static volatile Long seed;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Nebula-StructureFinder");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private static final AtomicInteger GEN = new AtomicInteger();

    /** Results per dimension: type id -> hits. */
    public final Map<ResourceKey<Level>, Map<String, List<StructureSearch.Hit>>> results = new ConcurrentHashMap<>();
    public final Map<ResourceKey<Level>, Vec3> origins = new ConcurrentHashMap<>();
    public volatile String status = "Idle";
    public volatile boolean busy;
    /** Pinned hit (x,z,dim) rendered even when "nearest only" is on. */
    public final Set<String> pinned = ConcurrentHashMap.newKeySet();

    public StructureFinder() {
        super("Structure Finder", "Nearest structures of every type, all dimensions.", Category.WORLD);
    }

    public static StructureFinder get() {
        return (StructureFinder) dev.nebula.module.ModuleManager.MODULES.stream().filter(m -> m instanceof StructureFinder).findFirst().orElseThrow();
    }

    @Override
    public String suffix() {
        return busy ? "..." : mc().hasSingleplayerServer() ? "SP" : seed == null ? "no seed" : "seed";
    }

    @Override
    public void onEnable() {
        if (mc().player != null) search(mc().player.level().dimension(), (StructureTypes.Type) null, null);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || busy || !autoRefresh.get()) return;
        var dim = p.level().dimension();
        Vec3 o = origins.get(dim);
        if (o != null && o.distanceTo(p.position()) > 512) search(dim, (StructureTypes.Type) null, null);
    }

    private static void chat(Component c) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        mc.execute(() -> { if (mc.player != null) mc.player.displayClientMessage(c, false); });
    }

    /**
     * Searches every type in {@code dim} (or just {@code only}) around the player (scaled to the target
     * dimension's coordinates). {@code report} prints the results to chat.
     */
    public void search(ResourceKey<Level> dim, StructureTypes.Type only, Integer n) {
        search(dim, only != null ? List.of(only) : null, n, only != null || n != null);
    }

    /** Type ids ticked in the panel (empty = none ticked). */
    public final Set<String> selected = ConcurrentHashMap.newKeySet();

    /** Search only the given types (null = every type in the dimension). */
    public void search(ResourceKey<Level> dim, List<StructureTypes.Type> only, Integer n, boolean print) {
        var mc = mc();
        var p = mc.player;
        if (p == null) return;
        int per = n != null ? n : count.i();
        int rings = radius.i();
        // Coordinates in the target dimension (Nether is 1:8).
        double scale = p.level().dimensionType().coordinateScale() / (dim == Level.NETHER ? 8.0 : 1.0);
        Vec3 origin = new Vec3(p.getX() * scale, p.getY(), p.getZ() * scale);
        int bx = (int) Math.floor(origin.x), bz = (int) Math.floor(origin.z);
        List<StructureTypes.Type> types = only != null && !only.isEmpty() ? List.copyOf(only) : StructureTypes.forDim(dim);
        var server = mc.getSingleplayerServer();
        Long sd = seed;
        if (server == null && sd == null) {
            status = "Multiplayer: set the world seed with .seed <seed>";
            chat(Component.literal("[Structure Finder] Set the server's seed first: .seed <seed>  (ask the owner or run /seed)").withStyle(ChatFormatting.YELLOW));
            return;
        }
        int gen = GEN.incrementAndGet();
        busy = true;
        status = "Searching " + dim.location().getPath() + "...";
        WORKER.submit(() -> {
            Map<String, List<StructureSearch.Hit>> res = new LinkedHashMap<>();
            try {
                FinderEnv env = null;
                String mode;
                if (server != null) {
                    var lvl = server.getLevel(dim);
                    if (lvl == null) throw new IllegalStateException("dimension not loaded");
                    mode = "singleplayer";
                    for (var t : types) {
                        if (GEN.get() != gen) return;
                        // world-gen objects of the live server: query on its own thread, like /locate
                        var hits = server.submit(() -> new StructureSearch(FinderEnv.of(lvl)).nearest(t, bx, bz, per, rings, () -> GEN.get() != gen)).get(60, TimeUnit.SECONDS);
                        res.put(t.id(), hits);
                    }
                } else {
                    try {
                        env = SeedWorld.env(sd, dim);
                        mode = "seed " + sd;
                    } catch (Throwable ex) {
                        Nebula.LOG.error("Structure Finder: could not build vanilla worldgen for seed {}", sd, ex);
                        mode = "seed " + sd + " (approx: no biome check)";
                    }
                    StructureSearch ss = env == null ? null : new StructureSearch(env);
                    for (var t : types) {
                        if (GEN.get() != gen) return;
                        res.put(t.id(), ss != null ? ss.nearest(t, bx, bz, per, rings, () -> GEN.get() != gen)
                                : ApproxPlacements.nearest(t, sd, bx, bz, per));
                    }
                }
                if (GEN.get() != gen) return;
                results.put(dim, res);
                origins.put(dim, new Vec3(p.getX(), p.getY(), p.getZ()));
                int total = res.values().stream().mapToInt(List::size).sum();
                status = "Found " + total + " in " + dim.location().getPath() + " (" + mode + ")";
                if (print) printResults(dim, types);
                if (dim == Level.OVERWORLD && portalRoom.get() && !res.getOrDefault("stronghold", List.of()).isEmpty()
                        && types.stream().anyMatch(t -> t.id().equals("stronghold"))) WORKER.submit(() -> portalTask(false));
                else Notifications.push("Structure Finder", status, true);
            } catch (Throwable ex) {
                Nebula.LOG.error("Structure Finder search failed", ex);
                status = "Failed: " + ex.getMessage();
            } finally {
                if (GEN.get() == gen) busy = false;
            }
        });
    }

    public void printResults(ResourceKey<Level> dim, List<StructureTypes.Type> types) {
        var res = results.get(dim);
        if (res == null) return;
        chat(Component.literal("[Structure Finder] " + dim.location().getPath() + ":").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (var t : types) {
            var hits = res.getOrDefault(t.id(), List.of());
            if (hits.isEmpty()) { chat(Component.literal("  " + t.name() + ": none in range").withStyle(ChatFormatting.GRAY)); continue; }
            StringBuilder sb = new StringBuilder("  " + t.name() + ": ");
            for (int i = 0; i < hits.size(); i++) {
                var h = hits.get(i);
                if (i > 0) sb.append(" | ");
                sb.append(h.x()).append(" ~ ").append(h.z()).append(" (").append((int) h.dist()).append("m").append(h.approx() ? ", approx" : "").append(")");
            }
            chat(Component.literal(sb.toString()));
        }
    }

    public static String pinKey(ResourceKey<Level> dim, StructureSearch.Hit h) {
        return dim.location() + "|" + h.x() + "|" + h.z();
    }

    @SubscribeEvent
    public void onRender(RenderLevelStageEvent e) {
        var p = mc().player;
        if (!isEnabled() || p == null || e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        var dim = p.level().dimension();
        var res = results.get(dim);
        if (res == null) return;
        var cam = e.getCamera().getPosition();
        var m = e.getPoseStack().last().pose();
        List<Object[]> draw = new ArrayList<>();
        for (var t : StructureTypes.forDim(dim)) {
            var hits = res.getOrDefault(t.id(), List.of());
            for (int i = 0; i < hits.size(); i++) {
                var h = hits.get(i);
                if (i == 0 || !onlyNearest.get() || pinned.contains(pinKey(dim, h))) draw.add(new Object[]{t, h});
            }
        }
        if (draw.isEmpty()) return;
        RenderSystem.lineWidth(2f);
        var b = World3D.begin(VertexFormat.Mode.DEBUG_LINES, false);
        Vec3 look = Vec3.directionFromRotation(e.getCamera().getXRot(), e.getCamera().getYRot());
        for (var o : draw) {
            var t = (StructureTypes.Type) o[0];
            var h = (StructureSearch.Hit) o[1];
            double x = h.x() + 8 - cam.x, z = h.z() + 8 - cam.z;
            if (beams.get()) {
                World3D.v(b, m, x, -64 - cam.y, z, t.color(), 0.9f);
                World3D.v(b, m, x, 320 - cam.y, z, t.color(), 0.2f);
            }
            if (tracers.get()) {
                World3D.v(b, m, look.x, look.y, look.z, t.color(), 0.9f);
                World3D.v(b, m, x, p.getY() - cam.y, z, t.color(), 0.9f);
            }
        }
        var pp = portalPos;
        if (pp != null && dim == Level.OVERWORLD) {
            double x = pp.getX() + 0.5 - cam.x, z = pp.getZ() + 0.5 - cam.z;
            World3D.v(b, m, x, pp.getY() - cam.y, z, 0xFF33FF66, 0.9f);
            World3D.v(b, m, x, 320 - cam.y, z, 0xFF33FF66, 0.2f);
        }
        World3D.end(b);
        if (pp != null && dim == Level.OVERWORLD) {
            double dist = Math.sqrt(pp.distToCenterSqr(p.position()));
            double k = dist > 64 ? 64 / dist : 1;
            World3D.label(e.getPoseStack(), e.getCamera(), (pp.getX() + 0.5 - cam.x) * k, (pp.getY() + 2 - cam.y) * k, (pp.getZ() + 0.5 - cam.z) * k,
                    String.format("End Portal Room %.0fm", dist), 0xFF33FF66, 1.2f);
        }
        for (var o : draw) {
            var t = (StructureTypes.Type) o[0];
            var h = (StructureSearch.Hit) o[1];
            double x = h.x() + 8 - cam.x, z = h.z() + 8 - cam.z, y = p.getY() + 2 - cam.y;
            double dist = Math.hypot(h.x() + 8 - p.getX(), h.z() + 8 - p.getZ());
            double k = dist > 64 ? 64 / dist : 1;
            World3D.label(e.getPoseStack(), e.getCamera(), x * k, y * k, z * k, t.name() + String.format(" %.0fm", dist) + (h.approx() ? " ~" : ""), t.color(), 1.2f);
        }
    }

    // ------------------------------------------------------------------ end portal room

    /** ".portal": stronghold first (if not known yet), then the portal room inside it. */
    public void findPortalRoom() {
        var ow = results.get(Level.OVERWORLD);
        if (ow == null || ow.getOrDefault("stronghold", List.of()).isEmpty()) {
            var t = StructureTypes.byId("stronghold");
            chat(Component.literal("[Structure Finder] Finding the nearest stronghold first...").withStyle(ChatFormatting.GRAY));
            boolean was = portalRoom.get();
            search(Level.OVERWORLD, t, 1);
            if (!was) WORKER.submit(() -> portalTask(true)); // queued after the search on the same worker
            return;
        }
        WORKER.submit(() -> portalTask(true));
    }

    private void portalTask(boolean verbose) {
        try {
            var ow = results.get(Level.OVERWORLD);
            if (ow == null || ow.getOrDefault("stronghold", List.of()).isEmpty()) { chat(Component.literal("[Structure Finder] No stronghold found.").withStyle(ChatFormatting.GRAY)); return; }
            var h = ow.get("stronghold").get(0);
            var mc = mc();
            var server = mc.getSingleplayerServer();
            int[] r = null;
            String how;
            if (server != null) {
                var lvl = server.getLevel(Level.OVERWORLD);
                r = server.submit(() -> portalFromStart(lvl, h.x() >> 4, h.z() >> 4)).get(60, TimeUnit.SECONDS);
                how = "world generator";
            } else {
                // multiplayer: only blocks the client has loaded can be checked
                r = mc.submit(() -> scanLoaded(h.x() >> 4, h.z() >> 4, 10)).get(30, TimeUnit.SECONDS);
                how = "loaded chunks";
            }
            if (r == null) {
                portalPos = null;
                portalInfo = null;
                String why = server != null ? "couldn't locate the portal room piece" : "portal room isn't in your loaded chunks yet - go near the stronghold (" + h.x() + " ~ " + h.z() + ") and run .portal again";
                chat(Component.literal("[Structure Finder] Stronghold at " + h.x() + " ~ " + h.z() + "; End portal room: " + why).withStyle(ChatFormatting.YELLOW));
                return;
            }
            portalPos = new net.minecraft.core.BlockPos(r[0], r[1], r[2]);
            String frames = r[3] > 0 ? r[3] + " frames" + (r[4] > 0 ? ", " + r[4] + " with eyes" : "") : "room bounds only";
            portalInfo = "End Portal Room " + r[0] + " " + r[1] + " " + r[2] + " (" + frames + ")";
            chat(Component.literal("[Structure Finder] Stronghold (start): " + h.x() + " ~ " + h.z()).withStyle(ChatFormatting.LIGHT_PURPLE));
            chat(Component.literal("[Structure Finder] End PORTAL ROOM: " + r[0] + " " + r[1] + " " + r[2] + "  (" + frames + ", via " + how + ")").withStyle(ChatFormatting.GREEN));
        } catch (Throwable ex) {
            Nebula.LOG.error("portal room search failed", ex);
            chat(Component.literal("[Structure Finder] Portal room search failed: " + ex.getMessage()).withStyle(ChatFormatting.RED));
        }
    }

    /** {x,y,z,frames,eyes} of the portal room in the stronghold started near chunk (cx,cz), from the server's structure start. */
    public static int[] portalFromStart(net.minecraft.server.level.ServerLevel lvl, int cx, int cz) {
        var reg = lvl.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var st = reg.get(net.minecraft.world.level.levelgen.structure.BuiltinStructures.STRONGHOLD);
        if (st == null) return null;
        // spiral out over the starting chunks
        for (int rad = 0; rad <= 2; rad++) for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) continue;
            var chunk = lvl.getChunk(cx + dx, cz + dz, net.minecraft.world.level.chunk.ChunkStatus.STRUCTURE_STARTS);
            var start = chunk.getStartForStructure(st);
            if (start == null || !start.isValid()) continue;
            for (var piece : start.getPieces()) {
                if (!(piece instanceof net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces.PortalRoom)) continue;
                var bb = piece.getBoundingBox();
                long sx = 0, sy = 0, sz = 0; int n = 0, eyes = 0;
                for (int x = bb.minX(); x <= bb.maxX(); x++) for (int z = bb.minZ(); z <= bb.maxZ(); z++) {
                    var ch = lvl.getChunk(x >> 4, z >> 4); // generates the room's chunks fully
                    for (int y = bb.minY(); y <= bb.maxY(); y++) {
                        var bs = ch.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                        if (bs.is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME)) {
                            sx += x; sy += y; sz += z; n++;
                            if (bs.getValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE)) eyes++;
                        }
                    }
                }
                if (n > 0) return new int[]{(int) Math.floorDiv(sx, n), (int) (sy / n), (int) Math.floorDiv(sz, n), n, eyes};
                return new int[]{(bb.minX() + bb.maxX()) / 2, bb.minY() + 3, (bb.minZ() + bb.maxZ()) / 2, 0, 0};
            }
        }
        return null;
    }

    /** Spiral over client-loaded chunks around (cx,cz) looking for end portal frames. Client thread. */
    private static int[] scanLoaded(int cx, int cz, int maxRad) {
        var lvl = net.minecraft.client.Minecraft.getInstance().level;
        if (lvl == null) return null;
        var src = lvl.getChunkSource();
        for (int rad = 0; rad <= maxRad; rad++) for (int dx = -rad; dx <= rad; dx++) for (int dz = -rad; dz <= rad; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != rad) continue;
            var ch = src.getChunk(cx + dx, cz + dz, net.minecraft.world.level.chunk.ChunkStatus.FULL, false);
            if (ch == null) continue;
            long sx = 0, sy = 0, sz = 0; int n = 0, eyes = 0;
            var secs = ch.getSections();
            for (int si = 0; si < secs.length; si++) {
                var sec = secs[si];
                if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(b -> b.is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME))) continue;
                int baseY = lvl.getSectionYFromSectionIndex(si) << 4;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    var bs = sec.getBlockState(x, y, z);
                    if (bs.is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME)) {
                        sx += ch.getPos().getMinBlockX() + x; sy += baseY + y; sz += ch.getPos().getMinBlockZ() + z; n++;
                        if (bs.getValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE)) eyes++;
                    }
                }
            }
            // frames of one room can straddle a chunk edge; good enough to point you at it
            if (n > 0) return new int[]{(int) (sx / n), (int) (sy / n), (int) (sz / n), n, eyes};
        }
        return null;
    }
}
