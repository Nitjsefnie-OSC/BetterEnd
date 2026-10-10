package org.betterx.betterend.blocks;


import org.betterx.betterend.registry.block.EndWaterPlantBlocks;
import de.ambertation.wover.block.api.BlockProperties.TripleShape;
import org.betterx.bclib.blocks.UnderwaterPlantWithAgeBlock;
import org.betterx.betterend.util.GrowthPlacer;
import org.betterx.betterend.registry.EndBlocks;
import de.ambertation.wover.tag.api.predefined.CommonBlockTags;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class EndLilySeedBlock extends UnderwaterPlantWithAgeBlock {
    public EndLilySeedBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void grow(WorldGenLevel world, RandomSource random, BlockPos pos) {
        if (canGrow(world, pos)) {
            GrowthPlacer placer = new GrowthPlacer(world);
            placer.set(
                    pos,
                    EndWaterPlantBlocks.END_LILY.defaultBlockState().setValue(EndLilyBlock.SHAPE, TripleShape.BOTTOM)
            );
            BlockPos up = pos.above();
            while (world.getFluidState(up).isSource()) {
                placer.set(
                        up,
                        EndWaterPlantBlocks.END_LILY.defaultBlockState().setValue(EndLilyBlock.SHAPE, TripleShape.MIDDLE)
                );
                up = up.above();
            }
            placer.set(
                    up,
                    EndWaterPlantBlocks.END_LILY.defaultBlockState().setValue(EndLilyBlock.SHAPE, TripleShape.TOP)
            );
            placer.notifyNeighbours();
        }
    }

    private boolean canGrow(WorldGenLevel world, BlockPos pos) {
        BlockPos up = pos.above();
        while (world.getBlockState(up).getFluidState().getType().equals(Fluids.WATER.getSource())) {
            up = up.above();
        }
        return world.isEmptyBlock(up);
    }

    @Override
    protected boolean isTerrain(BlockState state) {
        return state.is(CommonBlockTags.END_STONES);
    }
}
