package org.betterx.betterend.mixin.common;

import org.betterx.betterend.interfaces.BETargetChecker;
import org.betterx.betterend.world.generator.TerrainGenerator;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Generates BetterEnd's island terrain in place of the vanilla End density.
 * <p>
 * Before the 26.3 release this was {@code NoiseChunkMixin}, which cancelled {@code NoiseChunk#fillSlice} and wrote
 * the island density into the noise interpolators. The release removed both (interpolation is now the
 * {@code minecraft:interpolated} density function), so instead the final density the generator samples is
 * swapped for one whose interpolated input is the island terrain - see
 * {@link TerrainGenerator#withIslandTerrain}. Like before, this only happens when the End target flag is set
 * on the {@link NoiseGeneratorSettings}, so the vanilla End and every other dimension are left untouched.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorTerrainMixin {
    @Shadow
    @Final
    private Holder<NoiseGeneratorSettings> settings;

    @ModifyExpressionValue(
            method = {"doFill", "iterateNoiseColumn"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseRouter;finalDensity()Lnet/minecraft/world/level/levelgen/densityfunction/DensityFunction;"
            )
    )
    private DensityFunction be_islandTerrain(DensityFunction finalDensity) {
        final NoiseGeneratorSettings generatorSettings = settings.value();
        if (!BETargetChecker.class.cast(generatorSettings).be_isTarget()) {
            return finalDensity;
        }
        return TerrainGenerator.withIslandTerrain(finalDensity, generatorSettings.noiseSettings());
    }
}
