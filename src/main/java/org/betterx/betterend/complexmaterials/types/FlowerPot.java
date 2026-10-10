package org.betterx.betterend.complexmaterials.types;

import de.ambertation.wover.block.api.render.BlockRenderTraits;
import org.betterx.betterend.blocks.FlowerPotBlock;
import static org.betterx.betterend.blocks.FlowerPotBlock.POT_LIGHT;
import org.betterx.betterend.complexmaterials.StoneMaterial;
import de.ambertation.wover.block.api.BlockDefinition;
import de.ambertation.wover.block.api.BlockRegistry;
import de.ambertation.wover.block.api.model.BlockModelBinding;
import de.ambertation.wover.block.api.trait.BlockRecipeTrait;
import de.ambertation.wover.block.api.trait.BlockTraitLookup;
import de.ambertation.wover.block.api.trait.BlockTraits;
import de.ambertation.wover.recipe.api.RecipeBuilder;
import de.ambertation.wover.recipe.api.RecipeMaterial;
import de.ambertation.wover.sets.api.blocks.BlockSet;
import de.ambertation.wover.sets.api.blocks.SlotFromDefinition;
import de.ambertation.wover.sets.api.blocks.SlotType;

import net.minecraft.tags.BlockTags;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlowerPot extends SlotFromDefinition {
    public static final FlowerPot SLOT = new FlowerPot();

    private FlowerPot() {
        super(StoneMaterial.FLOWER_POT);
    }

    @Override
    protected @Nullable BlockDefinition<?, ?> startBlockDefinition(
            @NotNull BlockRegistry registry,
            @NotNull BlockSet<?> set,
            @NotNull String name
    ) {
        return registry.defineDefaultBlock(
                name, (def) -> new FlowerPotBlock(def.getProperties())
        );
    }

    public static BlockDefinition<?, ?> asFlowerPot(BlockDefinition<?, ?> def) {
        return def.lightLevel(state -> state.getValue(POT_LIGHT) * 5)
                  .addTrait(BlockRenderTraits.RENDER_LAYER.cutout())
                  // Like vanilla's flower_pot. On 26.3 this tag is part of #minecraft:washed_away_by_fluids,
                  // so pots keep being washed away by water and lava as they were up to 26.2.
                  .addTags(BlockTags.FLOWER_POTS);
    }

    public static BlockRecipeTrait recipe(RecipeMaterial brick) {
        return BlockTraits.RECIPE.with(potRecipe(brick));
    }

    private static BlockRecipeTrait.RecipeFactory potRecipe(RecipeMaterial brick) {
        return (key, block, context) -> {
            RecipeBuilder
                    .crafting(key.identifier(), block)
                    .outputCount(3)
                    .shape("# #", " # ")
                    .addMaterial('#', brick)
                    .group("end_pots")
                    .build(context);
        };
    }

    @Override
    protected void addSlotSpecificDefinitions(BlockSet<?> set, BlockDefinition<?, ?> def) {
        super.addSlotSpecificDefinitions(set, def);
        asFlowerPot(def);
        def.addTrait(BlockTraits.LOOT_TABLE.dropSelf());
    }

    @Override
    protected BlockRecipeTrait buildRecipe(BlockSet<?> set, BlockTraitLookup traitLookup) {
        final BlockRecipeTrait.RecipeFactory potRecipe = potRecipe(set.recipeMaterial(SlotType.BRICK));
        return BlockTraits.RECIPE.with(
                (key, block, context) -> {
                    // Sets without a BRICK slot (the vanilla-backed stone sets) cannot name their matching vanilla
                    // brick here; a fallback would silently resolve to the source block and emit a second pot
                    // recipe - from full blocks - under the block's own ID. Those sets declare the recipe by hand
                    // instead (EndCraftingRecipesProvider), so skip rather than emit a wrong recipe.
                    if (set.getBlock(SlotType.BRICK) == null) return;

                    potRecipe.buildRecipe(key, block, context);
                });
    }

    @Override
    protected BlockModelBinding buildModel(BlockSet<?> set, BlockTraitLookup traitLookup) {
        return FlowerPotBlock.buildModel();
    }

    @Override
    protected void finalizeDefinitions(BlockSet<?> set, BlockDefinition<?, ?> def) {
        // Not a full cube (a flower pot), so it must not inherit the set material's sulfur cube archetype -
        // a cube renders what it swallowed as a block model, and a flower pot inside one reads as a bug.
        def.addTrait(BlockTraits.SULFUR_CUBE_ARCHETYPE.notSwallowable());
    }
}
