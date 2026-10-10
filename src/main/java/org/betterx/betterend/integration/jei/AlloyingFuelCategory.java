package org.betterx.betterend.integration.jei;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.block.EndFunctionalBlocks;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import java.text.NumberFormat;
import java.util.List;

/**
 * The End Stone Smelter's fuels ({@code EndStoneSmelterBlockEntity#smelterFuels()}): the smelter only burns
 * long-burning fuels (blaze rods, dried kelp blocks, coal and charcoal blocks, lava buckets and anything
 * registered through {@code registerFuel}), not every item JEI's vanilla fuel category lists.
 */
public class AlloyingFuelCategory implements IRecipeCategory<FuelEntry> {
    public static final IRecipeType<FuelEntry> TYPE = IRecipeType.create(BetterEnd.C.mk("alloying_fuel"), FuelEntry.class);

    // Same layout as JEI's own fuel categories: flame on top of the fuel slot, text to the right.
    private static final int TEXT_X = 20;
    private static final int HEIGHT = 34;

    private final IDrawable icon;
    private final int width;

    public AlloyingFuelCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(EndFunctionalBlocks.END_STONE_SMELTER);
        this.width = TEXT_X + Minecraft.getInstance().font.width(burnTimeText(99_999)) + 2;
    }

    private static Component burnTimeText(int ticks) {
        final NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(false);
        return Component.translatable("category.betterend.jei.alloying_fuel.burn_time", format.format(ticks / 20.0));
    }

    @Override
    public IRecipeType<FuelEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.betterend.jei.alloying_fuel");
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FuelEntry entry, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 17)
               .setStandardSlotBackground()
               .add(new ItemStack(entry.item()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, FuelEntry entry, IFocusGroup focuses) {
        builder.addAnimatedRecipeFlame(entry.burnTimeTicks()).setPosition(1, 0);
        builder.addText(List.of(burnTimeText(entry.burnTimeTicks())), width - TEXT_X, HEIGHT)
               .setTextAlignment(VerticalAlignment.CENTER)
               .setColor(0xFF808080)
               .setPosition(TEXT_X, 0);
    }
}
