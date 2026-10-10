package org.betterx.betterend.util;

import org.betterx.bclib.util.BlocksHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Places the blocks of a multi-block plant without updates (like {@link BlocksHelper#setWithoutUpdate}), so
 * half-built plants never see their own neighbour checks fail. In a live {@link Level} (sapling/seed growth,
 * bonemeal) {@link #notifyNeighbours()} then sends the neighbour and shape updates a normal block change would
 * have caused - observers and BUDs only react to those. During worldgen (a {@code WorldGenRegion}) nothing is
 * recorded and nothing is notified.
 */
public final class GrowthPlacer {
    private final LevelAccessor world;
    private final List<BlockPos> placed;

    public GrowthPlacer(LevelAccessor world) {
        this.world = world;
        this.placed = world instanceof Level ? new ArrayList<>() : null;
    }

    public void set(BlockPos pos, BlockState state) {
        BlocksHelper.setWithoutUpdate(world, pos, state);
        if (placed != null) placed.add(pos.immutable());
    }

    public void notifyNeighbours() {
        if (placed == null || !(world instanceof Level level)) return;
        for (BlockPos pos : placed) {
            BlockState state = level.getBlockState(pos);
            level.updateNeighborsAt(pos, state.getBlock());
            state.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
        }
        placed.clear();
    }
}
