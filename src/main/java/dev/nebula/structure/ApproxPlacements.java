package dev.nebula.structure;

import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.*;

/**
 * Last-resort fallback when the vanilla worldgen data can't be built: vanilla 1.20.1 grid placements
 * only (spacing / separation / salt), no biome or terrain check. Every result is marked approx: it is a
 * chunk where the structure *can* attempt to spawn, not one where it did.
 */
public final class ApproxPlacements {
    private record P(int spacing, int separation, int salt, boolean triangular, int offX, int offZ) {}

    private static final Map<String, P> TABLE = Map.ofEntries(
            Map.entry("village", new P(34, 8, 10387312, false, 0, 0)),
            Map.entry("desert_pyramid", new P(32, 8, 14357617, false, 0, 0)),
            Map.entry("igloo", new P(32, 8, 14357618, false, 0, 0)),
            Map.entry("jungle_pyramid", new P(32, 8, 14357619, false, 0, 0)),
            Map.entry("swamp_hut", new P(32, 8, 14357620, false, 0, 0)),
            Map.entry("ancient_city", new P(24, 8, 20083232, false, 0, 0)),
            Map.entry("monument", new P(32, 5, 10387313, true, 0, 0)),
            Map.entry("mansion", new P(80, 20, 10387319, true, 0, 0)),
            Map.entry("shipwreck", new P(24, 4, 165745295, false, 0, 0)),
            Map.entry("ocean_ruin", new P(20, 8, 14357621, false, 0, 0)),
            Map.entry("trail_ruins", new P(34, 8, 83469867, false, 0, 0)),
            Map.entry("ruined_portal", new P(40, 15, 34222645, false, 0, 0)),
            Map.entry("ruined_portal_nether", new P(40, 15, 34222645, false, 0, 0)),
            Map.entry("fortress", new P(27, 4, 30084232, false, 0, 0)),
            Map.entry("bastion_remnant", new P(27, 4, 30084232, false, 0, 0)),
            Map.entry("nether_fossil", new P(2, 1, 14357921, false, 0, 0)),
            Map.entry("end_city", new P(20, 11, 10387313, true, 0, 0)));

    public static boolean supports(StructureTypes.Type t) {
        return TABLE.containsKey(t.id());
    }

    public static List<StructureSearch.Hit> nearest(StructureTypes.Type t, long seed, int bx, int bz, int n) {
        P p = TABLE.get(t.id());
        if (p == null) return List.of();
        var rs = new RandomSpreadStructurePlacement(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1f, p.salt(),
                Optional.empty(), p.spacing(), p.separation(), p.triangular() ? RandomSpreadType.TRIANGULAR : RandomSpreadType.LINEAR);
        int ox = SectionPos.blockToSectionCoord(bx), oz = SectionPos.blockToSectionCoord(bz);
        List<StructureSearch.Hit> out = new ArrayList<>();
        int rings = Math.max(2, (int) Math.ceil(Math.sqrt(n)) + 2);
        for (int k = -rings; k <= rings; k++)
            for (int l = -rings; l <= rings; l++) {
                ChunkPos cp = rs.getPotentialStructureChunk(seed, ox + p.spacing() * k, oz + p.spacing() * l);
                int x = cp.getMinBlockX(), z = cp.getMinBlockZ();
                double d = Math.hypot(x - bx, z - bz);
                out.add(new StructureSearch.Hit(t.name(), t.structures().get(0), x, z, d, true));
            }
        out.sort(Comparator.comparingDouble(StructureSearch.Hit::dist));
        return out.subList(0, Math.min(n, out.size()));
    }

    private ApproxPlacements() {}
}
