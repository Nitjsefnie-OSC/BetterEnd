package org.betterx.betterend.world.features;


import org.betterx.betterend.registry.block.EndMushroomBlocks;
import org.betterx.bclib.blocks.BasePlantWithAgeBlock;
import org.betterx.betterend.registry.EndBlocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

public class GlowPillarFeature extends ScatterFeature<ScatterFeatureConfig> {
    public static final MapCodec<GlowPillarFeature> CODEC = ScatterFeatureConfig.CODEC.xmap(GlowPillarFeature::new, f -> f.config);

    public GlowPillarFeature(ScatterFeatureConfig config) {
        super(config);
    }

    @Override
    public MapCodec<GlowPillarFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean canGenerate(
            ScatterFeatureConfig cfg,
            WorldGenLevel world,
            RandomSource random,
            BlockPos center,
            BlockPos blockPos,
            float radius
    ) {
        return EndMushroomBlocks.GLOWING_PILLAR_SEED.defaultBlockState().canSurvive(world, blockPos);
    }

    @Override
    public void generate(
            ScatterFeatureConfig cfg, WorldGenLevel world, RandomSource random, BlockPos blockPos
    ) {
        BasePlantWithAgeBlock seed = ((BasePlantWithAgeBlock) EndMushroomBlocks.GLOWING_PILLAR_SEED);
        seed.growAdult(world, random, blockPos);
    }

    @Override
    protected int getChance() {
        return 10;
    }
}
