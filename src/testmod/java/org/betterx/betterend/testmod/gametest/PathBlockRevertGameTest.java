package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.block.EndTerrainBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression test for BetterEnd #605: on 26.2 BetterEnd path blocks were vanilla {@code DirtPathBlock}s, which
 * hard-code {@code minecraft:dirt} as the block they revert to once something solid covers them (scheduled tick)
 * or when they are placed under a solid block ({@code getStateForPlacement}).
 * <p>
 * 26.3's {@code PathBlock} takes the block it reverts to as a constructor argument and {@code EndPathBlock} passes
 * the path's own terrain block, so here a covered path reverts to that terrain (which, being covered, later decays
 * to end stone itself). Its {@code getStateForPlacement} still hard-codes dirt, which {@code EndPathBlock}
 * overrides. Terrain or end stone are both fine; dirt is not.
 */
public class PathBlockRevertGameTest {
    private static final BlockPos PATH = new BlockPos(1, 1, 1);

    private static Map<Block, Block> terrainOf() {
        return Map.ofEntries(
                Map.entry(EndTerrainBlocks.END_MYCELIUM_PATH, EndTerrainBlocks.END_MYCELIUM),
                Map.entry(EndTerrainBlocks.END_MOSS_PATH, EndTerrainBlocks.END_MOSS),
                Map.entry(EndTerrainBlocks.CHORUS_NYLIUM_PATH, EndTerrainBlocks.CHORUS_NYLIUM),
                Map.entry(EndTerrainBlocks.CAVE_MOSS_PATH, EndTerrainBlocks.CAVE_MOSS),
                Map.entry(EndTerrainBlocks.CRYSTAL_MOSS_PATH, EndTerrainBlocks.CRYSTAL_MOSS),
                Map.entry(EndTerrainBlocks.SHADOW_GRASS_PATH, EndTerrainBlocks.SHADOW_GRASS),
                Map.entry(EndTerrainBlocks.PINK_MOSS_PATH, EndTerrainBlocks.PINK_MOSS),
                Map.entry(EndTerrainBlocks.AMBER_MOSS_PATH, EndTerrainBlocks.AMBER_MOSS),
                Map.entry(EndTerrainBlocks.JUNGLE_MOSS_PATH, EndTerrainBlocks.JUNGLE_MOSS),
                Map.entry(EndTerrainBlocks.SANGNUM_PATH, EndTerrainBlocks.SANGNUM),
                Map.entry(EndTerrainBlocks.RUTISCUS_PATH, EndTerrainBlocks.RUTISCUS)
        );
    }

    private static List<Block> paths() {
        return List.copyOf(terrainOf().keySet());
    }

    private static boolean isEndRevert(Block path, BlockState state) {
        return state != null && (state.is(Blocks.END_STONE) || state.is(terrainOf().get(path)));
    }

    /** Covering an existing path with a solid block schedules a tick that must not turn it into dirt. */
    @GameTest
    public void coveredPathRevertsToEndTerrain(GameTestHelper helper) {
        final List<Block> paths = paths();
        for (int i = 0; i < paths.size(); i++) {
            final BlockPos pos = PATH.east(i % 4).south(i / 4);
            helper.setBlock(pos.below(), Blocks.END_STONE);
            helper.setBlock(pos, paths.get(i));
            helper.setBlock(pos.above(), Blocks.END_STONE);
        }
        helper.succeedWhen(() -> {
            for (int i = 0; i < paths.size(); i++) {
                final BlockPos pos = PATH.east(i % 4).south(i / 4);
                final BlockState state = helper.getBlockState(pos);
                if (!isEndRevert(paths.get(i), state)) {
                    throw helper.assertionException(Component.literal(
                            "covered " + paths.get(i) + " became " + state + ", expected its terrain or end stone"));
                }
            }
        });
    }

    /** Placing a path directly under a solid block must yield End terrain, not dirt. */
    @GameTest
    public void pathPlacedUnderBlockBecomesEndTerrain(GameTestHelper helper) {
        helper.setBlock(PATH.below(), Blocks.END_STONE);
        helper.setBlock(PATH, Blocks.AIR);
        helper.setBlock(PATH.above(), Blocks.END_STONE);
        final BlockPos absBelow = helper.absolutePos(PATH.below());
        for (Block path : paths()) {
            final BlockPlaceContext ctx = new BlockPlaceContext(
                    helper.getLevel(),
                    null,
                    InteractionHand.MAIN_HAND,
                    new ItemStack(path),
                    new BlockHitResult(Vec3.atCenterOf(absBelow), Direction.UP, absBelow, false)
            );
            final BlockState placed = path.getStateForPlacement(ctx);
            if (!isEndRevert(path, placed)) {
                throw helper.assertionException(Component.literal(
                        path + " placed under a block became " + placed + ", expected its terrain or end stone"));
            }
        }
        helper.succeed();
    }
}
