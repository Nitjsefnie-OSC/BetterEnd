package org.betterx.betterend.world.features;


import org.betterx.betterend.registry.block.EndPlantBlocks;
import org.betterx.betterend.registry.block.EndTerrainBlocks;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.betterend.blocks.NeonCactusPlantBlock;
import org.betterx.betterend.registry.EndBlocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class NeonCactusFeature extends DefaultFeature {
    public static final MapCodec<NeonCactusFeature> CODEC = MapCodec.unit(NeonCactusFeature::new);

    @Override
    public MapCodec<NeonCactusFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel world,
            ChunkGenerator generator,
            RandomSource random,
            BlockPos pos
    ) {
        BlockState ground = world.getBlockState(pos.below());
        if (!ground.is(EndTerrainBlocks.ENDSTONE_DUST) && !ground.is(EndTerrainBlocks.END_MOSS)) {
            return false;
        }

        NeonCactusPlantBlock cactus = ((NeonCactusPlantBlock) EndPlantBlocks.NEON_CACTUS);
        cactus.growPlant(world, pos, random);

        return true;
    }
}
