package org.betterx.betterend.world.generator;

import org.betterx.bclib.api.v2.generator.BCLibEndBiomeSource;
import org.betterx.bclib.api.v2.generator.config.BCLEndBiomeSourceConfig;
import org.betterx.betterend.BetterEnd;
import org.betterx.worlds.together.world.event.WorldBootstrap;
import org.betterx.worlds.together.worldPreset.TogetherWorldPreset;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * The empty ring around the central End island (quiqueck/BetterEnd#288).
 * <p>
 * Its size is part of the End settings every world stores in level.dat ({@code inner_void_radius_squared} of the
 * End biome source, in blocks squared). The {@code voidRingSize} option of BetterEnd's generator config is the
 * radius (in blocks) a <b>new</b> world starts with; it is written into that world's settings when the world is
 * created, so changing the option later does not tear up existing worlds. To change an existing world, edit
 * {@code inner_void_radius_squared} in its level.dat (WorldGenSettings / dimensions / minecraft:the_end / generator /
 * biome_source / config).
 * <p>
 * Both terrain generators follow the stored value: BetterEnd's own island layers and the vanilla End terrain (whose
 * outer islands otherwise always start 1024 blocks from the center).
 */
public final class VoidRing {
    /**
     * The value {@code voidRingSize} shipped with; it leaves new worlds at the preset's radius.
     */
    public static final int DEFAULT_CONFIG_SIZE = 1000;
    /**
     * Vanilla's outer-island limit: island cells (16 blocks) closer than sqrt(4096) * 16 = 1024 blocks stay empty.
     */
    public static final long VANILLA_ISLAND_LIMIT = 4096L;

    private static volatile long vanillaIslandLimit = VANILLA_ISLAND_LIMIT;
    private static volatile long terrainRadiusSqr = (long) DEFAULT_CONFIG_SIZE * DEFAULT_CONFIG_SIZE;

    private VoidRing() {
    }

    /**
     * Used by the vanilla End island density function in place of its hard-coded {@code 4096}.
     */
    public static long vanillaIslandLimit() {
        return vanillaIslandLimit;
    }

    /**
     * The squared radius BetterEnd's island layers keep free of islands.
     */
    public static long terrainRadiusSqr() {
        return terrainRadiusSqr;
    }

    /**
     * Called when the End level of a server starts: adopt the ring size stored with that world.
     */
    public static void onEndLevelInit(BiomeSource biomeSource) {
        if (biomeSource instanceof BCLibEndBiomeSource bcl) {
            final long radiusSqr = bcl.getTogetherConfig().innerVoidRadiusSquared;
            vanillaIslandLimit = Math.max(0, radiusSqr) / 256L;
            // Worlds that kept the default radius keep the island layers they were generated with: BetterEnd's
            // own terrain always used voidRingSize (1000 blocks by default) instead of the stored radius.
            terrainRadiusSqr = radiusSqr == BCLEndBiomeSourceConfig.DEFAULT.innerVoidRadiusSquared
                    ? GeneratorOptions.getIslandDistBlockSqr()
                    : radiusSqr;
        } else {
            vanillaIslandLimit = VANILLA_ISLAND_LIMIT;
            terrainRadiusSqr = GeneratorOptions.getIslandDistBlockSqr();
        }
    }

    /**
     * {@code WorldEvents.ADAPT_WORLD_PRESET}: only fires while a new world is created. Writes the configured
     * {@code voidRingSize} into the End settings of that world, unless the radius was already changed (for example on
     * the world setup screen).
     */
    public static Holder<WorldPreset> adaptWorldPreset(Holder<WorldPreset> currentPreset, WorldDimensions worldDims) {
        final int size = GeneratorOptions.getIslandDistBlock();
        if (size == DEFAULT_CONFIG_SIZE || size <= 0) return currentPreset;
        final int radiusSqr = (int) Math.min(Integer.MAX_VALUE, (long) size * size);

        final Set<BCLibEndBiomeSource> sources = Collections.newSetFromMap(new IdentityHashMap<>());
        if (currentPreset != null && currentPreset.value() instanceof TogetherWorldPreset preset) {
            addSource(sources, preset.getDimension(LevelStem.END));
        }
        if (worldDims != null) {
            addSource(sources, worldDims.dimensions().get(LevelStem.END));
        }
        final RegistryAccess access = WorldBootstrap.getLastRegistryAccess();
        if (access != null) {
            access.registry(Registries.LEVEL_STEM).ifPresent(r -> addSource(sources, r.get(LevelStem.END)));
        }

        for (BCLibEndBiomeSource source : sources) {
            final BCLEndBiomeSourceConfig cfg = source.getTogetherConfig();
            if (cfg.innerVoidRadiusSquared != BCLEndBiomeSourceConfig.DEFAULT.innerVoidRadiusSquared) continue;
            source.setTogetherConfig(new BCLEndBiomeSourceConfig(
                    cfg.mapVersion,
                    cfg.generatorVersion,
                    cfg.withVoidBiomes,
                    radiusSqr,
                    cfg.centerBiomesSize,
                    cfg.voidBiomesSize,
                    cfg.landBiomesSize,
                    cfg.barrensBiomesSize
            ));
        }
        if (!sources.isEmpty()) {
            BetterEnd.LOGGER.info("New world: End void ring radius set to " + size + " blocks (voidRingSize).");
        }
        return currentPreset;
    }

    private static void addSource(Set<BCLibEndBiomeSource> sources, LevelStem stem) {
        if (stem != null && stem.generator().getBiomeSource() instanceof BCLibEndBiomeSource bcl) {
            sources.add(bcl);
        }
    }
}
