package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.blocks.RespawnObeliskBlock;
import org.betterx.betterend.registry.block.EndFunctionalBlocks;
import org.betterx.betterend.registry.item.EndResourceItems;
import de.ambertation.wover.block.api.BlockProperties.TripleShape;
import de.ambertation.wover.test.api.gametest.MockPlayers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Covers {@code respawn_obelisk}'s activation rules: main-hand-only, requires more than 5 amber gems,
 * consumes exactly 6 regardless of a larger stack. Also confirms it has no dimension gate, unlike
 * vanilla's bed (overworld-only) or respawn anchor (nether-only) - a real behavioural divergence worth
 * pinning down rather than assuming. Not mechanically re-tested in the Nether/End here though: GameTest
 * structures run in the overworld test dimension, and standing up a second dimension's worth of test
 * infrastructure just to prove {@code useItemOn} has no dimension check (readable directly in the
 * source - there is no such check anywhere in the method) would cost more than it proves.
 */
public class RespawnObeliskGameTest {
    private static final BlockPos OBELISK_POS = new BlockPos(1, 2, 1);

    private static BlockHitResult hit(GameTestHelper helper, BlockPos pos) {
        final BlockPos abs = helper.absolutePos(pos);
        return new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    }

    @GameTest
    public void offHandGemsDoNotActivateIt(GameTestHelper helper) {
        helper.setBlock(OBELISK_POS, EndFunctionalBlocks.RESPAWN_OBELISK);
        final ServerPlayer player = MockPlayers.survival(helper, OBELISK_POS.east());
        final BlockState state = helper.getBlockState(OBELISK_POS);
        final ItemStack gems = new ItemStack(EndResourceItems.AMBER_GEM, 10);

        state.useItemOn(gems, helper.getLevel(), player, InteractionHand.OFF_HAND, hit(helper, OBELISK_POS));

        if (gems.getCount() != 10) {
            throw helper.assertionException(Component.literal(
                    "off-hand amber gems were consumed (count now " + gems.getCount()
                            + ") - activation should be main-hand-only"
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void mainHandActivatesAndConsumesExactlySix(GameTestHelper helper) {
        helper.setBlock(OBELISK_POS, EndFunctionalBlocks.RESPAWN_OBELISK);
        final ServerPlayer player = MockPlayers.survival(helper, OBELISK_POS.east());
        final BlockState state = helper.getBlockState(OBELISK_POS);
        final ItemStack gems = new ItemStack(EndResourceItems.AMBER_GEM, 10);

        state.useItemOn(gems, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, OBELISK_POS));

        final List<String> failures = new ArrayList<>();
        if (gems.getCount() != 4) {
            failures.add("expected exactly 6 gems consumed from a stack of 10 (leaving 4), got "
                    + gems.getCount() + " remaining");
        }
        if (player.getRespawnConfig() == null) {
            failures.add("player's respawn config was never set");
        }

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "respawn_obelisk activation regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void fiveGemsAreNotEnough(GameTestHelper helper) {
        helper.setBlock(OBELISK_POS, EndFunctionalBlocks.RESPAWN_OBELISK);
        final ServerPlayer player = MockPlayers.survival(helper, OBELISK_POS.east());
        final BlockState state = helper.getBlockState(OBELISK_POS);
        final ItemStack gems = new ItemStack(EndResourceItems.AMBER_GEM, 5);

        state.useItemOn(gems, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit(helper, OBELISK_POS));

        if (gems.getCount() != 5) {
            throw helper.assertionException(Component.literal(
                    "exactly 5 amber gems (the documented threshold is \"more than 5\") still activated"
                            + " the obelisk - count is now " + gems.getCount()
            ));
        }
        helper.succeed();
    }

    // --- respawn position: the player must fit (feet and head), like at a vanilla respawn anchor ---

    private static final BlockPos RESPAWN_OBELISK_POS = new BlockPos(2, 2, 2);

    private static void buildObelisk(GameTestHelper helper) {
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        final BlockState obelisk = EndFunctionalBlocks.RESPAWN_OBELISK.defaultBlockState();
        helper.setBlock(RESPAWN_OBELISK_POS, obelisk.setValue(RespawnObeliskBlock.SHAPE, TripleShape.BOTTOM));
        helper.setBlock(RESPAWN_OBELISK_POS.above(), obelisk.setValue(RespawnObeliskBlock.SHAPE, TripleShape.MIDDLE));
        helper.setBlock(RESPAWN_OBELISK_POS.above(2), obelisk.setValue(RespawnObeliskBlock.SHAPE, TripleShape.TOP));
    }

    private static Vec3 respawnAt(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, new BlockPos(0, 2, 0));
        player.setRespawnPosition(
                new ServerPlayer.RespawnConfig(
                        helper.getLevel().dimension(),
                        helper.absolutePos(RESPAWN_OBELISK_POS),
                        0.0F,
                        false
                ),
                false
        );
        return player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING).position();
    }

    @GameTest
    public void doesNotRespawnIntoAOneBlockHighGap(GameTestHelper helper) {
        buildObelisk(helper);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            helper.setBlock(RESPAWN_OBELISK_POS.relative(dir).above(), Blocks.OBSIDIAN);
        }

        final Vec3 respawn = respawnAt(helper);
        final Vec3 obelisk = Vec3.atBottomCenterOf(helper.absolutePos(RESPAWN_OBELISK_POS));
        if (respawn.distanceTo(obelisk) < 3) {
            throw helper.assertionException(Component.literal(
                    "player respawned at " + respawn + " next to the obelisk, although every neighbour is only"
                            + " one block high"
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void respawnsOnTheOnlyTwoBlockHighSide(GameTestHelper helper) {
        buildObelisk(helper);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir != Direction.EAST) {
                helper.setBlock(RESPAWN_OBELISK_POS.relative(dir).above(), Blocks.OBSIDIAN);
            }
        }

        final Vec3 respawn = respawnAt(helper);
        final Vec3 expected = Vec3.atBottomCenterOf(helper.absolutePos(RESPAWN_OBELISK_POS.east()));
        if (respawn.distanceTo(expected) > 1.0E-6) {
            throw helper.assertionException(Component.literal(
                    "player respawned at " + respawn + ", expected the free side " + expected
            ));
        }
        helper.succeed();
    }
}
