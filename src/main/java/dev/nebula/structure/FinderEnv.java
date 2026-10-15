package dev.nebula.structure;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Everything needed to decide whether a structure starts in a chunk, without loading chunks. */
public record FinderEnv(RegistryAccess registryAccess, ChunkGenerator generator, BiomeSource biomeSource, RandomState randomState,
                        StructureTemplateManager templates, ChunkGeneratorStructureState state, long seed,
                        LevelHeightAccessor height, boolean serverBacked) {

    /** The real integrated/dedicated server level (same data /locate uses). Must be used on the server thread. */
    public static FinderEnv of(ServerLevel level) {
        var cs = level.getChunkSource();
        return new FinderEnv(level.registryAccess(), cs.getGenerator(), cs.getGenerator().getBiomeSource(), cs.randomState(),
                level.getServer().getStructureManager(), cs.getGeneratorState(), level.getSeed(), level, true);
    }
}
