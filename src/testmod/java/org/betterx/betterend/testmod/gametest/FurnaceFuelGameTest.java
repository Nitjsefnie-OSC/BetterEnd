package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.block.EndMetalBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.number.ResolvableNumber;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression coverage for {@code charcoal_block} actually burning at its declared fuel time.
 * <p>
 * Per {@code org.betterx.bclib.api.v2.FuelComponents}' own file comment: the hook that turns a block's
 * {@code Fuel} interface into a real, usable furnace burn time used to be wired from a datagen-only
 * provider, so nothing in BCLib, BetterEnd or BetterNether ever actually got the burn time it declared -
 * a silent, total regression that only datagen output (never read at runtime) would have shown as
 * "working". This asserts the number the *game* will actually use, not the block's own declared
 * {@code getFuelTime()} - the two only agree if the registration hook is wired correctly.
 * <p>
 * <b>How this differs from the same test on 1.21.6 / 26.1 / 26.2.</b> Those branches could ask
 * {@code level.fuelValues().burnDuration(stack)} - one server-wide {@code FuelValues} table mapping an
 * item to an {@code int}. 26.3 deleted {@code FuelValues} and {@code Level#fuelValues()} outright.
 * A burn time is now the {@code minecraft:cooking_fuel} item component ({@link CookingFuel}), whose
 * {@code burnTime} is a {@link ResolvableNumber} - for everything vanilla and everything BCLib
 * registers, a {@code Reference} into the new {@code minecraft:number_provider} datapack registry -
 * and resolving it to a tick count needs a {@link LootContext}, because the provider is a
 * {@code ConditionalValue} that halves the duration for blast furnaces and smokers
 * ({@code minecraft:block/fast_cooking}).
 * <p>
 * So the single 26.2 assertion is reproduced here in the two halves 26.3 splits it into:
 * <ul>
 *     <li>{@link #charcoalBlockBurnsAtItsDeclaredFuelTime} resolves the component exactly the way
 *     {@code AbstractFurnaceBlockEntity#getBurnDuration} does - same {@code ResolvableNumber.getIntFromItem}
 *     call, against a real furnace's own {@code BaseContainerBlockEntity#getLootContext} - and asserts the
 *     resulting tick count, which is the direct analogue of the old {@code burnDuration(...) >= 16000}.</li>
 *     <li>{@link #charcoalBlockIsAcceptedAsFurnaceFuel} asserts the end-to-end consequence: a plain vanilla
 *     furnace accepts the block as fuel and lights. On 26.3 {@code AbstractFurnaceBlockEntity#isFuel} is
 *     literally {@code itemStack.has(DataComponents.COOKING_FUEL)}, so "is a fuel at all" became a separate,
 *     independently breakable fact from "burns for N ticks" - on 26.2 both fell out of the one table.</li>
 * </ul>
 * Nothing from the 26.2 test is dropped; the {@code >= 16000} number survives unchanged, and the extra
 * check exists only because 26.3 added a way for it to fail on its own.
 */
public class FurnaceFuelGameTest {
    private static final BlockPos FURNACE_POS = new BlockPos(1, 2, 1);

    /**
     * 16000 ticks - vanilla {@code coal_block}'s burn time, which {@code CharcoalBlock.getFuelTime()}
     * matches, and the value behind {@link NumberProviders#COOKING_TIME_COAL_BLOCK}.
     */
    private static final int EXPECTED_TICKS = 16000;

    private static AbstractFurnaceBlockEntity placeFurnace(GameTestHelper helper) {
        helper.setBlock(FURNACE_POS, Blocks.FURNACE);
        final BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(FURNACE_POS));
        if (!(be instanceof AbstractFurnaceBlockEntity furnace)) {
            throw helper.assertionException(Component.literal(
                    "minecraft:furnace did not create an AbstractFurnaceBlockEntity"
            ));
        }
        return furnace;
    }

    /**
     * A copy of {@code BaseContainerBlockEntity#getLootContext(ServerLevel)}, which is {@code protected}
     * and so cannot be called from a test in another package. Same parameter set
     * ({@link LootContextParamSets#CONTAINER_PROCESS}) and same four parameters, so the
     * {@code minecraft:block/fast_cooking} condition inside the cooking number provider sees exactly what
     * it sees during a real furnace tick.
     */
    private static LootContext furnaceLootContext(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
        return new LootContext.Builder(
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.BLOCK_STATE, furnace.getBlockState())
                        .withParameter(LootContextParams.BLOCK_ENTITY, furnace)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(furnace.getBlockPos()))
                        .withParameter(LootContextParams.CONTAINER, furnace)
                        .create(LootContextParamSets.CONTAINER_PROCESS)
        ).create(Optional.empty());
    }

    /**
     * The 26.3 form of the 26.2 assertion: the tick count the furnace will actually burn this item for.
     * <p>
     * Also pins down that the component is a reference to vanilla's 16000-tick provider rather than some
     * inline constant that happens to be large enough - that is what {@code FuelComponents}' lookup of
     * {@code getFuelTime()} against vanilla's providers is supposed to produce, and it is the same fact
     * {@code EndStoneSmelterBlockEntity#canUseAsFuel} depends on (its {@code LONG_BURNING_FUELS} set
     * matches on the provider key, so a constant-backed fuel would silently stop working in the smelter).
     */
    @GameTest
    public void charcoalBlockBurnsAtItsDeclaredFuelTime(GameTestHelper helper) {
        final AbstractFurnaceBlockEntity furnace = placeFurnace(helper);
        final ItemStack stack = new ItemStack(EndMetalBlocks.CHARCOAL_BLOCK);

        final CookingFuel fuel = stack.get(DataComponents.COOKING_FUEL);
        if (fuel == null) {
            throw helper.assertionException(Component.literal(
                    "charcoal_block carries no minecraft:cooking_fuel component - the block's declared fuel"
                            + " time is not reaching the item at all, so it is not a furnace fuel"
            ));
        }

        if (!(fuel.burnTime() instanceof ResolvableNumber.Reference reference)) {
            throw helper.assertionException(Component.literal(
                    "charcoal_block's cooking_fuel burnTime is " + fuel.burnTime()
                            + ", expected a reference to minecraft:cooking/time_coal_block"
            ));
        }
        if (!reference.key().equals(NumberProviders.COOKING_TIME_COAL_BLOCK)) {
            throw helper.assertionException(Component.literal(
                    "charcoal_block's cooking_fuel burnTime points at " + reference.key().identifier()
                            + ", expected " + NumberProviders.COOKING_TIME_COAL_BLOCK.identifier()
            ));
        }

        // Exactly what AbstractFurnaceBlockEntity#getBurnDuration computes.
        final int burnDuration = ResolvableNumber.getIntFromItem(
                stack,
                DataComponents.COOKING_FUEL,
                CookingFuel::burnTime,
                furnaceLootContext(helper.getLevel(), furnace),
                0
        );

        if (burnDuration < EXPECTED_TICKS) {
            throw helper.assertionException(Component.literal(
                    "the furnace's own burn-duration resolution for charcoal_block returned " + burnDuration
                            + ", expected at least " + EXPECTED_TICKS + " - the block's declared fuel time is"
                            + " not reaching the actual cooking_fuel component"
            ));
        }
        helper.succeed();
    }

    /**
     * End-to-end: a plain vanilla furnace holding charcoal_block as fuel and a smeltable input lights up.
     * <p>
     * New on 26.3 only because {@code isFuel} became {@code has(DataComponents.COOKING_FUEL)} - an item can
     * now resolve a burn duration and still be refused, or vice versa, so the acceptance half is worth its
     * own assertion. Slot indices are literals because {@code AbstractFurnaceBlockEntity.SLOT_INPUT} /
     * {@code SLOT_FUEL} are {@code protected}.
     */
    @GameTest(maxTicks = 100)
    public void charcoalBlockIsAcceptedAsFurnaceFuel(GameTestHelper helper) {
        final AbstractFurnaceBlockEntity furnace = placeFurnace(helper);

        // A stack of many, so the test never depends on what happens when the last fuel item is consumed.
        furnace.setItem(0, new ItemStack(Items.IRON_ORE));                                 // SLOT_INPUT
        furnace.setItem(1, new ItemStack(EndMetalBlocks.CHARCOAL_BLOCK.asItem(), 64));     // SLOT_FUEL

        if (!furnace.canPlaceItem(1, new ItemStack(EndMetalBlocks.CHARCOAL_BLOCK))) {
            throw helper.assertionException(Component.literal(
                    "a vanilla furnace refuses charcoal_block in its fuel slot - it carries no"
                            + " minecraft:cooking_fuel component, which is all AbstractFurnaceBlockEntity#isFuel"
                            + " checks on 26.3"
            ));
        }

        helper.startSequence()
              .thenWaitUntil(() -> {
                  final var state = helper.getLevel().getBlockState(helper.absolutePos(FURNACE_POS));
                  if (!state.getValue(AbstractFurnaceBlock.LIT)) {
                      throw helper.assertionException(Component.literal(
                              "the furnace never lit while holding charcoal_block as its only fuel"
                      ));
                  }
              })
              .thenSucceed();
    }
}
