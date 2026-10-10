package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.item.EndFoodItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.UseRemainder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * A food hands back a container after eating exactly when one went into making it (issue #544).
 * <p>
 * On 1.21.1 {@code bolux_mushroom_cooked} and {@code chorus_mushroom_cooked} were registered with
 * {@code Foods.MUSHROOM_STEW}, whose {@code FoodProperties} still carried the stew's bowl conversion,
 * so eating them returned a bowl although they are just smoked/campfire-cooked mushrooms. Since 1.21.2
 * the conversion lives in {@code Item.Properties#usingConvertsTo} ({@link DataComponents#USE_REMAINDER})
 * instead, so copying the stew's food stats no longer copies the bowl - this pins that down.
 * <p>
 * The other direction: {@code umbrella_cluster_juice} is filled by using a glass bottle on an umbrella
 * tree cluster, so drinking it gives the bottle back, like vanilla's honey bottle and potions.
 */
public class FoodUseRemainderGameTest {
    @GameTest
    public void containerIsReturnedExactlyWhenOneWasConsumed(GameTestHelper helper) {
        final Map<String, Item> expected = new LinkedHashMap<>();
        final Map<String, Item> foods = new LinkedHashMap<>();
        foods.put("shadow_berry_raw", EndFoodItems.SHADOW_BERRY_RAW);
        foods.put("shadow_berry_cooked", EndFoodItems.SHADOW_BERRY_COOKED);
        foods.put("end_fish_raw", EndFoodItems.END_FISH_RAW);
        foods.put("end_fish_cooked", EndFoodItems.END_FISH_COOKED);
        foods.put("sweet_berry_jelly", EndFoodItems.SWEET_BERRY_JELLY);
        foods.put("shadow_berry_jelly", EndFoodItems.SHADOW_BERRY_JELLY);
        foods.put("blossom_berry_jelly", EndFoodItems.BLOSSOM_BERRY_JELLY);
        foods.put("blossom_berry", EndFoodItems.BLOSSOM_BERRY);
        foods.put("amber_root_raw", EndFoodItems.AMBER_ROOT_RAW);
        foods.put("chorus_mushroom_raw", EndFoodItems.CHORUS_MUSHROOM_RAW);
        foods.put("chorus_mushroom_cooked", EndFoodItems.CHORUS_MUSHROOM_COOKED);
        foods.put("bolux_mushroom_cooked", EndFoodItems.BOLUX_MUSHROOM_COOKED);
        foods.put("cave_pumpkin_pie", EndFoodItems.CAVE_PUMPKIN_PIE);
        foods.put("umbrella_cluster_juice", EndFoodItems.UMBRELLA_CLUSTER_JUICE);
        expected.put("umbrella_cluster_juice", Items.GLASS_BOTTLE);

        final List<String> failures = new ArrayList<>();
        foods.forEach((name, item) -> {
            final UseRemainder remainder = new ItemStack(item).get(DataComponents.USE_REMAINDER);
            final Item actual = remainder == null ? null : remainder.convertInto().create().getItem();
            final Item want = expected.get(name);
            if (actual != want) {
                failures.add(name + " returns " + (actual == null ? "nothing" : actual)
                        + " after eating, expected " + (want == null ? "nothing" : want));
            }
        });

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "food container mismatch:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }
}
