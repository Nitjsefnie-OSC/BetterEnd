package org.betterx.betterend.world.biome.cave;

import org.betterx.bclib.interfaces.SurfaceMaterialProvider;
import org.betterx.betterend.registry.EndCarvers;
import org.betterx.betterend.registry.EndSounds;
import org.betterx.betterend.world.biome.EndBiome;
import org.betterx.betterend.world.biome.EndBiomeBuilder;
import org.betterx.betterend.world.biome.EndBiomeKey;
import de.ambertation.wover.biome.api.BiomeKey;
import de.ambertation.wover.biome.api.data.BiomeGenerationDataContainer;
import de.ambertation.wover.generator.api.biomesource.WoverBiomeData;

import com.mojang.datafixers.util.Function11;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EndCaveBiome extends EndBiome {
    public static final MapCodec<EndCaveBiome> CODEC = simpleCaveBiomeCodec(EndCaveBiome::new);


    public static <T extends EndCaveBiome> MapCodec<T> simpleCaveBiomeCodec(final Function11<Float, ResourceKey<Biome>, BiomeGenerationDataContainer, Float, Float, Integer, Boolean, ResourceKey<Biome>, ResourceKey<Biome>, Boolean, SurfaceMaterialProvider, T> factory) {
        return codec(
                Codec.BOOL.fieldOf("has_caves").orElse(true).forGetter(EndBiome::hasCaves),
                SurfaceMaterialProvider.CODEC.fieldOf("surface")
                                             .orElse(new DefaultSurfaceMaterialProvider())
                                             .forGetter(o -> o.surfMatProv),
                factory
        );
    }

    /**
     * 26.3 removed {@code KeyDispatchDataCodec}; the {@code KEY_CODEC} constant that wrapped
     * {@link #CODEC} is gone rather than replaced. See {@link EndBiome#codec()}.
     */
    @Override
    public MapCodec<? extends WoverBiomeData> codec() {
        return CODEC;
    }


    protected EndCaveBiome(
            float fogDensity,
            @NotNull ResourceKey<Biome> biome,
            @NotNull BiomeGenerationDataContainer generatorData,
            float terrainHeight,
            float genChance,
            int edgeSize,
            boolean vertical,
            @Nullable ResourceKey<Biome> edge,
            @Nullable ResourceKey<Biome> parent,
            boolean hasCaves,
            SurfaceMaterialProvider surface
    ) {
        super(
                fogDensity, biome, generatorData, terrainHeight,
                genChance, edgeSize, vertical,
                edge, parent,
                hasCaves,
                surface
        );
    }

    public static abstract class Config<C extends Config<?>> extends EndBiome.Config {
        protected Config(EndBiomeKey<C, ?> key) {
            super();
        }


        @Override
        public void addCustomBuildData(EndBiomeBuilder builder) {
            builder.carver(EndCarvers.ROUND_CAVE)
                   .carver(EndCarvers.TUNNEL_CAVE)
                   .music(EndSounds.MUSIC_CAVES)
                   .loop(EndSounds.AMBIENT_CAVES);
        }

        @Override
        public boolean hasCaves() {
            return false;
        }

        @Override
        public boolean hasReturnGateway() {
            return false;
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
                @Nullable ResourceKey<Biome> edge,
                @Nullable ResourceKey<Biome> parent,
                boolean hasCave,
                SurfaceMaterialProvider surface
        ) {
            return new EndCaveBiome(
                    fogDensity, key.key, generatorData,
                    terrainHeight, genChance, edgeSize, vertical, edge, parent,
                    hasCave, surface
            );
        }

    }

    // Per-biome cave surface materials (ceiling / wall). These used to be consumed by CaveSurfaceCoater,
    // which ran at carve time and wrote them into the blocks a carver had just removed. 26.3's carvers can
    // no longer touch the world at all - they only fill a CarvingMask - so the coater is gone (see
    // EndCaveCarver's class doc). They are now read by CaveSurfaceCoatFeature, the decoration-time coat that
    // EndFeatures#addDefaultFeatures puts on every biome; together with getTopMaterial() they remain the
    // single description of what a cave is made of, so overriding one of them here is all a new cave biome
    // has to do. Returning null means "leave this face alone".
    public BlockState getCeil(BlockPos pos) {
        return null;
    }

    public BlockState getWall(BlockPos pos) {
        return null;
    }
}
