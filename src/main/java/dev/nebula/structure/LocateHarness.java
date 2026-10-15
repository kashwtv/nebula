package dev.nebula.structure;

import dev.nebula.Nebula;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only accuracy test (never active in the release jar): when the server's run dir contains
 * "nebula_structtest", compares the offline seed engine against the server's own /locate for every
 * structure at several origins, appends results to nebula_structtest_results.txt, then flags DONE so the dev client (DevAutomation) moves on to the next seed.
 */
public final class LocateHarness {
    private LocateHarness() {}

    /** Set when a run finished; the dev client then closes the world and opens the next seed. */
    public static volatile boolean DONE;

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        var server = e.getServer();
        Path dir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get();
        Nebula.LOG.info("STRUCTTEST check {} {}", dir, Files.exists(dir.resolve("nebula_structtest")));
        if (!Files.exists(dir.resolve("nebula_structtest"))) return;
        List<String> out = new ArrayList<>();
        int total = 0, exact = 0, inTop = 0, serverEngine = 0;
        int[][] origins = {{0, 0}, {2500, -1800}};
        long seed = server.overworld().getSeed();
        for (StructureTypes.Type t : StructureTypes.ALL) {
            ServerLevel lvl = server.getLevel(t.dim());
            if (lvl == null) continue;
            for (ResourceLocation id : t.structures()) {
                var holder = lvl.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
                if (holder == null) { out.add("MISSING " + id); continue; }
                for (int[] o : origins) {
                    if (!t.dim().equals(net.minecraft.world.level.Level.OVERWORLD) && o[0] != 0) continue; // fewer origins off-overworld
                    BlockPos origin = new BlockPos(o[0], 64, o[1]);
                    long t0 = System.currentTimeMillis();
                    var real = lvl.getChunkSource().getGenerator().findNearestMapStructure(lvl, HolderSet.direct(holder), origin, 100, false);
                    BlockPos rp = real == null ? null : real.getFirst();
                    BlockPos mine = null, srv = null;
                    boolean top = false;
                    String err = "";
                    try {
                        StructureSearch ss = new StructureSearch(SeedWorld.env(seed, t.dim()));
                        mine = ss.locateLike(id, origin, 100);
                        StructureTypes.Type single = new StructureTypes.Type(t.name(), t.id(), t.dim(), t.color(), List.of(id));
                        for (var h : ss.nearest(single, o[0], o[1], 5, 100, () -> false))
                            if (rp != null && h.x() == rp.getX() && h.z() == rp.getZ()) top = true;
                        srv = new StructureSearch(FinderEnv.of(lvl)).locateLike(id, origin, 100);
                    } catch (Exception ex) {
                        err = " ERR " + ex;
                        Nebula.LOG.error("structtest engine failed {}", id, ex);
                    }
                    total++;
                    boolean ok = rp == null ? mine == null : mine != null && rp.getX() == mine.getX() && rp.getZ() == mine.getZ();
                    if (ok) exact++;
                    if (top || (rp == null && mine == null)) inTop++;
                    if (rp == null ? srv == null : srv != null && rp.getX() == srv.getX() && rp.getZ() == srv.getZ()) serverEngine++;
                    out.add(String.format("%s %s origin=%d,%d locate=%s seedEngine=%s serverEngine=%s %s top5=%s (%dms)%s", ok ? "MATCH" : "DIFF ",
                            id.getPath(), o[0], o[1], fmt(rp), fmt(mine), fmt(srv), ok ? "" : "", top, System.currentTimeMillis() - t0, err));
                    Nebula.LOG.info("STRUCTTEST {}", out.get(out.size() - 1));
                }
            }
        }
        out.add(0, String.format("SEED %d: exact %d/%d, locate-in-top5 %d/%d, server-env engine %d/%d", seed, exact, total, inTop, total, serverEngine, total));
        try {
            Files.write(dir.resolve("nebula_structtest_results.txt"), (String.join("\n", out) + "\n\n").getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ex) { Nebula.LOG.error("structtest write failed", ex); }
        Nebula.LOG.info("STRUCTTEST {}", out.get(0));
        DONE = true;
    }

    private static String fmt(BlockPos p) {
        return p == null ? "none" : p.getX() + "," + p.getZ();
    }
}
