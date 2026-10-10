package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.blocks.ChandelierBlock;
import de.ambertation.wover.test.api.gametest.FluidWashSweep;
import de.ambertation.wover.test.api.gametest.GameTestAssertions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Chandeliers are non-colliding lights on a floor, wall or ceiling and must be washed away by water and
 * lava like vanilla's torches. Up to 26.2 that followed from having no collision shape; 26.3 only lets a
 * fluid flow into blocks in {@code #minecraft:washed_away_by_fluids}. Vanilla's torch is the control.
 */
public class ChandelierFluidWashGameTest {
    private static List<Block> chandeliers() {
        final List<Block> found = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof ChandelierBlock) found.add(block);
        }
        return found;
    }

    @GameTest
    public void chandeliersAreInWashedAwayTag(GameTestHelper helper) {
        final List<Block> chandeliers = chandeliers();
        final List<String> failures = new ArrayList<>();
        if (chandeliers.isEmpty()) failures.add("no ChandelierBlock registered at all");
        for (Block block : chandeliers) {
            if (!FluidWashSweep.shouldBeWashedAway(helper, block)) {
                failures.add(FluidWashSweep.id(block) + " has a collision shape");
            }
            if (!block.defaultBlockState().is(BlockTags.WASHED_AWAY_BY_FLUIDS)) {
                failures.add(FluidWashSweep.id(block) + " is not in #minecraft:washed_away_by_fluids");
            }
        }
        GameTestAssertions.failIfAny(helper, "Chandeliers vs #minecraft:washed_away_by_fluids", failures);
        helper.succeed();
    }

    @GameTest(maxTicks = 200)
    public void waterWashesChandeliersAway(GameTestHelper helper) {
        wash(helper, Blocks.WATER);
    }

    @GameTest(maxTicks = 200)
    public void lavaWashesChandeliersAway(GameTestHelper helper) {
        wash(helper, Blocks.LAVA);
    }

    private static void wash(GameTestHelper helper, Block fluid) {
        final List<Block> blocks = new ArrayList<>();
        blocks.add(Blocks.TORCH);
        blocks.addAll(chandeliers());
        final List<Block> skipped = FluidWashSweep.washEach(helper, fluid, blocks, List.of(Blocks.STONE));
        if (!skipped.isEmpty()) {
            GameTestAssertions.failIfAny(helper, "Setup", List.of("could not place " + skipped.stream().map(FluidWashSweep::id).toList()));
        }
    }
}
