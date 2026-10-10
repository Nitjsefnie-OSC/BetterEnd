package org.betterx.betterend.testmod.gametest;

import org.betterx.bclib.trait.block.PlantBlockTrait;
import org.betterx.bclib.trait.block.PlantLikeBlockTrait;
import org.betterx.bclib.trait.block.WaterPlantBlockTrait;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.block.EndTerrainBlocks;
import de.ambertation.wover.block.api.trait.BlockTrait;
import de.ambertation.wover.tag.api.predefined.CommonBlockTags;
import de.ambertation.wover.test.api.gametest.FluidWashSweep;
import de.ambertation.wover.test.api.gametest.GameTestAssertions;

import com.mojang.logging.LogUtils;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Every BetterEnd plant without a collision shape must be washed away by flowing water and lava, like
 * vanilla's plants. 26.3 decides that by {@code #minecraft:washed_away_by_fluids} instead of
 * {@code blocksMotion()}, so a plant missing from the tag silently became a fluid barrier. Vanilla
 * {@code short_grass} runs through the in-world tests as the control.
 */
public class PlantFluidWashGameTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<TagKey<Block>> PLANT_TAGS = List.of(
            CommonBlockTags.PLANT,
            CommonBlockTags.SAPLINGS,
            CommonBlockTags.SEEDS,
            CommonBlockTags.VINES,
            CommonBlockTags.WATER_PLANT,
            BlockTags.SAPLINGS,
            BlockTags.FLOWERS,
            BlockTags.CROPS
    );

    private static boolean isPlant(Block block) {
        if (BlockTrait.hasRuntimeTrait(block, PlantBlockTrait.KEY)
                || BlockTrait.hasRuntimeTrait(block, PlantLikeBlockTrait.KEY)
                || BlockTrait.hasRuntimeTrait(block, WaterPlantBlockTrait.KEY)) {
            return true;
        }
        final BlockState state = block.defaultBlockState();
        return PLANT_TAGS.stream().anyMatch(state::is);
    }

    private static List<Block> soils() {
        return List.of(
                Blocks.END_STONE, Blocks.GRASS_BLOCK, Blocks.FARMLAND, Blocks.GRAVEL, Blocks.SAND,
                EndTerrainBlocks.END_MYCELIUM, EndTerrainBlocks.END_MOSS, EndTerrainBlocks.CHORUS_NYLIUM,
                EndTerrainBlocks.CAVE_MOSS, EndTerrainBlocks.CRYSTAL_MOSS, EndTerrainBlocks.SHADOW_GRASS,
                EndTerrainBlocks.PINK_MOSS, EndTerrainBlocks.AMBER_MOSS, EndTerrainBlocks.JUNGLE_MOSS,
                EndTerrainBlocks.SANGNUM, EndTerrainBlocks.RUTISCUS, EndTerrainBlocks.PALLIDIUM_FULL,
                EndTerrainBlocks.ENDSTONE_DUST
        );
    }

    /** Vanilla short grass first, then every BetterEnd plant the tag should hold. */
    private static List<Block> washable(GameTestHelper helper) {
        final List<Block> result = new ArrayList<>();
        result.add(Blocks.SHORT_GRASS);
        for (Block block : FluidWashSweep.plantsIn(BetterEnd.MOD_ID, PlantFluidWashGameTest::isPlant)) {
            if (FluidWashSweep.isDecidedByTag(block) && FluidWashSweep.shouldBeWashedAway(helper, block)) {
                result.add(block);
            }
        }
        return result;
    }

    @GameTest
    public void plantsAreInWashedAwayTag(GameTestHelper helper) {
        final List<Block> plants = FluidWashSweep.plantsIn(BetterEnd.MOD_ID, PlantFluidWashGameTest::isPlant);
        if (plants.size() < 50) {
            GameTestAssertions.failIfAny(helper, "Plant sweep", List.of("found only " + plants.size() + " plants"));
        }
        GameTestAssertions.failIfAny(helper, "Plants vs #minecraft:washed_away_by_fluids", FluidWashSweep.findTagMismatches(helper, plants));
        helper.succeed();
    }

    @GameTest(maxTicks = 200)
    public void waterWashesPlantsAway1(GameTestHelper helper) {
        wash(helper, Blocks.WATER, 0);
    }

    @GameTest(maxTicks = 200)
    public void waterWashesPlantsAway2(GameTestHelper helper) {
        wash(helper, Blocks.WATER, 1);
    }

    @GameTest(maxTicks = 200)
    public void lavaWashesPlantsAway1(GameTestHelper helper) {
        wash(helper, Blocks.LAVA, 0);
    }

    @GameTest(maxTicks = 200)
    public void lavaWashesPlantsAway2(GameTestHelper helper) {
        wash(helper, Blocks.LAVA, 1);
    }

    /** Half {@code part} of {@link #washable}, each half led by the vanilla control. */
    private static void wash(GameTestHelper helper, Block fluid, int part) {
        final List<Block> all = washable(helper);
        final List<Block> plants = new ArrayList<>();
        plants.add(Blocks.SHORT_GRASS);
        for (int i = 1; i < all.size(); i++) {
            if (i % 2 == part) plants.add(all.get(i));
        }
        final List<Block> skipped = FluidWashSweep.washEach(helper, fluid, plants, soils());
        LOGGER.info(
                "{} wash test {}: {} plants in-world, {} only tag-checked (survive on no test soil): {}",
                FluidWashSweep.id(fluid), part + 1, plants.size() - skipped.size(), skipped.size(),
                skipped.stream().map(FluidWashSweep::id).toList()
        );
        if (skipped.contains(Blocks.SHORT_GRASS)) {
            GameTestAssertions.failIfAny(helper, "Control", List.of("short_grass survives on none of the test soils"));
        }
    }
}
