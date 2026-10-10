package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.blocks.SulphurCrystalBlock;
import org.betterx.betterend.registry.block.EndStoneBlocks;
import org.betterx.betterend.registry.item.EndResourceItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression for BetterEnd #517: a fully grown sulphur crystal dropped nothing without silk touch.
 * <p>
 * The loot table conditioned the crystalline sulphur pool on {@code age=3}, but
 * {@link SulphurCrystalBlock#AGE} only goes up to {@link SulphurCrystalBlock#MAX_AGE} (2), so the
 * condition could never match. Drops are computed through {@code Block.getDrops}, i.e. the real
 * generated loot table, not by inspecting the builder.
 * <p>
 * Follow-up: the young stages (age 0 and 1) always drop exactly one crystal of themselves,
 * regardless of tool.
 */
public class SulphurCrystalDropGameTest {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    private static List<ItemStack> drops(GameTestHelper helper, int age, ItemStack tool) {
        final BlockState state = EndStoneBlocks.SULPHUR_CRYSTAL.defaultBlockState()
                                                               .setValue(SulphurCrystalBlock.FACING, Direction.UP)
                                                               .setValue(SulphurCrystalBlock.AGE, age);
        helper.setBlock(POS, state);
        return Block.getDrops(state, helper.getLevel(), helper.absolutePos(POS), null, null, tool);
    }

    @GameTest
    public void fullyGrownCrystalDropsCrystallineSulphur(GameTestHelper helper) {
        final List<ItemStack> drops = drops(helper, SulphurCrystalBlock.MAX_AGE, new ItemStack(Items.IRON_PICKAXE));
        final int count = drops.stream()
                               .filter(s -> s.is(EndResourceItems.CRYSTALLINE_SULPHUR))
                               .mapToInt(ItemStack::getCount)
                               .sum();
        if (count < 1 || count > 3) {
            throw helper.assertionException(Component.literal(
                    "a fully grown sulphur crystal mined with an iron pickaxe should drop 1-3 crystalline sulphur, got "
                            + drops
            ));
        }
        helper.succeed();
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static ItemStack pickaxe(GameTestHelper helper, ResourceKey<Enchantment> key, int level) {
        final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.enchant(enchantment(helper, key), level);
        return pickaxe;
    }

    private static boolean isExactlyOneCrystal(List<ItemStack> drops) {
        return drops.size() == 1
                && drops.get(0).is(EndStoneBlocks.SULPHUR_CRYSTAL.asItem())
                && drops.get(0).getCount() == 1;
    }

    /**
     * Maintainer decision (#517 follow-up): the young crystals (age 0 and 1) always drop exactly one
     * copy of themselves, with any tool and without fortune multiplication.
     */
    @GameTest
    public void youngCrystalsAlwaysDropThemselves(GameTestHelper helper) {
        final List<ItemStack> tools = List.of(
                ItemStack.EMPTY,
                new ItemStack(Items.IRON_PICKAXE),
                pickaxe(helper, Enchantments.SILK_TOUCH, 1),
                pickaxe(helper, Enchantments.FORTUNE, 3)
        );
        for (int age = 0; age < SulphurCrystalBlock.MAX_AGE; age++) {
            for (ItemStack tool : tools) {
                // several rolls so a fortune multiplier would show up
                for (int i = 0; i < 16; i++) {
                    final List<ItemStack> drops = drops(helper, age, tool);
                    if (!isExactlyOneCrystal(drops)) {
                        throw helper.assertionException(Component.literal(
                                "an age-" + age + " sulphur crystal should drop exactly one sulphur crystal with "
                                        + (tool.isEmpty() ? "an empty hand" : tool.toString())
                                        + ", got " + drops
                        ));
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest
    public void silkTouchDropsTheCrystalItself(GameTestHelper helper) {
        final ItemStack pickaxe = pickaxe(helper, Enchantments.SILK_TOUCH, 1);

        final List<ItemStack> drops = drops(helper, SulphurCrystalBlock.MAX_AGE, pickaxe);
        if (drops.stream().noneMatch(s -> s.is(EndStoneBlocks.SULPHUR_CRYSTAL.asItem()))
                || drops.stream().anyMatch(s -> s.is(EndResourceItems.CRYSTALLINE_SULPHUR))) {
            throw helper.assertionException(Component.literal(
                    "a silk-touch pickaxe should drop the sulphur crystal block and no crystalline sulphur, got "
                            + drops
            ));
        }
        helper.succeed();
    }
}
