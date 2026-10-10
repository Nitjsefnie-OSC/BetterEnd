package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.BetterEnd;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Regression coverage for #601: the BetterEnd spears carried a {@code minecraft:tool} component whose rules were
 * built from a {@code null} mining tag. The resulting {@link HolderSet.Named} has a {@code null} key, so
 * {@link HolderSet#unwrapKey()} throws ({@code Optional.of(null)}) - which crashed world loading as soon as
 * another mod (Golden Age Combat) inspected every item's tool rules.
 */
public class ToolComponentGameTest {
    @GameTest
    public void toolRulesNeverUseUnnamedTagSets(GameTestHelper helper) {
        final List<String> broken = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            final var id = BuiltInRegistries.ITEM.getKey(item);
            if (!BetterEnd.MOD_ID.equals(id.getNamespace())) continue;

            final Tool tool = item.components().get(DataComponents.TOOL);
            if (tool == null) continue;
            for (Tool.Rule rule : tool.rules()) {
                // Direct sets (like vanilla's sword cobweb rule) are fine; a tag-backed set must name its tag.
                if (rule.blocks() instanceof HolderSet.Named<?> named && named.key() == null) {
                    broken.add(id.toString());
                }
            }
        }
        if (!broken.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "minecraft:tool rules backed by a tag set without a tag key: " + broken
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void spearsHaveNoToolComponentLikeVanilla(GameTestHelper helper) {
        if (Items.IRON_SPEAR.components().has(DataComponents.TOOL)) {
            throw helper.assertionException(Component.literal("vanilla iron_spear unexpectedly has a tool component"));
        }
        final List<String> broken = new ArrayList<>();
        int spears = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            final var id = BuiltInRegistries.ITEM.getKey(item);
            if (!BetterEnd.MOD_ID.equals(id.getNamespace()) || !id.getPath().endsWith("_spear")) continue;
            spears++;
            if (item.components().has(DataComponents.TOOL)) broken.add(id + " (has tool)");

            // Item.Properties#spear sets the attack speed from the attack duration (1 / duration - 4, always
            // negative); a later tool(...) call used to overwrite it with the digger baseline of 0.
            final var modifiers = item.components().get(DataComponents.ATTRIBUTE_MODIFIERS);
            final boolean slowedDown = modifiers != null && modifiers
                    .modifiers()
                    .stream()
                    .anyMatch(e -> e.attribute().is(Attributes.ATTACK_SPEED)
                            && e.modifier().is(Item.BASE_ATTACK_SPEED_ID)
                            && e.modifier().amount() < 0);
            if (!slowedDown) broken.add(id + " (attack speed not from spear tuning)");
        }
        if (spears < 3) {
            throw helper.assertionException(Component.literal("expected at least 3 BetterEnd spears, found " + spears));
        }
        if (!broken.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "spears not configured like vanilla spears: " + broken
            ));
        }
        helper.succeed();
    }
}
