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

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression test for BetterEnd #605: BetterEnd path blocks are vanilla {@code DirtPathBlock}s, which hard-code
 * {@code minecraft:dirt} as the block they revert to once something solid covers them (scheduled tick) or when
 * they are placed under a solid block ({@code getStateForPlacement}). They must revert to the terrain block they
 * were made from instead.
 */
public class PathBlockRevertGameTest {
    private static final BlockPos PATH = new BlockPos(1, 1, 1);

    private static List<Block> paths() {
        return List.of(
                EndTerrainBlocks.END_MYCELIUM_PATH,
                EndTerrainBlocks.END_MOSS_PATH,
                EndTerrainBlocks.CHORUS_NYLIUM_PATH,
                EndTerrainBlocks.CAVE_MOSS_PATH,
                EndTerrainBlocks.CRYSTAL_MOSS_PATH,
                EndTerrainBlocks.SHADOW_GRASS_PATH,
                EndTerrainBlocks.PINK_MOSS_PATH,
                EndTerrainBlocks.AMBER_MOSS_PATH,
                EndTerrainBlocks.JUNGLE_MOSS_PATH,
                EndTerrainBlocks.SANGNUM_PATH,
                EndTerrainBlocks.RUTISCUS_PATH
        );
    }

    /** The terrain block each entry of {@link #paths()} was made from, in the same order. */
    private static List<Block> sources() {
        return List.of(
                EndTerrainBlocks.END_MYCELIUM,
                EndTerrainBlocks.END_MOSS,
                EndTerrainBlocks.CHORUS_NYLIUM,
                EndTerrainBlocks.CAVE_MOSS,
                EndTerrainBlocks.CRYSTAL_MOSS,
                EndTerrainBlocks.SHADOW_GRASS,
                EndTerrainBlocks.PINK_MOSS,
                EndTerrainBlocks.AMBER_MOSS,
                EndTerrainBlocks.JUNGLE_MOSS,
                EndTerrainBlocks.SANGNUM,
                EndTerrainBlocks.RUTISCUS
        );
    }

    /** Covering an existing path with a solid block schedules a tick that must turn it back into its terrain. */
    @GameTest
    public void coveredPathRevertsToItsTerrain(GameTestHelper helper) {
        final List<Block> paths = paths();
        final List<Block> sources = sources();
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
                if (!state.is(sources.get(i))) {
                    throw helper.assertionException(Component.literal(
                            "covered " + paths.get(i) + " became " + state + ", expected " + sources.get(i)));
                }
            }
        });
    }

    /** Placing a path directly under a solid block must yield its terrain block, not dirt. */
    @GameTest
    public void pathPlacedUnderBlockBecomesItsTerrain(GameTestHelper helper) {
        helper.setBlock(PATH.below(), Blocks.END_STONE);
        helper.setBlock(PATH, Blocks.AIR);
        helper.setBlock(PATH.above(), Blocks.END_STONE);
        final BlockPos absBelow = helper.absolutePos(PATH.below());
        final List<Block> paths = paths();
        final List<Block> sources = sources();
        for (int i = 0; i < paths.size(); i++) {
            final Block path = paths.get(i);
            final BlockPlaceContext ctx = new BlockPlaceContext(
                    helper.getLevel(),
                    null,
                    InteractionHand.MAIN_HAND,
                    new ItemStack(path),
                    new BlockHitResult(Vec3.atCenterOf(absBelow), Direction.UP, absBelow, false)
            );
            final BlockState placed = path.getStateForPlacement(ctx);
            if (placed == null || !placed.is(sources.get(i))) {
                throw helper.assertionException(Component.literal(
                        path + " placed under a block became " + placed + ", expected " + sources.get(i)));
            }
        }
        helper.succeed();
    }
}
