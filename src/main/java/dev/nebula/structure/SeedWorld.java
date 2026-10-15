package dev.nebula.structure;

import com.mojang.datafixers.DataFixer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagManager;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Builds the vanilla "Default" world (worldgen data from the game jar: noise settings, multi-noise
 * biome parameters, structure sets, template pools, structure templates, biome tags) for any seed,
 * entirely offline. Used when the real server isn't available (multiplayer).
 */
public final class SeedWorld {
    private static RegistryAccess.Frozen registries;
    private static MultiPackResourceManager resources;
    private static StructureTemplateManager templates;

    private SeedWorld() {}

    private static synchronized void init() throws Exception {
        if (registries != null) return;
        resources = new MultiPackResourceManager(PackType.SERVER_DATA, List.of(ServerPacksSource.createVanillaPackSource()));
        RegistryAccess.Frozen base = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        RegistryAccess.Frozen wg = RegistryDataLoader.load(resources, base, RegistryDataLoader.WORLDGEN_REGISTRIES);
        wg.registries().forEach(e -> bindTags(e.value()));
        registries = new RegistryAccess.ImmutableRegistryAccess(java.util.stream.Stream.concat(base.registries(), wg.registries())).freeze();
        Path tmp = Files.createTempDirectory("nebula_structfinder");
        LevelStorageSource.LevelStorageAccess access = LevelStorageSource.createDefault(tmp).createAccess("seedworld");
        DataFixer fixer = DataFixers.getDataFixer();
        templates = new StructureTemplateManager(resources, access, fixer, BuiltInRegistries.BLOCK.asLookup());
    }

    private static <T> void bindTags(Registry<T> reg) {
        ResourceKey<? extends Registry<T>> key = reg.key();
        TagLoader<Holder<T>> loader = new TagLoader<>(id -> reg.getHolder(ResourceKey.create(key, id)), TagManager.getTagDir(key));
        Map<ResourceLocation, Collection<Holder<T>>> built = loader.loadAndBuild(resources);
        Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>();
        built.forEach((id, list) -> map.put(TagKey.create(key, id), List.copyOf(list)));
        reg.bindTags(map);
    }

    private static final Map<String, FinderEnv> CACHE = new HashMap<>();

    /** Vanilla default world generator for this seed and dimension. Slow the first time (~seconds). */
    public static synchronized FinderEnv env(long seed, ResourceKey<Level> dim) throws Exception {
        String k = seed + "|" + dim.location();
        FinderEnv cached = CACHE.get(k);
        if (cached != null) return cached;
        init();
        WorldDimensions dims = registries.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).createWorldDimensions();
        ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, dim.location());
        LevelStem stem = dims.get(stemKey).orElseThrow(() -> new IllegalArgumentException("no dimension " + dim.location()));
        ChunkGenerator gen = stem.generator();
        RandomState rs = gen instanceof NoiseBasedChunkGenerator n
                ? RandomState.create(n.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), seed)
                : RandomState.create(net.minecraft.world.level.levelgen.NoiseGeneratorSettings.dummy(), registries.lookupOrThrow(Registries.NOISE), seed);
        var state = gen.createState(registries.lookupOrThrow(Registries.STRUCTURE_SET), rs, seed);
        state.ensureStructuresGenerated();
        DimensionType type = stem.type().value();
        LevelHeightAccessor h = LevelHeightAccessor.create(type.minY(), type.height());
        FinderEnv env = new FinderEnv(registries, gen, gen.getBiomeSource(), rs, templates, state, seed, h, false);
        if (CACHE.size() > 6) CACHE.clear();
        CACHE.put(k, env);
        return env;
    }

    /** Vanilla seed parsing (WorldOptions.parseSeed): numbers as-is, other text hashed. */
    public static long parseSeed(String s) {
        s = s.trim();
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return s.hashCode(); }
    }
}
