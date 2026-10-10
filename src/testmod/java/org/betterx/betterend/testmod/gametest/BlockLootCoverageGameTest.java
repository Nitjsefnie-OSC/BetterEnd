package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.BetterEnd;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Sweeps every registered {@code betterend:} block for loot that silently went missing.
 * <p>
 * Commit d9bac273f moved block properties out of the constructors and deleted the generated data; every
 * block whose definition lost its loot-table trait on the way kept a {@code drops} key that pointed at a
 * table which was simply never generated again, so breaking it dropped nothing (#370 was 64 of them, the
 * smaragdant crystal sub-blocks, terminite/thallasium blocks and tiles, brimstone and missing tile followed).
 * <p>
 * Unless a block is listed in {@link #DROPLESS} (each entry says why), this checks that
 * <ul>
 *     <li>it has a loot-table key at all,</li>
 *     <li>a block with an item drops <em>something</em> in at least one of its states with at least one
 *     ordinary tool (bare hand, the diamond tools, shears, or silk touch) - through its loot table or a
 *     {@code getDrops} override, which is what furnaces, anvils and the smelter use, and</li>
 *     <li>a block without an item resolves to a loot table that is actually loaded.</li>
 * </ul>
 */
public class BlockLootCoverageGameTest {
    private static final BlockPos POS = new BlockPos(1, 2, 1);

    /**
     * Blocks that deliberately drop nothing when broken.
     */
    private static final Set<String> DROPLESS = Set.of(
            // the eternal portal's portal surface; like vanilla's nether_portal it has no table
            "betterend:end_portal_block",
            // technical bubble column above hydrothermal vents, like vanilla's bubble_column
            "betterend:vent_bubble_column",
            // the leaves of the end lotus have no item; the lotus drops from its stem and flower
            "betterend:end_lotus_leaf",
            // not obtainable, like vanilla's budding_amethyst
            "betterend:budding_smaragdant_crystal",
            // the unbreakable (negative strength) variants that make up the eternal portal frame; the
            // pedestal only gives back the item placed on it
            "betterend:flavolite_runed_eternal",
            "betterend:eternal_pedestal"
    );

    @GameTest
    public void everyBlockDropsSomething(GameTestHelper helper) {
        final var registries = helper.getLevel().registryAccess();
        final Holder<Enchantment> silkTouch = registries.lookupOrThrow(Registries.ENCHANTMENT)
                                                        .getOrThrow(Enchantments.SILK_TOUCH);
        final ItemStack silkPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPickaxe.enchant(silkTouch, 1);
        final ItemStack silkHoe = new ItemStack(Items.DIAMOND_HOE);
        silkHoe.enchant(silkTouch, 1);
        final List<ItemStack> tools = List.of(
                ItemStack.EMPTY,
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.DIAMOND_AXE),
                new ItemStack(Items.DIAMOND_SHOVEL),
                new ItemStack(Items.DIAMOND_HOE),
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.SHEARS),
                silkPickaxe,
                silkHoe
        );

        final var lootTables = helper.getLevel().getServer().reloadableRegistries();
        final List<String> failures = new ArrayList<>();
        int checked = 0;

        for (Block block : BuiltInRegistries.BLOCK) {
            final var blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (!blockId.getNamespace().equals(BetterEnd.MOD_ID)) continue;
            final String id = blockId.toString();
            if (DROPLESS.contains(id)) continue;
            checked++;

            final Optional<ResourceKey<LootTable>> key = block.getLootTable();
            if (key.isEmpty()) {
                failures.add(id + ": has no loot table key (noLootTable)");
                continue;
            }
            final boolean tableLoaded = lootTables.getLootTable(key.get()) != LootTable.EMPTY;

            if (block.asItem() == Items.AIR) {
                if (!tableLoaded) failures.add(id + ": loot table " + key.get().identifier() + " is not loaded");
                continue;
            }

            if (!dropsSomething(helper, block, tools, failures)) {
                failures.add(id + ": drops nothing in any state with any tool (loot table "
                        + key.get().identifier() + (tableLoaded ? "" : " is not loaded") + ")");
            }
        }

        if (checked < 600) failures.add("expected at least 600 betterend blocks, found " + checked);
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Block loot coverage (" + failures.size() + " problems):\n - "
                            + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    private static boolean dropsSomething(
            GameTestHelper helper,
            Block block,
            List<ItemStack> tools,
            List<String> failures
    ) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) {
            for (ItemStack tool : tools) {
                try {
                    final List<ItemStack> drops = Block.getDrops(
                            state, helper.getLevel(), helper.absolutePos(POS), null, null, tool
                    );
                    if (drops.stream().anyMatch(s -> !s.isEmpty())) return true;
                } catch (RuntimeException e) {
                    failures.add(BuiltInRegistries.BLOCK.getKey(block) + ": computing drops of " + state
                            + " with " + tool.getItem() + " threw " + e);
                    return true;
                }
            }
        }
        return false;
    }
}
