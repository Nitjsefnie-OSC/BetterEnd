package org.betterx.betterend.world.features;


import org.betterx.betterend.registry.block.EndSaplingBlocks;
import org.betterx.betterend.blocks.HydraluxSaplingBlock;
import org.betterx.betterend.registry.EndBlocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

public class HydraluxFeature extends UnderwaterPlantScatter<ScatterFeatureConfig> {
    public static final MapCodec<HydraluxFeature> CODEC = ScatterFeatureConfig.CODEC.xmap(HydraluxFeature::new, f -> f.config);

    public HydraluxFeature(ScatterFeatureConfig config) {
        super(config);
    }

    @Override
    public MapCodec<HydraluxFeature> codec() {
        return CODEC;
    }

    @Override
    public void generate(ScatterFeatureConfig cfg, WorldGenLevel world, RandomSource random, BlockPos blockPos) {
        HydraluxSaplingBlock seed = (HydraluxSaplingBlock) EndSaplingBlocks.HYDRALUX_SAPLING;
        seed.grow(world, random, blockPos);
    }

    @Override
    protected int getChance() {
        return 15;
    }
}
