package org.betterx.betterend.complexmaterials.types;


import de.ambertation.wover.block.api.render.BlockRenderTraits;
import org.betterx.betterend.registry.item.EndResourceItems;
import org.betterx.betterend.blocks.ChandelierBlock;
import org.betterx.betterend.complexmaterials.MetalMaterial;
import org.betterx.betterend.registry.EndItems;
import de.ambertation.wover.block.api.BlockDefinition;
import de.ambertation.wover.block.api.BlockRegistry;
import de.ambertation.wover.block.api.model.BlockModelBinding;
import de.ambertation.wover.block.api.trait.BlockRecipeTrait;
import de.ambertation.wover.block.api.trait.BlockTraitLookup;
import de.ambertation.wover.block.api.trait.BlockTraits;
import de.ambertation.wover.recipe.api.RecipeBuilder;
import de.ambertation.wover.sets.api.blocks.BlockSet;
import de.ambertation.wover.sets.api.blocks.SlotFromDefinition;

import net.minecraft.tags.BlockTags;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Chandelier extends SlotFromDefinition {
    public static final Chandelier SLOT = new Chandelier();

    private Chandelier() {
        super(MetalMaterial.CHANDELIER);
    }

    @Override
    protected @Nullable BlockDefinition<?, ?> startBlockDefinition(
            @NotNull BlockRegistry registry,
            @NotNull BlockSet<?> set,
            @NotNull String name
    ) {
        return registry.defineDefaultBlock(name, (def) -> new ChandelierBlock(def.getProperties()));
    }

    @Override
    protected void addSlotSpecificDefinitions(BlockSet<?> set, BlockDefinition<?, ?> def) {
        super.addSlotSpecificDefinitions(set, def);
        def.addTrait(BlockRenderTraits.RENDER_LAYER.cutout());
        def.lightLevel((bs) -> 15)
           .noCollission()
           .noOcclusion()
           .isValidSpawn((state, level, pos, entity) -> false)
           .requiresCorrectToolForDrops()
           // Like vanilla's torches and wall torches (and BetterNether's willow torch): a non-colliding
           // light on a floor, wall or ceiling is washed away by water and lava. Up to 26.2 that followed
           // from noCollission() (no blocksMotion()); 26.3 only lets fluids flow into blocks in this tag.
           // Vanilla's lanterns stay out, but they have a collision shape.
           .addTags(BlockTags.WASHED_AWAY_BY_FLUIDS);
        def.addTrait(BlockTraits.LOOT_TABLE.dropSelf());
    }

    @Override
    protected BlockRecipeTrait buildRecipe(BlockSet<?> set, BlockTraitLookup traitLookup) {
        return BlockTraits.RECIPE.with(
                (key, block, context) -> {
                    RecipeBuilder
                            .crafting(key.identifier(), block)
                            .shape("I#I", " # ")
                            .addMaterial('#', set.getItem(MetalMaterial.INGOT))
                            .addMaterial('I', EndResourceItems.LUMECORN_ROD)
                            .group("end_metal_chandelier")
                            .build(context);
                });
    }

    @Override
    protected BlockModelBinding buildModel(BlockSet<?> set, BlockTraitLookup traitLookup) {
        return ChandelierBlock.buildModel(set, traitLookup);
    }

    @Override
    protected void finalizeDefinitions(BlockSet<?> set, BlockDefinition<?, ?> def) {
        // Not a full cube (a chandelier), so it must not inherit the set material's sulfur cube archetype -
        // a cube renders what it swallowed as a block model, and a chandelier inside one reads as a bug.
        def.addTrait(BlockTraits.SULFUR_CUBE_ARCHETYPE.notSwallowable());
    }
}
