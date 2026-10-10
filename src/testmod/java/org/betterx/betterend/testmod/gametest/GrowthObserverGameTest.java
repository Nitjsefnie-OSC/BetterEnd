package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.blocks.EndBlockProperties;
import org.betterx.betterend.blocks.HydraluxSaplingBlock;
import org.betterx.betterend.registry.block.EndPlantBlocks;
import org.betterx.betterend.registry.block.EndSaplingBlocks;
import org.betterx.betterend.registry.block.EndStoneBlocks;
import org.betterx.betterend.registry.block.EndTerrainBlocks;
import org.betterx.betterend.registry.block.EndWaterPlantBlocks;

import de.ambertation.wover.sets.api.blocks.SlotType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * #456: a plant growing in a live level (random tick or bonemeal) must notify its neighbours like any other
 * block change, so an observer watching it fires. Growth builds the plant with {@code setWithoutUpdate} (the
 * worldgen helper) and used to change the observed block silently.
 */
public class GrowthObserverGameTest {
    private static final BlockPos PLANT = new BlockPos(3, 1, 3);
    // Looks west, at the plant.
    private static final BlockPos OBSERVER = PLANT.east();
    private static final int ATTEMPTS = 500;

    @GameTest
    public void hydraluxSaplingGrowthTriggersObserver(GameTestHelper helper) {
        helper.setBlock(PLANT.below(), EndStoneBlocks.SULPHURIC_ROCK.getBlock(SlotType.SOURCE));
        // The arena is 8 blocks tall; a hydralux needs 4-8 blocks of water, so taller rolls just do not grow
        // and the loop below retries.
        for (int y = PLANT.getY(); y < 8; y++) helper.setBlock(PLANT.atY(y), Blocks.WATER);
        final Block sapling = EndSaplingBlocks.HYDRALUX_SAPLING;
        helper.setBlock(PLANT, sapling.defaultBlockState().setValue(HydraluxSaplingBlock.AGE, 3));
        placeObserver(helper);

        final ServerLevel level = helper.getLevel();
        final BlockPos abs = helper.absolutePos(PLANT);
        for (int i = 0; i < ATTEMPTS && helper.getBlockState(PLANT).is(sapling); i++) {
            ((HydraluxSaplingBlock) sapling).performBonemeal(level, level.getRandom(), abs, level.getBlockState(abs), BonemealSource.INTERACTION);
        }
        if (!helper.getBlockState(PLANT).is(EndWaterPlantBlocks.HYDRALUX)) {
            throw helper.assertionException(Component.literal(
                    "hydralux sapling did not grow within " + ATTEMPTS + " bonemeal calls, left "
                            + helper.getBlockState(PLANT)));
        }
        assertObserverFires(helper, "hydralux sapling growing into a hydralux", s ->
                s.is(EndWaterPlantBlocks.HYDRALUX)
                        && s.getValue(EndBlockProperties.HYDRALUX_SHAPE) == EndBlockProperties.HydraluxShape.ROOTS);
    }

    @GameTest
    public void umbrellaMossBonemealTriggersObserver(GameTestHelper helper) {
        helper.setBlock(PLANT.below(), EndTerrainBlocks.JUNGLE_MOSS);
        helper.setBlock(PLANT, EndPlantBlocks.UMBRELLA_MOSS);
        placeObserver(helper);

        final ServerLevel level = helper.getLevel();
        final BlockPos abs = helper.absolutePos(PLANT);
        final BlockState state = level.getBlockState(abs);
        ((BonemealableBlock) state.getBlock())
                .performBonemeal(level, level.getRandom(), abs, state, BonemealSource.INTERACTION);
        assertObserverFires(helper, "umbrella moss growing tall", s -> s.is(EndPlantBlocks.UMBRELLA_MOSS_TALL));
    }

    private static void placeObserver(GameTestHelper helper) {
        helper.setBlock(OBSERVER, Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.WEST));
        if (helper.getBlockState(OBSERVER).getValue(ObserverBlock.POWERED)
                || helper.getLevel().getBlockTicks().hasScheduledTick(helper.absolutePos(OBSERVER), Blocks.OBSERVER)) {
            throw helper.assertionException(Component.literal("observer was already triggered by the test setup"));
        }
    }

    private static void assertObserverFires(
            GameTestHelper helper,
            String what,
            Predicate<BlockState> plantStillThere
    ) {
        if (!helper.getLevel().getBlockTicks().hasScheduledTick(helper.absolutePos(OBSERVER), Blocks.OBSERVER)) {
            throw helper.assertionException(Component.literal(
                    "an observer watching a " + what + " was not triggered (growth sent no shape update)"));
        }
        // The plant must also survive the neighbour updates it now sends to itself.
        helper.succeedWhen(() -> {
            if (!helper.getBlockState(OBSERVER).getValue(ObserverBlock.POWERED)) {
                throw helper.assertionException(Component.literal("observer did not pulse"));
            }
            if (!plantStillThere.test(helper.getBlockState(PLANT))) {
                throw helper.assertionException(Component.literal(
                        "the grown plant broke after notifying its neighbours: " + helper.getBlockState(PLANT)));
            }
        });
    }
}
