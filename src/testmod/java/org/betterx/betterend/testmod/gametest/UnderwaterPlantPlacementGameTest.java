package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.block.EndTerrainBlocks;
import org.betterx.betterend.registry.block.EndWaterPlantBlocks;

import de.ambertation.wover.test.api.gametest.MockPlayers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * #532: bubble coral (an {@code UnderwaterPlantBlock}) must be placeable by a player, through the real
 * {@code BlockItem} path, into a water source on top of any block in its survival tag - and, being an
 * underwater plant, not out of water.
 */
public class UnderwaterPlantPlacementGameTest {
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos ABOVE = GROUND.above();
    private static final BlockPos PLAYER = new BlockPos(3, 1, 1);

    @GameTest
    public void bubbleCoralPlacesInWaterOnEndStoneAndMoss(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        for (Block ground : List.of(Blocks.END_STONE, EndTerrainBlocks.END_MOSS, EndTerrainBlocks.CAVE_MOSS)) {
            helper.setBlock(GROUND, ground);
            helper.setBlock(ABOVE, Blocks.WATER);
            place(helper);
            if (!helper.getBlockState(ABOVE).is(EndWaterPlantBlocks.BUBBLE_CORAL)) {
                failures.add("using bubble coral on underwater " + ground + " left " + helper.getBlockState(ABOVE));
            }
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "bubble coral placement regression:\n - " + String.join("\n - ", failures)));
        }
        helper.succeed();
    }

    @GameTest
    public void bubbleCoralDoesNotPlaceOutOfWater(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.END_STONE);
        helper.setBlock(ABOVE, Blocks.AIR);
        place(helper);
        if (helper.getBlockState(ABOVE).is(EndWaterPlantBlocks.BUBBLE_CORAL)) {
            throw helper.assertionException(Component.literal("bubble coral was placed out of water"));
        }
        helper.succeed();
    }

    private static void place(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, PLAYER);
        final ItemStack stack = new ItemStack(EndWaterPlantBlocks.BUBBLE_CORAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos abs = helper.absolutePos(GROUND);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }
}
