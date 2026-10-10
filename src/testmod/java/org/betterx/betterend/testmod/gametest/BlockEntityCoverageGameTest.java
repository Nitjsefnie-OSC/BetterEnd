package org.betterx.betterend.testmod.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Every betterend: block that has a block entity must be a valid block for the type it creates.
 * <p>
 * The shared pedestal and flower-pot types are built from whatever pedestals/pots are registered when
 * {@code EndBlockEntities} is loaded, which is before the full block registry is. The smaragdant
 * crystal pedestal was missing from the pedestal type, so {@code /setblock} (and placing it) failed
 * while creating the block entity. Creating the entity directly runs the same validation.
 */
public class BlockEntityCoverageGameTest {
    @GameTest
    public void everyEntityBlockCreatesAValidBlockEntity(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        final BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        int checked = 0;

        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            final Block block = entry.getValue();
            final String id = entry.getKey().location().toString();
            if (!id.startsWith("betterend:")) continue;
            if (!(block instanceof EntityBlock entityBlock)) continue;

            final BlockState state = block.defaultBlockState();
            final BlockEntity blockEntity;
            try {
                blockEntity = entityBlock.newBlockEntity(pos, state);
            } catch (Throwable t) {
                failures.add(id + ": newBlockEntity threw " + t);
                continue;
            }
            checked++;

            if (blockEntity == null) {
                failures.add(id + ": newBlockEntity returned null");
            } else if (!blockEntity.getType().isValid(state)) {
                failures.add(id + ": not a valid block for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()));
            }
        }

        if (checked == 0) {
            throw helper.assertionException(Component.literal("no betterend: entity blocks found"));
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "blocks that cannot create their block entity:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }
}
