package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.blocks.FlowerPotBlock;
import org.betterx.betterend.blocks.entities.FlowerPotBlockEntity;
import org.betterx.betterend.registry.block.EndSaplingBlocks;
import org.betterx.betterend.registry.block.EndTerrainBlocks;
import de.ambertation.wover.test.api.gametest.PottableSweep;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Covers flower-pot soil acceptance data, via {@code wover-test-api}'s {@link PottableSweep}, for every
 * plant BetterEnd registers as pottable rather than a hand-picked few.
 * <p>
 * This checks the registry data - every plant's declared soil tag actually matches at least one
 * registered soil - not the physical "place plant in my flower pot block" interaction, which is
 * necessarily BetterEnd-specific (WorldWeaver's {@code wover-pottable-api} is a data registry, not a pot
 * block implementation - see {@code PottableSweep}'s own class javadoc). A plant whose soil tag matches
 * nothing is a silent dead end no amount of clicking a real pot would ever catch, since the plant would
 * never be potable on anything at all.
 */
public class FlowerPotGameTest {
    @GameTest
    public void everyPlantsSoilTagIsSatisfiableBySomeRegisteredSoil(GameTestHelper helper) {
        final List<String> problems = PottableSweep.findPlantsWithUnsatisfiableSoilTag(helper, "betterend");

        if (!problems.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "betterend pottable-plant regression:\n - " + String.join("\n - ", problems)
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void atLeastOnePlantAndSoilAreRegistered(GameTestHelper helper) {
        if (PottableSweep.plantsIn(helper, "betterend").isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "no betterend plants are registered as pottable at all - either a real regression, or"
                            + " this sweep is looking in the wrong place"
            ));
        }
        helper.succeed();
    }

    /**
     * #389: the pre-26.x pot encoded plant and soil as block-state integers (4352 states per pot) and
     * timed out building its state map. Contents now live in the block entity, so every pot variant must
     * stay at the 4 POT_LIGHT states, get a {@link FlowerPotBlockEntity}, and keep a seated plant
     * (#542/#447/#489: Dragon Tree Sapling in a Violecite pot showed only a hitbox).
     */
    @GameTest
    public void everyPotVariantHasFewStatesAndKeepsItsContents(GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final List<String> problems = new ArrayList<>();
        int pots = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof FlowerPotBlock)) continue;
            pots++;
            final String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            final int states = block.getStateDefinition().getPossibleStates().size();
            if (states > 4) problems.add(id + " has " + states + " block states (expected 4)");

            helper.setBlock(pos, block);
            if (!(helper.getBlockEntity(pos, FlowerPotBlockEntity.class) instanceof FlowerPotBlockEntity pot)) {
                problems.add(id + " has no flower pot block entity");
                continue;
            }
            pot.setSoil(Optional.of(EndTerrainBlocks.SHADOW_GRASS.builtInRegistryHolder().key()));
            pot.setPlant(Optional.of(EndSaplingBlocks.DRAGON_TREE_SAPLING.builtInRegistryHolder().key()));
            final FlowerPotBlockEntity after = helper.getBlockEntity(pos, FlowerPotBlockEntity.class);
            if (after == null || after.getPlantBlock().orElse(null) != EndSaplingBlocks.DRAGON_TREE_SAPLING
                    || after.getSoilBlock().orElse(null) != EndTerrainBlocks.SHADOW_GRASS) {
                problems.add(id + " lost its seated soil/plant");
            }
        }
        if (pots == 0) problems.add("no FlowerPotBlock registered at all");
        if (!problems.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "betterend flower pot regression:\n - " + String.join("\n - ", problems)
            ));
        }
        helper.succeed();
    }

    /** Every pot is in {@code #minecraft:flower_pots} like vanilla's {@code flower_pot}. */
    @GameTest
    public void potsAreFlowerPots(GameTestHelper helper) {
        final List<String> problems = new ArrayList<>();
        int pots = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof FlowerPotBlock)) continue;
            pots++;
            if (!block.defaultBlockState().is(BlockTags.FLOWER_POTS)) {
                problems.add(BuiltInRegistries.BLOCK.getKey(block) + " is not in #minecraft:flower_pots");
            }
        }
        if (pots == 0) problems.add("no FlowerPotBlock registered at all");
        if (!problems.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Pots vs #minecraft:flower_pots:\n - " + String.join("\n - ", problems)
            ));
        }
        helper.succeed();
    }
}
