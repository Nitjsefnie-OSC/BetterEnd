package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.blocks.SulphurCrystalBlock;
import org.betterx.betterend.registry.block.EndCrystalBlocks;
import org.betterx.betterend.registry.block.EndStoneBlocks;
import org.betterx.betterend.world.features.terrain.SingleBlockFeature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Sulphur crystals (and the smaragdant shard and hydrothermal vent, which shared the pattern) used to
 * default to {@code waterlogged=true}: anything placing them without an explicit state - {@code /setblock},
 * structures, features - left a water source behind once the block was broken. The default is now dry,
 * and world generation derives the flag from the fluid it actually replaces.
 */
public class WaterloggedDefaultStateGameTest {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos ABOVE = GROUND.above();

    @GameTest
    public void defaultStatesAreNotWaterlogged(GameTestHelper helper) {
        for (Block block : List.of(
                EndStoneBlocks.SULPHUR_CRYSTAL,
                EndCrystalBlocks.SMARAGDANT_CRYSTAL_SHARD,
                EndStoneBlocks.HYDROTHERMAL_VENT
        )) {
            if (block.defaultBlockState().getValue(BlockStateProperties.WATERLOGGED)) {
                throw helper.assertionException(Component.literal(block + " still defaults to waterlogged=true"));
            }
        }
        helper.succeed();
    }

    @GameTest
    public void defaultCrystalOnLandLeavesNoWaterWhenBroken(GameTestHelper helper) {
        helper.setBlock(GROUND, EndStoneBlocks.BRIMSTONE);
        helper.setBlock(ABOVE, EndStoneBlocks.SULPHUR_CRYSTAL.defaultBlockState());
        helper.getLevel().destroyBlock(helper.absolutePos(ABOVE), false);
        final BlockState after = helper.getBlockState(ABOVE);
        if (!after.isAir() || !after.getFluidState().isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "breaking a default-state sulphur crystal on land left " + after));
        }
        helper.succeed();
    }

    @GameTest
    public void naturalCrystalFollowsReplacedFluid(GameTestHelper helper) {
        final BlockState crystal = EndStoneBlocks.SULPHUR_CRYSTAL.defaultBlockState();
        final BlockState wet = SulphurCrystalBlock.naturalState(crystal, Blocks.WATER.defaultBlockState(), Direction.UP, 2);
        final BlockState dry = SulphurCrystalBlock.naturalState(crystal, Blocks.AIR.defaultBlockState(), Direction.UP, 2);
        if (!wet.getValue(SulphurCrystalBlock.WATERLOGGED) || dry.getValue(SulphurCrystalBlock.WATERLOGGED)) {
            throw helper.assertionException(Component.literal(
                    "feature crystal state should be waterlogged exactly in water: in water " + wet + ", in air " + dry));
        }
        helper.succeed();
    }

    @GameTest
    public void featurePlacedShardFollowsFluid(GameTestHelper helper) {
        final SingleBlockFeature feature = new SingleBlockFeature();
        final SimpleBlockConfiguration config = new SimpleBlockConfiguration(
                BlockStateProvider.simple(EndCrystalBlocks.SMARAGDANT_CRYSTAL_SHARD));
        for (boolean underwater : new boolean[]{false, true}) {
            helper.setBlock(GROUND, Blocks.END_STONE);
            helper.setBlock(ABOVE, underwater ? Blocks.WATER : Blocks.AIR);
            final boolean placed = feature.place(new FeaturePlaceContext<>(
                    Optional.empty(),
                    helper.getLevel(),
                    helper.getLevel().getChunkSource().getGenerator(),
                    helper.getLevel().getRandom(),
                    helper.absolutePos(ABOVE),
                    config
            ));
            final BlockState state = helper.getBlockState(ABOVE);
            if (!placed || !state.is(EndCrystalBlocks.SMARAGDANT_CRYSTAL_SHARD)
                    || state.getValue(BlockStateProperties.WATERLOGGED) != underwater) {
                throw helper.assertionException(Component.literal(
                        "feature-placed shard " + (underwater ? "under water" : "on land") + " ended up as " + state));
            }
        }
        helper.succeed();
    }
}
