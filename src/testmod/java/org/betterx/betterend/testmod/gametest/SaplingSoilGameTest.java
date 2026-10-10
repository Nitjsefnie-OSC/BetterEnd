package org.betterx.betterend.testmod.gametest;

import org.betterx.bclib.blocks.FeatureSaplingBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Every BetterEnd sapling grows on every block it can be planted on.
 * <p>
 * A sapling's soil and its tree's soil are two separate checks - the sapling's survival tag, and whatever
 * the tree feature tests below its origin for worldgen - and nothing kept them in step. Where they
 * disagreed the sapling planted, survived, took bone meal and random ticks, and never grew: a lacugrove
 * sapling on endstone dust (the feature wanted {@code #minecraft:nylium}), and a mossy glowshroom sapling
 * on pallidium or sangnum (its survival tag gained the flower islet coats, the feature still wanted end
 * moss or end mycelium). Nothing about that is visible to a player except a sapling that never grows.
 * <p>
 * The soils are not listed here: every registered block is tried under each sapling and whatever the
 * sapling's own {@code canSurvive} accepts is a soil it must grow on, so a soil added to a survival tag
 * later is covered without touching this test. Trees grow through {@link FeatureSaplingBlock#growFeatureNow}
 * - the sapling path, so the success roll and the {@code STAGE} step are not in the way - in a 32-block
 * arena: lacugroves and mossy glowshrooms reach 25 blocks and more, which the default arena clips.
 */
public class SaplingSoilGameTest {
    private static final String HUGE_ARENA = "betterend-testmod:huge_empty";
    private static final int SIZE = 32;
    private static final BlockPos SUPPORT = new BlockPos(SIZE / 2, 0, SIZE / 2);
    private static final BlockPos SOIL = SUPPORT.above();
    private static final BlockPos PLANT = SOIL.above();
    /**
     * Growth attempts per soil. In an empty arena the trees have all the room they need, so a working soil
     * grows on the first try; the margin only absorbs a feature that rolls its own room check.
     */
    private static final int ATTEMPTS = 5;

    @GameTest(structure = HUGE_ARENA, maxTicks = 100)
    public void everySaplingGrowsOnEverySoilItCanBePlantedOn(GameTestHelper helper) {
        final RandomSource random = RandomSource.create(5L);
        final List<String> failures = new ArrayList<>();
        int saplings = 0;

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof FeatureSaplingBlock sapling)) continue;
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("betterend")) continue;
            saplings++;

            final List<Block> soils = soilsOf(helper, sapling);
            if (soils.isEmpty()) {
                failures.add(name(sapling) + " survives on no block at all - nothing to grow it on");
                continue;
            }

            for (Block soil : soils) {
                if (!grows(helper, sapling, soil, random)) {
                    failures.add(name(sapling) + " can be planted on " + name(soil) + " but never grows there ("
                            + ATTEMPTS + " attempts in an empty " + SIZE + "-block arena)");
                }
            }
        }

        if (saplings == 0) failures.add("no BetterEnd FeatureSaplingBlock is registered - nothing was tested");
        clearArena(helper);

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Sapling soil regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    /** Every block the sapling's own {@code canSurvive} accepts beneath it. */
    private static List<Block> soilsOf(GameTestHelper helper, FeatureSaplingBlock sapling) {
        final ServerLevel level = helper.getLevel();
        final BlockPos plant = helper.absolutePos(PLANT);
        final BlockState saplingState = sapling.defaultBlockState();
        final List<Block> soils = new ArrayList<>();

        clearArena(helper);
        for (Block soil : BuiltInRegistries.BLOCK) {
            if (soil.defaultBlockState().isAir()) continue;
            set(helper, SOIL, soil.defaultBlockState());
            if (saplingState.canSurvive(level, plant)) soils.add(soil);
        }
        return soils;
    }

    private static boolean grows(GameTestHelper helper, FeatureSaplingBlock sapling, Block soil, RandomSource random) {
        final ServerLevel level = helper.getLevel();
        final BlockPos plant = helper.absolutePos(PLANT);

        clearArena(helper);
        set(helper, SOIL, soil.defaultBlockState());
        set(helper, PLANT, sapling.defaultBlockState());
        for (int i = 0; i < ATTEMPTS; i++) {
            final BlockState state = level.getBlockState(plant);
            if (!state.is(sapling)) return false;
            if (sapling.growFeatureNow(level, plant, state, random)) return !level.getBlockState(plant).is(sapling);
        }
        return false;
    }

    /** Air everywhere, on a stone floor - endstone dust is a falling block and needs something under it. */
    private static void clearArena(GameTestHelper helper) {
        for (BlockPos rel : BlockPos.betweenClosed(0, 0, 0, SIZE - 1, SIZE - 1, SIZE - 1)) {
            set(helper, rel, rel.getY() == SUPPORT.getY()
                    ? Blocks.STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState());
        }
    }

    private static void set(GameTestHelper helper, BlockPos rel, BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(rel), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}
