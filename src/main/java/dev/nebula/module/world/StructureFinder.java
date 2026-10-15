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
        if (mc().player != null) search(mc().player.level().dimension(), null, null);
    }

    @Override
    public void onTick() {
        var p = mc().player;
        if (p == null || busy || !autoRefresh.get()) return;
        var dim = p.level().dimension();
        Vec3 o = origins.get(dim);
        if (o != null && o.distanceTo(p.position()) > 512) search(dim, null, null);
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
        var mc = mc();
        var p = mc.player;
        if (p == null) return;
        int per = n != null ? n : count.i();
        int rings = radius.i();
        // Coordinates in the target dimension (Nether is 1:8).
        double scale = p.level().dimensionType().coordinateScale() / (dim == Level.NETHER ? 8.0 : 1.0);
        Vec3 origin = new Vec3(p.getX() * scale, p.getY(), p.getZ() * scale);
        int bx = (int) Math.floor(origin.x), bz = (int) Math.floor(origin.z);
        List<StructureTypes.Type> types = only != null ? List.of(only) : StructureTypes.forDim(dim);
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
        boolean print = only != null || n != null;
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
        World3D.end(b);
        for (var o : draw) {
            var t = (StructureTypes.Type) o[0];
            var h = (StructureSearch.Hit) o[1];
            double x = h.x() + 8 - cam.x, z = h.z() + 8 - cam.z, y = p.getY() + 2 - cam.y;
            double dist = Math.hypot(h.x() + 8 - p.getX(), h.z() + 8 - p.getZ());
            double k = dist > 64 ? 64 / dist : 1;
            World3D.label(e.getPoseStack(), e.getCamera(), x * k, y * k, z * k, t.name() + String.format(" %.0fm", dist) + (h.approx() ? " ~" : ""), t.color(), 1.2f);
        }
    }
}
