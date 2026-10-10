package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.blocks.entities.EndStoneSmelterBlockEntity;
import org.betterx.betterend.client.gui.EndStoneSmelterMenu;
import org.betterx.betterend.registry.block.EndFunctionalBlocks;
import org.betterx.betterend.registry.block.EndMetalBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The End Stone Smelter's fuel list ({@link EndStoneSmelterBlockEntity#smelterFuels}) - what JEI's
 * "Alloying Fuel" category shows - against what the smelter actually does.
 * <p>
 * The list used to be {@code availableFuels()}, which only held fuels added through {@code registerFuel}.
 * Nothing ever called that, so the JEI category was always empty while the smelter happily burned blaze
 * rods, dried kelp blocks, coal blocks and lava buckets. These tests pin down that the list is not empty,
 * that it holds exactly the long-burning fuels, and that every listed item really lights the smelter for
 * the listed number of ticks.
 */
public class EndStoneSmelterFuelGameTest {
    private static final int ROW_LENGTH = 7;

    private static BlockPos slot(int index) {
        return new BlockPos(index % ROW_LENGTH, 2, 1 + 2 * (index / ROW_LENGTH));
    }

    private static EndStoneSmelterBlockEntity smelter(GameTestHelper helper, BlockPos pos) {
        final BlockEntity be = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (!(be instanceof EndStoneSmelterBlockEntity smelter)) {
            throw helper.assertionException(Component.literal(
                    "end_stone_smelter did not create an EndStoneSmelterBlockEntity at " + pos
            ));
        }
        return smelter;
    }

    private static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static void failIfAny(GameTestHelper helper, String headline, List<String> failures) {
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(headline + ":\n - " + String.join("\n - ", failures)));
        }
    }

    /**
     * The list holds the four vanilla long-burning fuels (with their vanilla burn times) and BetterEnd's
     * charcoal block, every listed item is accepted in the fuel slot, and ordinary fuels such as coal are
     * neither listed nor accepted.
     */
    @GameTest
    public void fuelListHoldsTheLongBurningFuels(GameTestHelper helper) {
        final Map<Item, Integer> fuels = EndStoneSmelterBlockEntity.smelterFuels(helper.getLevel().fuelValues());
        final List<String> failures = new ArrayList<>();
        if (fuels.isEmpty()) {
            failures.add("smelterFuels() is empty - the JEI Alloying Fuel category would show nothing");
        }

        final Map<Item, Integer> expected = Map.of(
                Items.BLAZE_ROD, 2400,
                Items.DRIED_KELP_BLOCK, 4001,
                Items.COAL_BLOCK, 16000,
                EndMetalBlocks.CHARCOAL_BLOCK.asItem(), 16000,
                Items.LAVA_BUCKET, 20000
        );
        expected.forEach((item, ticks) -> {
            final Integer listed = fuels.get(item);
            if (listed == null) failures.add(id(item) + " is not listed");
            else if (listed.intValue() != ticks) failures.add(id(item) + " is listed with " + listed + " ticks, expected " + ticks);
        });

        helper.setBlock(slot(0), EndFunctionalBlocks.END_STONE_SMELTER);
        final EndStoneSmelterBlockEntity smelter = smelter(helper, slot(0));
        fuels.keySet().forEach(item -> {
            final ItemStack stack = new ItemStack(item);
            if (!EndStoneSmelterBlockEntity.canUseAsFuel(helper.getLevel(), stack)) failures.add(id(item) + " is listed but canUseAsFuel says no");
            if (!smelter.canPlaceItem(EndStoneSmelterMenu.FUEL_SLOT, stack)) failures.add(id(item) + " is listed but cannot be put into the fuel slot");
        });

        for (Item item : List.of(Items.COAL, Items.CHARCOAL, Items.OAK_LOG, Items.STICK)) {
            if (fuels.containsKey(item)) failures.add(id(item) + " is listed, but it burns for less than 2000 ticks");
            if (smelter.canPlaceItem(EndStoneSmelterMenu.FUEL_SLOT, new ItemStack(item))) failures.add(id(item) + " is accepted in the fuel slot");
        }

        failIfAny(helper, "End Stone Smelter fuel list", failures);
        helper.succeed();
    }

    /**
     * Every listed fuel, as the only fuel item of its own smelter with an alloying recipe ready, lights the
     * smelter, is consumed, and burns for exactly the number of ticks the list (and so JEI) shows.
     */
    @GameTest(maxTicks = 100)
    public void everyListedFuelBurnsForItsListedTime(GameTestHelper helper) {
        final List<Map.Entry<Item, Integer>> fuels = List.copyOf(EndStoneSmelterBlockEntity.smelterFuels(helper.getLevel().fuelValues()).entrySet());
        if (fuels.isEmpty()) {
            throw helper.assertionException(Component.literal("smelterFuels() is empty"));
        }
        if (fuels.size() > 4 * ROW_LENGTH) {
            throw helper.assertionException(Component.literal("more fuels than the test has room for: " + fuels.size()));
        }

        final List<EndStoneSmelterBlockEntity> smelters = new ArrayList<>();
        for (int i = 0; i < fuels.size(); i++) {
            helper.setBlock(slot(i), EndFunctionalBlocks.END_STONE_SMELTER);
            final EndStoneSmelterBlockEntity smelter = smelter(helper, slot(i));
            // Two iron ores match the additional_iron alloying recipe, whose burn-time factor is 1.
            smelter.setItem(EndStoneSmelterMenu.INGREDIENT_SLOT_A, new ItemStack(Items.IRON_ORE));
            smelter.setItem(EndStoneSmelterMenu.INGREDIENT_SLOT_B, new ItemStack(Items.IRON_ORE));
            smelter.setItem(EndStoneSmelterMenu.FUEL_SLOT, new ItemStack(fuels.get(i).getKey()));
            smelters.add(smelter);
        }

        helper.startSequence()
              .thenIdle(5)
              .thenExecute(() -> {
                  final List<String> failures = new ArrayList<>();
                  for (int i = 0; i < fuels.size(); i++) {
                      final Item item = fuels.get(i).getKey();
                      final int listed = fuels.get(i).getValue();
                      final EndStoneSmelterBlockEntity smelter = smelters.get(i);

                      final var state = helper.getLevel().getBlockState(helper.absolutePos(slot(i)));
                      if (!state.getValue(AbstractFurnaceBlock.LIT)) {
                          failures.add(id(item) + " did not light the smelter");
                          continue;
                      }
                      final ItemStack left = smelter.getItem(EndStoneSmelterMenu.FUEL_SLOT);
                      if (left.is(item)) {
                          failures.add(id(item) + " was not consumed");
                      }
                      final CompoundTag tag = smelter.saveWithoutMetadata(helper.getLevel().registryAccess());
                      final int burnTicks = Short.toUnsignedInt(tag.getShortOr("FuelTime", (short) 0));
                      if (burnTicks != listed) {
                          failures.add(id(item) + " burns for " + burnTicks + " ticks, but the fuel list shows " + listed);
                      }
                  }
                  failIfAny(helper, "End Stone Smelter fuels", failures);
              })
              .thenSucceed();
    }
}
