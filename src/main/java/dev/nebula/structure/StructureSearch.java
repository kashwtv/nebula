package dev.nebula.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * Finds structure starts the way world generation decides them (ChunkGenerator.createStructures):
 * placement grid (RandomSpread / ConcentricRings) + frequency + exclusion zone, then the structure
 * set's weighted pick, then the structure's own biome/terrain check (findValidGenerationPoint).
 * No chunks are generated or loaded.
 */
public final class StructureSearch {
    public record Hit(String type, ResourceLocation id, int x, int z, double dist, boolean approx) {}

    private final FinderEnv env;
    private final Map<Long, Map<ResourceLocation, Boolean>> validCache = new HashMap<>();

    public StructureSearch(FinderEnv env) {
        this.env = env;
    }

    private Holder<Structure> holder(ResourceLocation id) {
        return env.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
    }

    private boolean valid(Holder<Structure> s, ChunkPos cp) {
        ResourceLocation id = s.unwrapKey().map(ResourceKey::location).orElse(null);
        Map<ResourceLocation, Boolean> m = validCache.computeIfAbsent(cp.toLong(), k -> new HashMap<>());
        Boolean b = m.get(id);
        if (b != null) return b;
        Structure st = s.value();
        var ctx = new Structure.GenerationContext(env.registryAccess(), env.generator(), env.biomeSource(), env.randomState(),
                env.templates(), env.seed(), cp, env.height(), st.biomes()::contains);
        boolean r;
        try { r = st.findValidGenerationPoint(ctx).isPresent(); } catch (RuntimeException ex) { r = false; }
        m.put(id, r);
        return r;
    }

    /** True when world generation would place {@code target} (from {@code set}) starting in chunk cp. */
    private boolean generates(StructureSet set, Holder<Structure> target, ChunkPos cp) {
        if (!set.placement().isStructureChunk(env.state(), cp.x, cp.z)) return false;
        List<StructureSet.StructureSelectionEntry> entries = set.structures();
        if (entries.size() == 1) return same(entries.get(0).structure(), target) && valid(target, cp);
        List<StructureSet.StructureSelectionEntry> list = new ArrayList<>(entries);
        WorldgenRandom r = new WorldgenRandom(new LegacyRandomSource(0L));
        r.setLargeFeatureSeed(env.seed(), cp.x, cp.z);
        int total = 0;
        for (var e : list) total += e.weight();
        while (!list.isEmpty()) {
            int j = r.nextInt(total);
            int k = 0;
            for (var e : list) { j -= e.weight(); if (j < 0) break; k++; }
            var e = list.get(k);
            if (valid(e.structure(), cp)) return same(e.structure(), target);
            list.remove(k);
            total -= e.weight();
        }
        return false;
    }

    private static boolean same(Holder<Structure> a, Holder<Structure> b) {
        return a.unwrapKey().equals(b.unwrapKey());
    }

    private List<StructureSet> setsFor(Holder<Structure> s) {
        List<StructureSet> out = new ArrayList<>();
        for (Holder<StructureSet> h : env.state().possibleStructureSets())
            for (var e : h.value().structures()) if (same(e.structure(), s)) { out.add(h.value()); break; }
        return out;
    }

    /** Nearest {@code n} starts of any of the type's structures around (bx, bz), sorted by distance. */
    public List<Hit> nearest(StructureTypes.Type type, int bx, int bz, int n, int maxRings, BooleanSupplier cancel) {
        Map<Long, Hit> hits = new HashMap<>();
        for (ResourceLocation id : type.structures()) {
            Holder<Structure> s = holder(id);
            if (s == null) continue;
            for (StructureSet set : setsFor(s)) {
                StructurePlacement p = set.placement();
                if (p instanceof ConcentricRingsStructurePlacement c) {
                    for (ChunkPos cp : env.state().getRingPositionsFor(c)) {
                        if (cancel.getAsBoolean()) return List.of();
                        if (generates(set, s, cp)) put(hits, type, id, p.getLocatePos(cp), bx, bz);
                    }
                } else if (p instanceof RandomSpreadStructurePlacement rs) {
                    int ox = SectionPos.blockToSectionCoord(bx), oz = SectionPos.blockToSectionCoord(bz);
                    int sp = rs.spacing();
                    List<Double> mine = new ArrayList<>();
                    for (int r = 0; r <= maxRings; r++) {
                        for (int k = -r; k <= r; k++) {
                            boolean ek = k == -r || k == r;
                            for (int l = -r; l <= r; l++) {
                                if (!ek && l != -r && l != r) continue;
                                if (cancel.getAsBoolean()) return List.of();
                                ChunkPos cp = rs.getPotentialStructureChunk(env.seed(), ox + sp * k, oz + sp * l);
                                if (generates(set, s, cp)) mine.add(put(hits, type, id, p.getLocatePos(cp), bx, bz));
                            }
                        }
                        if (mine.size() >= n) {
                            mine.sort(null);
                            if ((r - 1) * sp * 16.0 - 16 > mine.get(n - 1)) break;
                        }
                    }
                }
            }
        }
        List<Hit> out = new ArrayList<>(hits.values());
        out.sort(Comparator.comparingDouble(Hit::dist));
        return out.size() > n ? new ArrayList<>(out.subList(0, n)) : out;
    }

    private static double put(Map<Long, Hit> hits, StructureTypes.Type t, ResourceLocation id, BlockPos pos, int bx, int bz) {
        double d = Math.sqrt((double) (pos.getX() - bx) * (pos.getX() - bx) + (double) (pos.getZ() - bz) * (pos.getZ() - bz));
        hits.putIfAbsent(BlockPos.asLong(pos.getX(), 0, pos.getZ()), new Hit(t.name(), id, pos.getX(), pos.getZ(), d, false));
        return d;
    }

    /**
     * Exactly mirrors /locate's search order (ChunkGenerator.findNearestMapStructure, radius in rings):
     * for random-spread placements the first hit on the first ring that has one, then the nearest across
     * placements. Used to compare against the server's /locate.
     */
    public BlockPos locateLike(ResourceLocation id, BlockPos origin, int radius) {
        Holder<Structure> s = holder(id);
        if (s == null) return null;
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (StructureSet set : setsFor(s)) {
            StructurePlacement p = set.placement();
            BlockPos found = null;
            if (p instanceof ConcentricRingsStructurePlacement c) {
                double d0 = Double.MAX_VALUE;
                for (ChunkPos cp : env.state().getRingPositionsFor(c)) {
                    if (!generates(set, s, cp)) continue;
                    BlockPos lp = p.getLocatePos(cp);
                    double d = lp.distSqr(origin);
                    if (d < d0) { d0 = d; found = lp; }
                }
            } else if (p instanceof RandomSpreadStructurePlacement rs) {
                int ox = SectionPos.blockToSectionCoord(origin.getX()), oz = SectionPos.blockToSectionCoord(origin.getZ());
                int sp = rs.spacing();
                outer:
                for (int r = 0; r <= radius; r++) {
                    for (int k = -r; k <= r; k++) {
                        boolean ek = k == -r || k == r;
                        for (int l = -r; l <= r; l++) {
                            if (!ek && l != -r && l != r) continue;
                            ChunkPos cp = rs.getPotentialStructureChunk(env.seed(), ox + sp * k, oz + sp * l);
                            if (generates(set, s, cp)) { found = p.getLocatePos(cp); break outer; }
                        }
                    }
                }
            }
            if (found != null && found.distSqr(origin) < bestD) { bestD = found.distSqr(origin); best = found; }
        }
        return best;
    }
}
