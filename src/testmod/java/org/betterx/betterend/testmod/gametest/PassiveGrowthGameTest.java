package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.blocks.BrimstoneBlock;
import org.betterx.betterend.blocks.EndPathBlock;
import org.betterx.betterend.blocks.EndPortalBlock;
import org.betterx.betterend.blocks.SulphurCrystalBlock;
import org.betterx.betterend.blocks.UmbrellaTreeClusterBlock;
import org.betterx.betterend.blocks.UmbrellaTreeClusterEmptyBlock;
import org.betterx.betterend.blocks.basis.PottableCropBlock;
import org.betterx.betterend.registry.block.EndCropBlocks;
import org.betterx.betterend.registry.block.EndStoneBlocks;
import org.betterx.betterend.registry.block.EndTerrainBlocks;
import org.betterx.betterend.registry.block.EndWoodBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.betterx.bclib.blocks.BasePlantWithAgeBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Passive (random-tick driven) growth for BetterEnd blocks other than saplings - BetterEnd #444/#578.
 * <p>
 * Since Mojang split {@code tick()} (scheduled ticks only) from {@code randomTick()}, growth logic in
 * either the wrong method or on a block that does not random-tick is silently dead while bonemeal still
 * works. Every test here therefore goes through the same gate the game's own chunk ticker uses -
 * {@link BlockState#isRandomlyTicking()} - before calling {@link BlockState#randomTick}, rather than
 * calling the block's method directly, so both halves of that contract are covered.
 */
public class PassiveGrowthGameTest {
    private static final BlockPos POS = new BlockPos(3, 3, 3);
    // Every growth roll checked here is 1/16 or better per call; 1000 attempts leaves a false-failure
    // chance far below one in a billion even for the 1/96 sulphur-crystal aging roll at 2000.
    private static final int ATTEMPTS = 1000;

    /**
     * Calls randomTick at {@code tickPos} until {@code done} holds, failing if the block there is not
     * random-ticking at all.
     */
    private static boolean randomTickUntil(
            GameTestHelper helper,
            BlockPos tickPos,
            int attempts,
            Predicate<GameTestHelper> done
    ) {
        final ServerLevel level = helper.getLevel();
        final BlockPos abs = helper.absolutePos(tickPos);
        final RandomSource random = level.getRandom();
        for (int i = 0; i < attempts; i++) {
            final BlockState state = level.getBlockState(abs);
            if (!state.isRandomlyTicking()) {
                throw helper.assertionException(Component.literal(
                        state + " is not random-ticking, so the game never calls its randomTick()"
                ));
            }
            state.randomTick(level, abs, random);
            if (done.test(helper)) return true;
        }
        return false;
    }

    private static Class<?> declaringClass(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                c.getDeclaredMethod(name, BlockState.class, ServerLevel.class, BlockPos.class, RandomSource.class);
                return c;
            } catch (NoSuchMethodException ignored) {
            } catch (RuntimeException | LinkageError e) {
                // Some block classes declare methods whose signatures mention client-only types, and
                // reflecting over them on the dedicated server refuses to load those - such a class
                // can't be inspected here, so treat it as "no override" for this level.
            }
        }
        return null;
    }

    /**
     * Classes the sweep flags on purpose-built code: {@link EndPathBlock#tick} is the scheduled
     * revert-to-terrain tick (its random ticking is merely inherited from the copied terrain
     * properties and does nothing), and {@link EndPortalBlock#randomTick} is an intentionally empty
     * override that suppresses vanilla's portal mob spawning.
     */
    private static final Set<Class<?>> NOT_GROWTH = Set.of(EndPathBlock.class, EndPortalBlock.class);

    private static boolean isOurs(Class<?> c) {
        return c != null && c.getName().startsWith("org.betterx.");
    }

    /**
     * Sweeps every BetterEnd block: a randomTick() override on a block that never random-ticks is dead
     * code, and a random-ticking block whose only override is tick() (which random ticks no longer
     * reach) almost certainly has its passive behaviour in the wrong method.
     */
    @GameTest
    public void randomTickLogicIsReachable(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            final Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!BetterEnd.MOD_ID.equals(id.getNamespace())) continue;
            if (NOT_GROWTH.stream().anyMatch(c -> c.isInstance(block))) continue;

            final boolean ticks = block.getStateDefinition()
                                       .getPossibleStates()
                                       .stream()
                                       .anyMatch(BlockState::isRandomlyTicking);
            final Class<?> randomTick = declaringClass(block.getClass(), "randomTick");
            final Class<?> tick = declaringClass(block.getClass(), "tick");

            if (isOurs(randomTick) && !ticks) {
                failures.add(id + " overrides randomTick() in " + randomTick.getSimpleName()
                        + " but no state is random-ticking");
            }
            if (ticks && isOurs(tick) && !isOurs(randomTick)) {
                failures.add(id + " random-ticks but only overrides tick() (in " + tick.getSimpleName()
                        + "), which random ticks never reach");
            }
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Unreachable random-tick logic:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void naturalEmptyUmbrellaClusterRefills(GameTestHelper helper) {
        helper.setBlock(
                POS,
                EndWoodBlocks.UMBRELLA_TREE_CLUSTER_EMPTY.defaultBlockState()
                                                         .setValue(UmbrellaTreeClusterEmptyBlock.NATURAL, true)
        );
        if (!randomTickUntil(helper, POS, ATTEMPTS, h -> h.getBlockState(POS).is(EndWoodBlocks.UMBRELLA_TREE_CLUSTER))) {
            throw helper.assertionException(Component.literal(
                    "a natural empty umbrella tree cluster did not refill within " + ATTEMPTS + " random ticks"
            ));
        }
        if (!helper.getBlockState(POS).getValue(UmbrellaTreeClusterBlock.NATURAL)) {
            throw helper.assertionException(Component.literal("a refilled natural cluster lost its NATURAL flag"));
        }
        helper.succeed();
    }

    @GameTest
    public void amberRootGrows(GameTestHelper helper) {
        helper.setBlock(POS.below(), EndTerrainBlocks.AMBER_MOSS);
        helper.setBlock(POS, EndCropBlocks.AMBER_ROOT);
        if (!randomTickUntil(helper, POS, ATTEMPTS, h -> h.getBlockState(POS).getValue(PottableCropBlock.AGE) > 0)) {
            throw helper.assertionException(Component.literal(
                    "amber root did not advance its age within " + ATTEMPTS + " random ticks"
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void cavePumpkinVineGrows(GameTestHelper helper) {
        helper.setBlock(POS.above(), Blocks.END_STONE);
        helper.setBlock(POS, EndCropBlocks.CAVE_PUMPKIN_SEED);
        if (!randomTickUntil(helper, POS, ATTEMPTS, h -> h.getBlockState(POS).getValue(BasePlantWithAgeBlock.AGE) > 0)) {
            throw helper.assertionException(Component.literal(
                    "cave pumpkin vine did not advance its age within " + ATTEMPTS + " random ticks"
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void brimstoneInWaterGrowsAndAgesSulphurCrystals(GameTestHelper helper) {
        helper.setBlock(POS, EndStoneBlocks.BRIMSTONE);
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            helper.setBlock(POS.relative(dir), Blocks.WATER);
        }
        final BlockPos crystalPos = POS.above();
        helper.setBlock(
                crystalPos,
                EndStoneBlocks.SULPHUR_CRYSTAL.defaultBlockState()
                                              .setValue(SulphurCrystalBlock.FACING, Direction.UP)
                                              .setValue(SulphurCrystalBlock.WATERLOGGED, true)
                                              .setValue(SulphurCrystalBlock.AGE, 0)
        );

        if (!randomTickUntil(helper, POS, ATTEMPTS, h -> h.getBlockState(POS).getValue(BrimstoneBlock.ACTIVATED))) {
            throw helper.assertionException(Component.literal("brimstone next to water never activated"));
        }
        if (!randomTickUntil(helper, POS, 2 * ATTEMPTS, h -> {
            final BlockState crystal = h.getBlockState(crystalPos);
            return crystal.is(EndStoneBlocks.SULPHUR_CRYSTAL) && crystal.getValue(SulphurCrystalBlock.AGE) > 0;
        })) {
            throw helper.assertionException(Component.literal(
                    "active brimstone did not age the attached sulphur crystal within " + 2 * ATTEMPTS
                            + " random ticks, crystal is " + helper.getBlockState(crystalPos)
            ));
        }
        helper.succeed();
    }
}
