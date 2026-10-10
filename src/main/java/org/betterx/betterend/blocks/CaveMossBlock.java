package org.betterx.betterend.blocks;

import org.betterx.bclib.blocks.BaseTerrainBlock;
import de.ambertation.wover.block.api.BlockProperties;
import de.ambertation.wover.block.api.BlockProperties.TripleShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Cave moss grows on cave floors, ceilings and on both sides of thin rock. Its {@link #SHAPE} tells the three
 * apart: {@link TripleShape#BOTTOM} is the moss on a floor (moss on top, end stone below - the default),
 * {@link TripleShape#TOP} the same block flipped upside down for ceilings, and {@link TripleShape#MIDDLE} moss
 * on every face. This restores the behaviour of the retired BCLib {@code TripleTerrainBlock}.
 */
public class CaveMossBlock extends BaseTerrainBlock {
    public static final EnumProperty<TripleShape> SHAPE = BlockProperties.TRIPLE_SHAPE;

    public CaveMossBlock(BlockBehaviour.Properties props, Block baseBlock) {
        super(props, baseBlock);
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, TripleShape.BOTTOM));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        final Direction dir = ctx.getClickedFace();
        final TripleShape shape = dir == Direction.UP
                ? TripleShape.BOTTOM
                : dir == Direction.DOWN ? TripleShape.TOP : TripleShape.MIDDLE;
        return defaultBlockState().setValue(SHAPE, shape);
    }

    @Override
    public InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (state.getValue(SHAPE) == TripleShape.BOTTOM) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        final TripleShape shape = state.getValue(SHAPE);
        if (shape == TripleShape.BOTTOM) {
            super.randomTick(state, world, pos, random);
        } else if (random.nextInt(16) == 0) {
            final boolean bottom = canStayBottom(world, pos);
            if (shape == TripleShape.TOP) {
                if (!bottom) {
                    world.setBlockAndUpdate(pos, getBaseBlock().defaultBlockState());
                }
            } else {
                final boolean top = canStay(state, world, pos) || isMiddle(world.getBlockState(pos.above()));
                if (!top && !bottom) {
                    world.setBlockAndUpdate(pos, getBaseBlock().defaultBlockState());
                } else if (top && !bottom) {
                    world.setBlockAndUpdate(pos, state.setValue(SHAPE, TripleShape.BOTTOM));
                } else if (!top) {
                    world.setBlockAndUpdate(pos, state.setValue(SHAPE, TripleShape.TOP));
                }
            }
        }
    }

    /** Whether the face below is open enough for moss growing downwards (the ceiling counterpart of {@link #canStay}). */
    protected boolean canStayBottom(LevelReader world, BlockPos pos) {
        final BlockPos blockPos = pos.below();
        final BlockState blockState = world.getBlockState(blockPos);
        if (isMiddle(blockState)) {
            return true;
        } else if (blockState.getFluidState().getAmount() == 8) {
            return false;
        } else {
            return !blockState.isFaceSturdy(world, blockPos, Direction.UP);
        }
    }

    protected boolean isMiddle(BlockState blockState) {
        return blockState.is(this) && blockState.getValue(SHAPE) == TripleShape.MIDDLE;
    }
}
