package org.betterx.betterend.blocks;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PathBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A shovel-flattened path over one of the End terrain blocks.
 * <p>
 * 26.3 renamed {@code DirtPathBlock} to {@link PathBlock} and, more importantly, changed its shape: the block
 * it reverts to is no longer hard-coded to {@code minecraft:dirt} but passed to the constructor, so
 * {@link PathBlock#turnToBaseBlock} and the "lost its support" tick both restore that block instead. The old
 * {@code DirtPathBlock(Properties)} constructor - which is what every path block here used to be built with -
 * therefore has no direct equivalent.
 * <p>
 * That constructor is {@code protected}, so it can only be reached through a subclass; this class exists purely
 * to supply it. Every path block passes its own terrain parent (the same block that
 * {@code PathBlockTrait.withSource(...)} already names as its silk-touch drop), which is also the behaviour
 * change worth noting: trampling {@code end_mycelium_path} now yields {@code end_mycelium} rather than
 * {@code minecraft:dirt}.
 * <p>
 * {@link PathBlock#getStateForPlacement} still hard-codes {@code minecraft:dirt} for a path placed directly
 * under a block (#605), so that case is overridden here to revert to the same block as the tick does.
 */
public class EndPathBlock extends PathBlock {
    private final Block source;

    public EndPathBlock(Block source, BlockBehaviour.Properties properties) {
        super(source, properties);
        this.source = source;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return !this.defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? Block.pushEntitiesUp(
                this.defaultBlockState(),
                source.defaultBlockState(),
                context.getLevel(),
                context.getClickedPos()
        )
                : super.getStateForPlacement(context);
    }
}
