package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.blocks.BulbVineLanternBlock;
import org.betterx.betterend.blocks.HydraluxPetalColoredBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression for BetterEnd #370: the dyed bulb lanterns (and the dyed hydralux petal blocks, built by the
 * same {@code ColoredMaterial}) had no loot table, so breaking them - with any tool - dropped nothing.
 * <p>
 * Sweeps every BetterEnd bulb lantern (plain and dyed) and every dyed petal block and checks that a
 * diamond pickaxe (lanterns) / diamond axe (petals) is an effective and correct tool, that the block is not
 * insta-breakable (the plain lantern's destroy time must survive the property copy), and that the real
 * loot table drops the block itself.
 */
public class ColoredBlockDropGameTest {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    @GameTest
    public void bulbLanternsAndDyedPetalsDropThemselves(GameTestHelper helper) {
        final ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        final ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        final List<String> failures = new ArrayList<>();
        int checked = 0;

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(BetterEnd.MOD_ID)) continue;
            final ItemStack tool;
            if (block instanceof BulbVineLanternBlock) tool = pickaxe;
            else if (block instanceof HydraluxPetalColoredBlock) tool = axe;
            else continue;
            checked++;

            final String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            final BlockState state = block.defaultBlockState();
            if (tool.getDestroySpeed(state) <= 1.0f) {
                failures.add(id + ": " + tool.getItem() + " does not mine it faster than a bare hand");
            }
            if (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)) {
                failures.add(id + ": " + tool.getItem() + " is not a correct tool for drops");
            }
            if (state.getDestroySpeed(helper.getLevel(), helper.absolutePos(POS)) <= 0) {
                failures.add(id + ": destroy time is " + state.getDestroySpeed(helper.getLevel(), helper.absolutePos(POS)));
            }
            final List<ItemStack> drops = Block.getDrops(
                    state, helper.getLevel(), helper.absolutePos(POS), null, null, tool
            );
            if (drops.stream().noneMatch(s -> s.is(block.asItem()))) {
                failures.add(id + ": dropped " + drops + " instead of itself");
            }
        }

        if (checked < 3 * 17 + 16) {
            failures.add("expected at least " + (3 * 17 + 16) + " lantern/petal blocks, found " + checked);
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Bulb lantern / dyed petal drop regression (" + failures.size() + "):\n - "
                            + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }
}
