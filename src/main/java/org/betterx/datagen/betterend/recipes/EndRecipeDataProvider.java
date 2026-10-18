package org.betterx.datagen.betterend.recipes;

import org.betterx.bclib.api.v3.datagen.RecipeDataProvider;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.integration.Integrations;
import org.betterx.betterend.recipe.*;

import net.minecraft.data.recipes.FinishedRecipe;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;

import java.util.List;
import java.util.function.Consumer;

public class EndRecipeDataProvider extends RecipeDataProvider {
    public EndRecipeDataProvider(FabricDataOutput output) {
        super(List.of(BetterEnd.MOD_ID), output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> exporter) {
        // The guide book item only exists when Patchouli is installed, so its recipe (and recipe advancement)
        // must only be loaded in that case - otherwise every datapack reload logs a parsing error.
        final Consumer<FinishedRecipe> patchouliExporter = withConditions(
                exporter,
                DefaultResourceConditions.allModsLoaded("patchouli")
        );
        super.buildRecipes(recipe -> {
            if (Integrations.GUIDE_BOOK_RECIPE.equals(recipe.getId())) patchouliExporter.accept(recipe);
            else exporter.accept(recipe);
        });
    }

    public static void buildRecipes() {
        CraftingRecipes.register();
        FurnaceRecipes.register();
        AlloyingRecipes.register();
        AnvilRecipes.register();
        SmithingRecipes.register();
        InfusionRecipes.register();
    }
}
