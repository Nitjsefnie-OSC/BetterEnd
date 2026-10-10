package org.betterx.betterend.testmod.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression for BetterEnd #474: helix tree leaves only ever dropped themselves, so the helix tree sapling
 * was unobtainable.
 * <p>
 * Sweeps every BetterEnd leaves-like block that is the source of a sapling (or of the seed/mushroom that
 * plays that role) and breaks it by hand many times through {@code Block.getDrops}, i.e. the real loot
 * table. The canopy blocks deliberately have very low rates (helix tree leaves and umbrella tree membranes
 * sit at 1/160 base), so the trial count is sized for those: a correct table fails to drop in
 * {@value #TRIALS} tries with probability e^-31.
 * <p>
 * {@link #everySaplingHasASourceBlock} keeps the table honest: any BetterEnd block tagged
 * {@code minecraft:saplings} must have a source here, and any BetterEnd block tagged
 * {@code minecraft:leaves} must either be a source or be listed in {@link #LEAVES_WITHOUT_SAPLING}.
 */
public class SaplingDropGameTest {
    private static final BlockPos POS = new BlockPos(1, 2, 1);
    private static final int TRIALS = 5000;
    // looked up by id: the 26.2 BlockTags constant for minecraft:saplings is not named SAPLINGS
    private static final TagKey<Block> SAPLINGS = TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("saplings"));

    /** leaves-like block id -> the item it should occasionally drop when broken by hand. */
    private static final Map<String, String> SOURCES = new LinkedHashMap<>();

    static {
        SOURCES.put("helix_tree_leaves", "helix_tree_sapling");
        SOURCES.put("umbrella_tree_membrane", "umbrella_tree_sapling");
        SOURCES.put("pythadendron_leaves", "pythadendron_sapling");
        SOURCES.put("lacugrove_leaves", "lacugrove_sapling");
        SOURCES.put("dragon_tree_leaves", "dragon_tree_sapling");
        SOURCES.put("tenanea_leaves", "tenanea_sapling");
        SOURCES.put("tenanea_outer_leaves", "tenanea_sapling");
        SOURCES.put("lucernia_leaves", "lucernia_sapling");
        SOURCES.put("lucernia_outer_leaves", "lucernia_sapling");
        SOURCES.put("mossy_glowshroom_fur", "mossy_glowshroom_sapling");
        SOURCES.put("glowing_pillar_leaves", "glowing_pillar_seed");
        SOURCES.put("blue_vine_fur", "blue_vine_seed");
        SOURCES.put("amaranita_fur", "small_amaranita_mushroom");
    }

    /** Leaves-tagged blocks that are not part of a sapling-grown plant. */
    private static final Set<String> LEAVES_WITHOUT_SAPLING = Set.of("cave_bush");

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(ResourceLocation.parse("betterend:" + path));
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(ResourceLocation.parse("betterend:" + path));
    }

    private static List<ItemStack> drops(GameTestHelper helper, BlockState state, ItemStack tool) {
        return Block.getDrops(state, helper.getLevel(), helper.absolutePos(POS), null, null, tool);
    }

    @GameTest
    public void leavesDropTheirSaplingByHand(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        for (Map.Entry<String, String> e : SOURCES.entrySet()) {
            final Block leaves = block(e.getKey());
            final Item sapling = item(e.getValue());
            if (leaves == null || sapling == null || sapling == Items.AIR) {
                failures.add(e.getKey() + " -> " + e.getValue() + ": not registered");
                continue;
            }
            final BlockState state = leaves.defaultBlockState();
            helper.setBlock(POS, state);
            int saplings = 0;
            for (int i = 0; i < TRIALS && saplings == 0; i++) {
                for (ItemStack s : drops(helper, state, ItemStack.EMPTY)) {
                    if (s.is(sapling)) saplings += s.getCount();
                }
            }
            if (saplings == 0) {
                failures.add(e.getKey() + " never dropped " + e.getValue() + " in " + TRIALS + " hand breaks");
            }
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "sapling drop regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void shearsDropTheLeavesAndNoSapling(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        for (Map.Entry<String, String> e : SOURCES.entrySet()) {
            final Block leaves = block(e.getKey());
            final Item sapling = item(e.getValue());
            final BlockState state = leaves.defaultBlockState();
            helper.setBlock(POS, state);
            final List<ItemStack> drops = drops(helper, state, new ItemStack(Items.SHEARS));
            if (drops.stream().noneMatch(s -> s.is(leaves.asItem())) || drops.stream().anyMatch(s -> s.is(sapling))) {
                failures.add(e.getKey() + " with shears should drop only itself, got " + drops);
            }
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "shears leaf-drop regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void everySaplingHasASourceBlock(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            final ResourceLocation id = entry.getKey().location();
            if (!"betterend".equals(id.getNamespace())) continue;
            final BlockState state = entry.getValue().defaultBlockState();
            final String path = id.getPath();
            if (state.is(SAPLINGS) && !SOURCES.containsValue(path)) {
                failures.add(path + " is a sapling, but no leaves block in this test drops it");
            }
            if (state.is(BlockTags.LEAVES) && !SOURCES.containsKey(path) && !LEAVES_WITHOUT_SAPLING.contains(path)) {
                failures.add(path + " is tagged leaves, but is neither a sapling source here nor listed as having none");
            }
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "sapling source coverage:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }
}
