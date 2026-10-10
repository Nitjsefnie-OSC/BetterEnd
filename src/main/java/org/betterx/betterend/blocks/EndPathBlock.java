package org.betterx.betterend.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A path made from a BetterEnd terrain block. Vanilla {@link DirtPathBlock} hard-codes {@code minecraft:dirt} as
 * the block it turns into when placed under, or covered by, a solid block; this reverts to the terrain block the
 * path was made from instead (e.g. {@code end_mycelium_path} back to {@code end_mycelium}), the same behaviour
 * 26.3's {@code PathBlock} has.
 */
public class EndPathBlock extends DirtPathBlock {
    private final Block source;

    public EndPathBlock(BlockBehaviour.Properties properties, Block source) {
        super(properties);
        this.source = source;
    }

    private BlockState revertedState() {
        return source.defaultBlockState();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return !this.defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? Block.pushEntitiesUp(
                this.defaultBlockState(),
                revertedState(),
                context.getLevel(),
                context.getClickedPos()
        )
                : super.getStateForPlacement(context);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        final BlockState newState = Block.pushEntitiesUp(state, revertedState(), level, pos);
        level.setBlockAndUpdate(pos, newState);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(null, newState));
    }
}
