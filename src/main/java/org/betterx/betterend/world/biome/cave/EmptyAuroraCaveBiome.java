package org.betterx.betterend.world.biome.cave;

import org.betterx.bclib.interfaces.SurfaceMaterialProvider;
import org.betterx.betterend.registry.EndParticles;
import org.betterx.betterend.registry.features.EndPlacedCaveFeatures;
import org.betterx.betterend.world.biome.EndBiome;
import org.betterx.betterend.world.biome.EndBiomeBuilder;
import org.betterx.betterend.world.biome.EndBiomeKey;
import de.ambertation.wover.biome.api.BiomeKey;
import de.ambertation.wover.biome.api.data.BiomeGenerationDataContainer;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceKey;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmptyAuroraCaveBiome extends EndCaveBiome.Config<EmptyAuroraCaveBiome> {
    public static final MapCodec<Biome> CODEC = EndCaveBiome.simpleCaveBiomeCodec(EmptyAuroraCaveBiome.Biome::new);


    public static class Biome extends EndCaveBiome {
        /**
         * 26.3 removed {@code KeyDispatchDataCodec}; the {@code KEY_CODEC} wrapper around
         * {@link EmptyAuroraCaveBiome#CODEC} is gone rather than replaced. See {@link EndBiome#codec()}.
         */
        @Override
        public MapCodec<? extends EndCaveBiome> codec() {
            return EmptyAuroraCaveBiome.CODEC;
        }

        protected Biome(
                float fogDensity,
                @NotNull ResourceKey<net.minecraft.world.level.biome.Biome> biome,
                @NotNull BiomeGenerationDataContainer generatorData,
                float terrainHeight,
                float genChance,
                int edgeSize,
                boolean vertical,
                @Nullable ResourceKey<net.minecraft.world.level.biome.Biome> edge,
                @Nullable ResourceKey<net.minecraft.world.level.biome.Biome> parent,
                boolean hasCaves,
                SurfaceMaterialProvider surface
        ) {
            super(
                    fogDensity, biome, generatorData, terrainHeight,
                    genChance, edgeSize, vertical,
                    edge, parent,
                    hasCaves, surface
            );
        }
    }

    @Override
    public @NotNull EndBiome instantiateBiome(
            float fogDensity,
            BiomeKey<?> key,
            @NotNull BiomeGenerationDataContainer generatorData,
            float terrainHeight,
            float genChance,
            int edgeSize,
            boolean vertical,
            @Nullable ResourceKey<net.minecraft.world.level.biome.Biome> edge,
            @Nullable ResourceKey<net.minecraft.world.level.biome.Biome> parent,
            boolean hasCave,
            SurfaceMaterialProvider surface
    ) {
        return new EmptyAuroraCaveBiome.Biome(fogDensity, key.key, generatorData, terrainHeight, genChance, edgeSize, vertical, edge, parent, hasCave, surface);
    }

    public EmptyAuroraCaveBiome(EndBiomeKey<EmptyAuroraCaveBiome, ?> key) {
        super(key);
    }

    @Override
    public void addCustomBuildData(EndBiomeBuilder builder) {
        super.addCustomBuildData(builder);
        builder.feature(EndPlacedCaveFeatures.STALACTITE_CLUSTER_PLAIN)
               .feature(EndPlacedCaveFeatures.STALAGMITE_SCATTER)
               .feature(EndPlacedCaveFeatures.STALACTITE_SCATTER)
               .feature(EndPlacedCaveFeatures.BIG_AURORA_CRYSTAL);
        builder.fogColor(150, 30, 68)
               .fogDensity(2.0F)
               .plantsColor(108, 25, 46)
               .waterAndFogColor(186, 77, 237)
               .particles(EndParticles.GLOWING_SPHERE, 0.001F);
    }

}
