package org.betterx.betterend.testmod.gametest;

import org.betterx.bclib.blocks.FeatureSaplingBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * BetterEnd saplings take bone meal like vanilla saplings, and still grow on their own as slowly as before.
 * <p>
 * Every {@link FeatureSaplingBlock} used to roll the same 1 in 16 for a random tick and for a bone meal
 * application. A tree needs two successes (the {@code STAGE} step, then the tree), so a player spent about
 * 32 bone meal per tree against four or five for an oak. Bone meal now rolls vanilla's 45%, and random
 * ticks keep their 1 in 16 through a separate roll - the second half matters as much as the first, because
 * the shared roll means the obvious fix would have made every sapling grow seven times faster by itself.
 * <p>
 * All bounds are several standard deviations wide (stated next to each), so a failure is a changed rate,
 * not a bad run.
 */
public class SaplingBonemealGameTest {
    private static final String HUGE_ARENA = "betterend-testmod:huge_empty";
    private static final int SIZE = 32;
    private static final BlockPos SUPPORT = new BlockPos(SIZE / 2, 0, SIZE / 2);
    private static final BlockPos SOIL = SUPPORT.above();
    private static final BlockPos PLANT = SOIL.above();

    /** Trees grown per sapling species; 8 species make 40 trees. */
    private static final int TREES_PER_SAPLING = 5;
    /** Far beyond any plausible run at 45% (P(more than 40) is below 1e-9); only a stuck sapling gets here. */
    private static final int MAX_APPLICATIONS = 40;
    /**
     * Bone meal per tree, averaged over all trees. Expected 2 / 0.45 = 4.44 with a standard deviation of
     * 2.33 per tree, 0.37 for the mean of 40: the bounds are 3.9 and 5.6 sigma out. The old rate averaged 32.
     */
    private static final double MIN_MEAN_BONE_MEAL = 3.0;
    private static final double MAX_MEAN_BONE_MEAL = 6.5;

    /** Rolls per rate measurement. */
    private static final int SAMPLES = 4000;
    /** Expected 0.45, standard deviation 0.0079 at 4000 samples: 6.3 sigma either way. */
    private static final double MIN_BONEMEAL_RATE = 0.40;
    private static final double MAX_BONEMEAL_RATE = 0.50;
    /** Expected 1/16 = 0.0625, standard deviation 0.0038 at 4000 samples: 4.6 sigma below, 6 above. */
    private static final double MIN_RANDOM_TICK_RATE = 0.045;
    private static final double MAX_RANDOM_TICK_RATE = 0.085;

