package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.block.EndTerrainBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression test for BetterEnd #606: cave moss lost its {@code shape} (top/middle/bottom) blockstate.
 * <p>
 * Cave moss used to be a BCLib {@code TripleTerrainBlock}: {@code bottom} is floor moss, {@code top} the flipped
 * ceiling variant and {@code middle} moss on every face. When that class was retired the block became a plain
 * {@code BaseTerrainBlock}, so the ceiling moss of lush caves was the floor model (moss facing the rock) and,
 * because {@code BaseTerrainBlock.randomTick} only checks the face <em>above</em>, every random tick could turn
 * ceiling moss back into end stone.
 * <p>
 * The property is looked up by name so this test compiles (and fails) against the unfixed block too.
 */
public class CaveMossShapeGameTest {
    private static final BlockPos CEILING_ROCK = new BlockPos(1, 4, 1);
    private static final BlockPos CEILING_MOSS = CEILING_ROCK.below();

    @GameTest
    public void caveMossHasTopMiddleBottomShapes(GameTestHelper helper) {
        final Property<?> shape = shapeProperty();
        if (shape == null) {
            throw helper.assertionException(Component.literal("betterend:cave_moss has no 'shape' blockstate property"));
        }
        final List<String> values = shape.getPossibleValues().stream().map(v -> valueName(shape, v)).sorted().toList();
        if (!values.equals(List.of("bottom", "middle", "top"))) {
            throw helper.assertionException(Component.literal(
                    "betterend:cave_moss 'shape' has values " + values + ", expected [bottom, middle, top]"));
        }
        final String def = stateValue(EndTerrainBlocks.CAVE_MOSS.defaultBlockState(), shape);
        if (!"bottom".equals(def)) {
            throw helper.assertionException(Component.literal(
                    "default cave_moss state is shape=" + def + ", expected bottom (floor moss)"));
        }
        helper.succeed();
    }

    /** Ceiling moss (rock above, air below) must survive random ticks instead of reverting to end stone. */
    @GameTest
    public void ceilingCaveMossSurvivesRandomTicks(GameTestHelper helper) {
        helper.setBlock(CEILING_ROCK, Blocks.END_STONE);
        helper.setBlock(CEILING_MOSS, ceilingState());
        helper.setBlock(CEILING_MOSS.below(), Blocks.AIR);

        final BlockPos abs = helper.absolutePos(CEILING_MOSS);
        final RandomSource random = RandomSource.create(606);
        for (int i = 0; i < 400; i++) {
            final BlockState state = helper.getLevel().getBlockState(abs);
            if (!state.is(EndTerrainBlocks.CAVE_MOSS)) {
                throw helper.assertionException(Component.literal(
                        "ceiling cave moss turned into " + state + " after " + i + " random ticks"));
            }
            state.randomTick(helper.getLevel(), abs, random);
        }
        helper.succeed();
    }

    private static BlockState ceilingState() {
        final Property<?> shape = shapeProperty();
        final BlockState state = EndTerrainBlocks.CAVE_MOSS.defaultBlockState();
        return shape == null ? state : withValue(state, shape, "top");
    }

    private static Property<?> shapeProperty() {
        return EndTerrainBlocks.CAVE_MOSS.getStateDefinition().getProperty("shape");
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String name) {
        return state.setValue(property, property.getValue(name).orElseThrow());
    }

    private static <T extends Comparable<T>> String stateValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String valueName(Property<T> property, Object value) {
        return property.getName((T) value);
    }
}
