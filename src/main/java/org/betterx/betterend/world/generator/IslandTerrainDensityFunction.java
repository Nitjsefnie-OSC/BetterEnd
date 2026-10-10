package org.betterx.betterend.world.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

import java.util.Arrays;

/**
 * BetterEnd's island terrain ({@link TerrainGenerator#fillTerrainDensity}) as a vanilla density function.
 * <p>
 * Up to the 26.3 snapshots BetterEnd replaced the End terrain by cancelling {@code NoiseChunk#fillSlice} and
 * writing its own density into every {@code NoiseChunk.NoiseInterpolator}'s cell-corner slices; vanilla then
 * interpolated those corner values per block. The 26.3 release removed that machinery: {@code NoiseChunk} no
 * longer has interpolators or slices, and interpolation is now the {@code minecraft:interpolated} density
 * function ({@link net.minecraft.world.level.levelgen.densityfunction.op.InterpolatedFunction}), which samples
 * its input on a cell-aligned volume and interpolates the result itself.
 * <p>
 * This function is that input. {@link TerrainGenerator#withIslandTerrain} swaps it in as the input of every
 * {@code interpolated} node of the End's final density, so the island density is again evaluated at exactly the
 * cell corners the old slices held and vanilla interpolates between them, as before.
 * <p>
 * It is only ever compiled, never serialised: it lives in a router built at runtime and is not registered as
 * a density function type.
 */
public record IslandTerrainDensityFunction(
        int cellSizeXz,
        int cellSizeY,
        int noiseMinY,
        int noiseHeight
) implements DensityFunction {
    public static final MapCodec<IslandTerrainDensityFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    Codec.INT.fieldOf("cell_size_xz").forGetter(IslandTerrainDensityFunction::cellSizeXz),
                    Codec.INT.fieldOf("cell_size_y").forGetter(IslandTerrainDensityFunction::cellSizeY),
                    Codec.INT.fieldOf("noise_min_y").forGetter(IslandTerrainDensityFunction::noiseMinY),
                    Codec.INT.fieldOf("noise_height").forGetter(IslandTerrainDensityFunction::noiseHeight)
            )
            .apply(instance, IslandTerrainDensityFunction::new));

    @Override
    public DensitySampler compileSampler(DensityFunction.CompileContext context) {
        return new Sampler(cellSizeXz, cellSizeY, noiseMinY, noiseHeight);
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        return this;
    }

    @Override
    public Interval range() {
        // The island density has no fixed bound (the island layers can exceed 1), so it must not be used to
        // let the compiler fold min/max nodes around it.
        return Interval.INFINITE;
    }

    @Override
    public @DensityFunction.Axes int domainAxes() {
        return DensityFunction.ALL_AXES;
    }

    @Override
    public MapCodec<IslandTerrainDensityFunction> codec() {
        return CODEC;
    }

    private record Sampler(int cellSizeXz, int cellSizeY, int noiseMinY, int noiseHeight) implements DensitySampler {
        private double[] newColumn() {
            return new double[Math.max(1, noiseHeight / cellSizeY + 1)];
        }

        private void fillColumn(double[] column, int blockX, int blockZ) {
            // fillTerrainDensity leaves the buffer untouched when the generator is not initialised; treat that
            // as void rather than reusing the previous column's values.
            Arrays.fill(column, -1.0);
            TerrainGenerator.fillTerrainDensity(
                    column,
                    Math.floorDiv(blockX, cellSizeXz) * cellSizeXz,
                    Math.floorDiv(blockZ, cellSizeXz) * cellSizeXz,
                    cellSizeXz,
                    cellSizeY,
                    noiseHeight
            );
        }

        private float valueAt(double[] column, int blockY) {
            final int index = Math.floorDiv(blockY - noiseMinY, cellSizeY);
            return (float) column[Math.max(0, Math.min(column.length - 1, index))];
        }

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer outputBuffer, DensityVolume volume) {
            final double[] column = newColumn();
            for (int z = 0; z < volume.sizeZ(); z++) {
                final int blockZ = volume.blockZ(z);
                for (int x = 0; x < volume.sizeX(); x++) {
                    fillColumn(column, volume.blockX(x), blockZ);
                    for (int y = 0; y < volume.sizeY(); y++) {
                        outputBuffer.set(volume.indexUnchecked(x, y, z), valueAt(column, volume.blockY(y)));
                    }
                }
            }
        }

        @Override
        public float sampleValue(SamplerContext context, int blockX, int blockY, int blockZ) {
            final double[] column = newColumn();
            fillColumn(column, blockX, blockZ);
            return valueAt(column, blockY);
        }
    }
}