    @GameTest(structure = HUGE_ARENA, maxTicks = 200)
    public void boneMealPerTreeIsVanillaLike(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos plant = helper.absolutePos(PLANT);
        final List<String> failures = new ArrayList<>();
        final StringBuilder perSapling = new StringBuilder();
        int trees = 0;
        int boneMeal = 0;

        resetArena(helper, true);
        for (FeatureSaplingBlock sapling : betterEndSaplings()) {
            final Block soil = firstSoil(helper, sapling);
            if (soil == null) {
                failures.add(name(sapling) + " survives on no block at all");
                continue;
            }

            int used = 0;
            for (int tree = 0; tree < TREES_PER_SAPLING; tree++) {
                resetArena(helper, false);
                set(helper, SOIL, soil.defaultBlockState());
                set(helper, PLANT, sapling.defaultBlockState());

                int applications = 0;
                while (level.getBlockState(plant).is(sapling) && applications < MAX_APPLICATIONS) {
                    // The path a dispenser and a survival player's click both take.
                    BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL, 64), level, plant);
                    applications++;
                }
                if (level.getBlockState(plant).is(sapling)) {
                    failures.add(name(sapling) + " on " + name(soil) + " was still a sapling after "
                            + MAX_APPLICATIONS + " bone meal");
                }
                used += applications;
            }
            trees += TREES_PER_SAPLING;
            boneMeal += used;
            perSapling.append(String.format(Locale.ROOT, "%n   %s: %.1f", name(sapling), used / (double) TREES_PER_SAPLING));
        }
        resetArena(helper, false);

        if (trees == 0) failures.add("no BetterEnd FeatureSaplingBlock is registered - nothing was tested");
        final double mean = trees == 0 ? 0 : boneMeal / (double) trees;
        if (trees > 0 && (mean < MIN_MEAN_BONE_MEAL || mean > MAX_MEAN_BONE_MEAL)) {
            failures.add(String.format(
                    Locale.ROOT,
                    "%d trees took %.2f bone meal each on average, expected %.1f-%.1f (a vanilla sapling: 4.4)%s",
                    trees, mean, MIN_MEAN_BONE_MEAL, MAX_MEAN_BONE_MEAL, perSapling
            ));
        }

        failIfAny(helper, "Sapling bone meal rate regression", failures);
        helper.succeed();
    }

    @GameTest(maxTicks = 100)
    public void boneMealIsVanillaAndRandomTicksKeepTheirRate(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final RandomSource random = RandomSource.create(7L);
        final BlockPos rel = new BlockPos(1, 2, 1);
        final BlockPos abs = helper.absolutePos(rel);
        final List<String> failures = new ArrayList<>();

        for (FeatureSaplingBlock sapling : betterEndSaplings()) {
            final BlockState stage0 = sapling.defaultBlockState().setValue(SaplingBlock.STAGE, 0);
            level.setBlock(abs, stage0, Block.UPDATE_CLIENTS);

            int takes = 0;
            for (int i = 0; i < SAMPLES; i++) {
                if (sapling.isBonemealSuccess(level, random, abs, stage0, BonemealSource.INTERACTION)) takes++;
            }
            checkRate(failures, name(sapling) + " bone meal", takes, MIN_BONEMEAL_RATE, MAX_BONEMEAL_RATE);

            // A random tick on a STAGE 0 sapling either does nothing or moves it to STAGE 1, so counting
            // the stage changes over many ticks from STAGE 0 measures the natural growth roll directly.
            int advances = 0;
            for (int i = 0; i < SAMPLES; i++) {
                level.setBlock(abs, stage0, Block.UPDATE_CLIENTS);
                stage0.randomTick(level, abs, random);
                final BlockState after = level.getBlockState(abs);
                if (after.is(sapling) && after.getValue(SaplingBlock.STAGE) == 1) advances++;
            }
            checkRate(failures, name(sapling) + " random tick", advances, MIN_RANDOM_TICK_RATE, MAX_RANDOM_TICK_RATE);
        }
        level.setBlock(abs, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

        failIfAny(helper, "Sapling growth rate regression", failures);
        helper.succeed();
    }

    private static void checkRate(List<String> failures, String what, int hits, double min, double max) {
        final double rate = hits / (double) SAMPLES;
        if (rate < min || rate > max) {
            failures.add(String.format(Locale.ROOT, "%s succeeded %.4f of %d times, expected %.3f-%.3f",
                    what, rate, SAMPLES, min, max));
        }
    }

    private static List<FeatureSaplingBlock> betterEndSaplings() {
        final List<FeatureSaplingBlock> saplings = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof FeatureSaplingBlock sapling
                    && BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("betterend")) {
                saplings.add(sapling);
            }
        }
        return saplings;
    }

    /** The first registered block the sapling's own {@code canSurvive} accepts beneath it. */
    private static Block firstSoil(GameTestHelper helper, FeatureSaplingBlock sapling) {
        final ServerLevel level = helper.getLevel();
        final BlockPos plant = helper.absolutePos(PLANT);
        final BlockState saplingState = sapling.defaultBlockState();
        Block found = null;
        for (Block soil : BuiltInRegistries.BLOCK) {
            if (soil.defaultBlockState().isAir()) continue;
            set(helper, SOIL, soil.defaultBlockState());
            if (saplingState.canSurvive(level, plant)) {
                found = soil;
                break;
            }
        }
        set(helper, SOIL, Blocks.AIR.defaultBlockState());
        return found;
    }

    /**
     * Air everywhere on a stone floor (endstone dust is a falling block). After the first full pass only the
     * blocks a tree left behind need writing, which keeps 40 trees cheap.
     */
    private static void resetArena(GameTestHelper helper, boolean full) {
        final ServerLevel level = helper.getLevel();
        for (BlockPos rel : BlockPos.betweenClosed(0, 0, 0, SIZE - 1, SIZE - 1, SIZE - 1)) {
            final BlockState want = rel.getY() == SUPPORT.getY()
                    ? Blocks.STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            if (full || level.getBlockState(helper.absolutePos(rel)) != want) set(helper, rel, want);
        }
    }

    private static void set(GameTestHelper helper, BlockPos rel, BlockState state) {
        helper.getLevel().setBlock(helper.absolutePos(rel), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    private static String name(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static void failIfAny(GameTestHelper helper, String title, List<String> failures) {
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(title + ":\n - " + String.join("\n - ", failures)));
        }
    }
}
