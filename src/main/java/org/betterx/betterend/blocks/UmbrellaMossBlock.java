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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class UmbrellaMossBlock extends BasePlantBlock {
    public UmbrellaMossBlock(BlockBehaviour.Properties props) {
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
    public boolean isValidBonemealTarget(LevelReader world, BlockPos pos, BlockState state) {
        // Umbrella moss survives on any soil, the tall moss it grows into only on jungle moss or mycelium -
        // growing it anywhere else made the new plant break immediately, so bone meal deleted the moss.
        // Like vanilla's TallGrassBlock, the space above has to be free too, or bone meal is used up for nothing.
        return EndPlantBlocks.UMBRELLA_MOSS_TALL.defaultBlockState().canSurvive(world, pos)
                && world.isEmptyBlock(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state) {
        return world.isEmptyBlock(pos.above());
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        int rot = world.random.nextInt(4);
        BlockState bs = EndPlantBlocks.UMBRELLA_MOSS_TALL.defaultBlockState().setValue(BaseDoublePlantBlock.ROTATION, rot);
        GrowthPlacer placer = new GrowthPlacer(world);
        placer.set(pos, bs);
        placer.set(pos.above(), bs.setValue(BaseDoublePlantBlock.TOP, true));
        placer.notifyNeighbours();
    }
}
