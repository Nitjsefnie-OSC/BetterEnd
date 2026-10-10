package org.betterx.betterend.testmod.gametest;

import org.betterx.bclib.blocks.LeveledAnvilBlock;
import org.betterx.bclib.recipes.AlloyingRecipe;
import org.betterx.bclib.recipes.AnvilRecipe;
import org.betterx.bclib.recipes.RecipeViewerStations;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.block.EndFunctionalBlocks;
import org.betterx.betterend.registry.item.EndEquipmentItems;
import de.ambertation.wover.recipe.api.SyncedRecipes;
import de.ambertation.wover.test.api.gametest.GameTestAssertions;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The data behind BCLib's JEI categories (alloying, anvil smithing): every BetterEnd anvil recipe must
 * list at least one hammer and one anvil that can actually craft it, the stations must be the ones the
 * anvil menu / smelter accept, and the input counts the category shows must come from the recipe.
 */
public class RecipeViewerStationsGameTest {
    @GameTest
    public void everyAnvilRecipeHasAHammerAndAnAnvil(GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Collection<RecipeHolder<AnvilRecipe>> recipes = SyncedRecipes.allOfType(level, AnvilRecipe.TYPE);
        final List<String> failures = new ArrayList<>();
        if (recipes.size() < 20) failures.add("only " + recipes.size() + " bclib:smithing recipes found");

        boolean sawMultiInput = false;
        for (RecipeHolder<AnvilRecipe> holder : recipes) {
            final AnvilRecipe recipe = holder.value();
            final List<Item> hammers = RecipeViewerStations.hammersFor(recipe, level.registryAccess());
            if (hammers.isEmpty()) failures.add(holder.id().location() + ": no hammer in " + recipe.getAllowedTools());
            for (Item hammer : hammers) {
                if (!hammer.getDefaultInstance().is(recipe.getAllowedTools())) {
                    failures.add(holder.id().location() + ": " + hammer + " is not in " + recipe.getAllowedTools());
                }
            }
            final List<Block> anvils = RecipeViewerStations.anvilsFor(recipe.getAnvilLevel());
            if (anvils.isEmpty()) failures.add(holder.id().location() + ": no anvil of level " + recipe.getAnvilLevel());
            for (Block anvil : anvils) {
                if (LeveledAnvilBlock.getAnvilCraftingLevel(anvil) < recipe.getAnvilLevel()) {
                    failures.add(holder.id().location() + ": " + anvil + " is below level " + recipe.getAnvilLevel());
                }
            }
            if (recipe.getInputCount() > 1) sawMultiInput = true;
        }
        if (!sawMultiInput) failures.add("no anvil recipe consumes more than one item - the count display is untested");

        GameTestAssertions.failIfAny(helper, failures.size() + " anvil recipe(s) not craftable as JEI shows them", failures);
        helper.succeed();
    }

    @GameTest
    public void anvilAndHammerTiersMatchTheMenu(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();

        final List<Block> levelOne = RecipeViewerStations.anvilsFor(1);
        if (!levelOne.contains(Blocks.ANVIL)) failures.add("vanilla anvil missing for level I: " + levelOne);
        if (!levelOne.contains(Blocks.CHIPPED_ANVIL)) failures.add("chipped anvil missing for level I: " + levelOne);

        final List<Block> levelTwo = RecipeViewerStations.anvilsFor(2);
        if (levelTwo.contains(Blocks.ANVIL)) failures.add("vanilla anvil listed for level II: " + levelTwo);
        if (!levelTwo.contains(EndFunctionalBlocks.AETERNIUM_ANVIL)) failures.add("aeternium anvil missing for level II: " + levelTwo);

        // terminite hammer head: 3 ingots, diamond-tier hammer, level III anvil
        final ServerLevel level = helper.getLevel();
        final RecipeHolder<AnvilRecipe> head = SyncedRecipes
                .allOfType(level, AnvilRecipe.TYPE)
                .stream()
                .filter(r -> r.id().location().equals(BetterEnd.C.mk("terminite_hammer_head")))
                .findFirst()
                .orElse(null);
        if (head == null) {
            failures.add("betterend:terminite_hammer_head recipe not found");
        } else {
            if (head.value().getInputCount() != 3) {
                failures.add("terminite_hammer_head consumes " + head.value().getInputCount() + " ingots, expected 3");
            }
            final List<Item> hammers = RecipeViewerStations.hammersFor(head.value(), level.registryAccess());
            if (!hammers.contains(EndEquipmentItems.DIAMOND_HAMMER)) failures.add("diamond hammer missing: " + hammers);
            if (hammers.contains(EndEquipmentItems.IRON_HAMMER)) failures.add("iron hammer listed: " + hammers);
            final List<Block> anvils = RecipeViewerStations.anvilsFor(head.value().getAnvilLevel());
            if (anvils.contains(Blocks.ANVIL)) failures.add("vanilla anvil listed: " + anvils);
        }

        GameTestAssertions.failIfAny(helper, "anvil/hammer tiers differ from the anvil menu", failures);
        helper.succeed();
    }

    @GameTest
    public void endStoneSmelterIsTheAlloyingWorkstation(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        final List<Block> workstations = RecipeViewerStations.alloyingWorkstations();
        if (!workstations.contains(EndFunctionalBlocks.END_STONE_SMELTER)) {
            failures.add("End Stone Smelter is not an alloying workstation: " + workstations);
        }
        final int recipes = SyncedRecipes.allOfType(helper.getLevel(), AlloyingRecipe.TYPE).size();
        if (recipes == 0) failures.add("no bclib:alloying recipes loaded");

        GameTestAssertions.failIfAny(helper, "alloying category would be empty or have no catalyst", failures);
        helper.succeed();
    }
}
