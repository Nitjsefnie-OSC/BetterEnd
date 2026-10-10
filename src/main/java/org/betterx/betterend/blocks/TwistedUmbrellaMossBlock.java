package org.betterx.betterend.blocks;


import org.betterx.betterend.registry.block.EndPlantBlocks;
import org.betterx.bclib.blocks.BaseDoublePlantBlock;
import org.betterx.betterend.util.GrowthPlacer;
import org.betterx.bclib.blocks.BasePlantBlock;
import org.betterx.betterend.registry.EndBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class TwistedUmbrellaMossBlock extends BasePlantBlock {
    public TwistedUmbrellaMossBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Environment(EnvType.CLIENT)
    public boolean hasEmissiveLighting(BlockGetter world, BlockPos pos) {
        return true;
    }

    @Environment(EnvType.CLIENT)
    public float getAmbientOcclusionLightLevel(BlockGetter world, BlockPos pos) {
        return 1F;
    }

    @Override
    public boolean isValidBonemealTarget(
            LevelReader world,
            BlockPos pos,
            BlockState state,
            BonemealSource bonemealSource
    ) {
        // Like vanilla's TallGrassBlock, the space above has to be free, or bone meal is used up for nothing.
        return world.isEmptyBlock(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(
            Level world,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            BonemealSource bonemealSource
    ) {
        return world.isEmptyBlock(pos.above());
    }

    @Override
    public void performBonemeal(
            ServerLevel world,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            BonemealSource bonemealSource
    ) {
        int rot = world.getRandom().nextInt(4);
        BlockState bs = EndPlantBlocks.TWISTED_UMBRELLA_MOSS_TALL.defaultBlockState()
                                                            .setValue(BaseDoublePlantBlock.ROTATION, rot);
        GrowthPlacer placer = new GrowthPlacer(world);
        placer.set(pos, bs);
        placer.set(pos.above(), bs.setValue(BaseDoublePlantBlock.TOP, true));
        placer.notifyNeighbours();
    }
}
