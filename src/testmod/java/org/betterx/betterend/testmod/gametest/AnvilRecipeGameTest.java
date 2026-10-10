package org.betterx.betterend.testmod.gametest;

import org.betterx.bclib.blocks.LeveledAnvilBlock;
import org.betterx.bclib.items.BaseAnvilItem;
import org.betterx.bclib.util.BCLDataComponents;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.blocks.AeterniumAnvil;
import org.betterx.betterend.complexmaterials.MetalMaterial;
import org.betterx.betterend.registry.block.EndMetalBlocks;
import org.betterx.betterend.registry.block.EndFunctionalBlocks;
import org.betterx.betterend.registry.item.EndEquipmentItems;
import org.betterx.betterend.registry.item.EndResourceItems;
import org.betterx.bclib.interfaces.AnvilScreenHandlerExtended;
import de.ambertation.wover.test.api.gametest.MockPlayers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Covers anvil tier gating and per-anvil durability, at the level that actually drives recipe/repair
 * eligibility ({@code LeveledAnvilBlock.getAnvilCraftingLevel}/{@code canHandle}), rather than
 * simulating a full anvil-use/fall-damage sequence to observe the same numbers indirectly.
 * <p>
 * The vanilla-anvil-is-tier-1-not-tier-0 offset is easy to get backwards: {@code getAnvilCraftingLevel}
 * treats {@code Blocks.ANVIL}/{@code CHIPPED_ANVIL}/{@code DAMAGED_ANVIL} as
 * {@code LegacyTiers.IRON.level - 1} (2 - 1 = 1), not 0.
 */
public class AnvilRecipeGameTest {
    @GameTest
    public void vanillaAnvilIsTierOneNotTierZero(GameTestHelper helper) {
        final int level = LeveledAnvilBlock.getAnvilCraftingLevel(Blocks.ANVIL);
        if (level != 1) {
            throw helper.assertionException(Component.literal(
                    "Blocks.ANVIL's crafting level is " + level + ", expected 1 (LegacyTiers.IRON.level - 1)"
            ));
        }
        helper.succeed();
    }

    @GameTest
    public void aeterniumAnvilOutranksVanillaAndHasTwelveDurability(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();

        final int aeterniumLevel = LeveledAnvilBlock.getAnvilCraftingLevel(EndFunctionalBlocks.AETERNIUM_ANVIL);
        if (aeterniumLevel != 5) {
            failures.add("aeternium_anvil's crafting level is " + aeterniumLevel + ", expected 5");
        }
        if (!LeveledAnvilBlock.canHandle(EndFunctionalBlocks.AETERNIUM_ANVIL, LeveledAnvilBlock.getAnvilCraftingLevel(Blocks.ANVIL))) {
            failures.add("aeternium_anvil cannot handle recipes a vanilla anvil can - tier gating is inverted");
        }

        final int durability = ((AeterniumAnvil) EndFunctionalBlocks.AETERNIUM_ANVIL).getMaxDurability();
        if (durability != 12) {
            failures.add("aeternium_anvil's max durability is " + durability + ", expected 12 (BCLib default is 5)");
        }

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Aeternium anvil regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    private static final BlockPos ANVIL = new BlockPos(1, 1, 1);
    private static final BlockPos PLAYER = new BlockPos(2, 1, 1);
    // clear of the block placed above ANVIL, so the player does not obstruct the placement
    private static final BlockPos PLACER = new BlockPos(3, 1, 1);
    private static final int INGOTS = 4;

    /**
     * Issue #534: with a diamond hammer only the recipes whose allowed tools include diamond hammers
     * (on this version just the sword blade - there are no spears) may match.
     */
    @GameTest
    public void diamondHammerOnlyMakesSwordBlade(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, PLAYER);
        final AnvilMenu menu = openAnvil(helper, player, EndFunctionalBlocks.AETERNIUM_ANVIL, EndEquipmentItems.DIAMOND_HAMMER);
        final Set<Item> expected = Set.of(
                EndEquipmentItems.AETERNIUM_SET.swordBlade
        );
        final Set<Item> seen = cycle(helper, player, menu, expected.size());
        if (!seen.equals(expected)) {
            throw helper.assertionException(Component.literal(
                    "Diamond hammer + aeternium ingot cycled through " + seen + ", expected " + expected
            ));
        }
        helper.succeed();
    }

    /**
     * Issue #534: a netherite hammer unlocks every aeternium part, and cycling reaches all of them.
     */
    @GameTest
    public void netheriteHammerCyclesAllAeterniumParts(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, PLAYER);
        final AnvilMenu menu = openAnvil(helper, player, EndFunctionalBlocks.AETERNIUM_ANVIL, EndEquipmentItems.NETHERITE_HAMMER);
        final var set = EndEquipmentItems.AETERNIUM_SET;
        final Set<Item> expected = Set.of(
                set.axeHead, set.pickaxeHead, set.shovelHead, set.hoeHead,
                set.hammerHead, set.swordBlade, set.forgedPlate
        );
        final Set<Item> seen = cycle(helper, player, menu, expected.size());
        if (!seen.equals(expected)) {
            throw helper.assertionException(Component.literal(
                    "Netherite hammer + aeternium ingot cycled through " + seen + ", expected " + expected
            ));
        }
        helper.succeed();
    }

    /**
     * Issues #386/#394: taking the result on the (dedicated) server crashed. Taking it must hand out the
     * part, consume one ingot and damage the hammer.
     */
    @GameTest
    public void takingAnvilResultConsumesIngotAndDamagesHammer(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, PLAYER);
        final AnvilMenu menu = openAnvil(helper, player, EndFunctionalBlocks.AETERNIUM_ANVIL, EndEquipmentItems.NETHERITE_HAMMER);
        final ItemStack expected = menu.getSlot(2).getItem().copy();
        if (expected.isEmpty()) {
            throw helper.assertionException(Component.literal("No anvil result for netherite hammer + aeternium ingot"));
        }
        menu.clicked(2, 0, ClickType.PICKUP, player);

        final List<String> failures = new ArrayList<>();
        if (!ItemStack.isSameItem(menu.getCarried(), expected)) {
            failures.add("carried " + menu.getCarried() + " after taking the result, expected " + expected);
        }
        final int ingots = menu.getSlot(0).getItem().getCount();
        if (ingots != INGOTS - 1) {
            failures.add("ingot count is " + ingots + " after crafting, expected " + (INGOTS - 1));
        }
        final int damage = menu.getSlot(1).getItem().getDamageValue();
        if (damage != 6) {
            failures.add("hammer damage is " + damage + " after crafting, expected 6");
        }
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Anvil take regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    /**
     * Issues #421/#429: putting a single non-hammer item into an anvil threw an NPE ("material is null")
     * while matching anvil recipes. It must fall through to vanilla behaviour.
     */
    @GameTest
    public void singleItemInVanillaAnvilDoesNotMatchAnvilRecipes(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, PLAYER);
        final AnvilMenu menu = openAnvil(helper, player, Blocks.ANVIL, null);
        final List<String> failures = new ArrayList<>();

        menu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
        int count = ((AnvilScreenHandlerExtended) menu).bcl_getRecipeCount();
        if (count != 0) failures.add("a lone iron sword matched " + count + " anvil recipes");

        menu.getSlot(0).set(ItemStack.EMPTY);
        menu.getSlot(1).set(new ItemStack(EndEquipmentItems.DIAMOND_HAMMER));
        count = ((AnvilScreenHandlerExtended) menu).bcl_getRecipeCount();
        if (count != 0) failures.add("a lone hammer matched " + count + " anvil recipes");

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Anvil single-item regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    /**
     * Issue #586: ender dust has no crafting-table recipe; it is ground on an anvil (pearl or shard plus an
     * iron-tier hammer). The recipe needs anvil level 2, so a thallasium anvil works and a vanilla anvil
     * (level 1) does not.
     */
    @GameTest
    public void enderPearlAndShardGrindToEnderDust(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, PLAYER);
        final Block thallasiumAnvil = EndMetalBlocks.THALLASIUM.getBlock(MetalMaterial.ANVIL);
        final List<String> failures = new ArrayList<>();

        for (Item input : List.of(Items.ENDER_PEARL, EndResourceItems.ENDER_SHARD)) {
            AnvilMenu menu = openAnvil(helper, player, thallasiumAnvil, null);
            menu.getSlot(0).set(new ItemStack(input));
            menu.getSlot(1).set(new ItemStack(EndEquipmentItems.IRON_HAMMER));
            final ItemStack result = menu.getSlot(2).getItem();
            if (!result.is(EndResourceItems.ENDER_DUST)) {
                failures.add("thallasium anvil + iron hammer + " + input + " gave " + result + ", expected ender_dust");
            }

            menu = openAnvil(helper, player, Blocks.ANVIL, null);
            menu.getSlot(0).set(new ItemStack(input));
            menu.getSlot(1).set(new ItemStack(EndEquipmentItems.IRON_HAMMER));
            if (menu.getSlot(2).getItem().is(EndResourceItems.ENDER_DUST)) {
                failures.add("vanilla anvil ground " + input + " into ender_dust, but the recipe needs anvil level 2");
            }
        }

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Ender dust anvil regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    /**
     * BCLib #128: a worn anvil kept its wear only for the aeternium anvil. The metal-set anvils got a plain
     * {@code BlockItem}, which ignores the wear stored in the drop, and the combined
     * {@code destruction * max + durability} value could not tell {@code (1, max)} from {@code (2, 0)}. Every
     * state of every BetterEnd anvil must survive break-and-place, through the real loot drop and the real
     * {@code BlockItem} path; drops in the legacy combined format must still place as they used to.
     */
    @GameTest
    public void wornAnvilKeepsItsWearWhenBrokenAndPlaced(GameTestHelper helper) {
        final ServerPlayer player = MockPlayers.survival(helper, PLACER);
        final ItemStack pickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
        final List<String> failures = new ArrayList<>();
        final List<LeveledAnvilBlock> anvils = BuiltInRegistries.BLOCK
                .stream()
                .filter(b -> b instanceof LeveledAnvilBlock)
                .filter(b -> BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(BetterEnd.C.namespace))
                .map(b -> (LeveledAnvilBlock) b)
                .toList();
        if (anvils.size() < 3) {
            failures.add("found only " + anvils.size() + " BetterEnd anvils, expected aeternium, terminite and thallasium");
        }

        for (LeveledAnvilBlock anvil : anvils) {
            final String name = BuiltInRegistries.BLOCK.getKey(anvil).toString();
            if (!(anvil.asItem() instanceof BaseAnvilItem)) {
                failures.add(name + "'s item is " + anvil.asItem().getClass().getSimpleName() + ", expected BaseAnvilItem");
                continue;
            }
            final int max = anvil.getMaxDurability();
            for (int destruction = 0; destruction <= 2; destruction++) {
                for (int durability = 0; durability <= max; durability++) {
                    final BlockState state = anvil.defaultBlockState()
                                                  .setValue(LeveledAnvilBlock.DESTRUCTION, destruction)
                                                  .setValue(anvil.getDurabilityProp(), durability);
                    helper.setBlock(ANVIL, state);
                    final List<ItemStack> drops = Block.getDrops(
                            state, helper.getLevel(), helper.absolutePos(ANVIL), null, player, pickaxe
                    );
                    if (drops.size() != 1 || !drops.getFirst().is(anvil.asItem())) {
                        failures.add(name + " " + state + " dropped " + drops);
                        continue;
                    }
                    checkPlaced(helper, player, drops.getFirst(), anvil, destruction, durability, "", failures);

                    // the legacy combined value is only unambiguous below max durability
                    if (durability < max) {
                        final ItemStack legacy = new ItemStack(anvil);
                        final int value = destruction * max + durability;
                        CustomData.update(
                                BCLDataComponents.ANVIL_ENTITY_DATA,
                                legacy,
                                tag -> tag.putInt(BaseAnvilItem.DESTRUCTION, value)
                        );
                        checkPlaced(helper, player, legacy, anvil, destruction, durability, " (legacy drop)", failures);
                    }
                }
            }
        }

        player.discard();
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    "Anvil wear round-trip regression:\n - " + String.join("\n - ", failures)
            ));
        }
        helper.succeed();
    }

    private static void checkPlaced(
            GameTestHelper helper,
            ServerPlayer player,
            ItemStack drop,
            LeveledAnvilBlock anvil,
            int destruction,
            int durability,
            String context,
            List<String> failures
    ) {
        helper.setBlock(ANVIL, Blocks.STONE);
        helper.setBlock(ANVIL.above(), Blocks.AIR);
        final ItemStack stack = drop.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos abs = helper.absolutePos(ANVIL);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

        final BlockState placed = helper.getBlockState(ANVIL.above());
        final String name = BuiltInRegistries.BLOCK.getKey(anvil) + " (" + destruction + ", " + durability + ")" + context;
        if (!placed.is(anvil)) {
            failures.add(name + " placed " + placed);
        } else if (placed.getValue(LeveledAnvilBlock.DESTRUCTION) != destruction
                || placed.getValue(anvil.getDurabilityProp()) != durability) {
            failures.add(name + " placed as (" + placed.getValue(LeveledAnvilBlock.DESTRUCTION)
                    + ", " + placed.getValue(anvil.getDurabilityProp()) + ")");
        }
        helper.setBlock(ANVIL.above(), Blocks.AIR);
    }

    private static AnvilMenu openAnvil(GameTestHelper helper, ServerPlayer player, Block anvil, Item hammer) {
        helper.setBlock(ANVIL, anvil);
        final AnvilMenu menu = new AnvilMenu(
                1,
                player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(ANVIL))
        );
        if (hammer != null) {
            menu.getSlot(0).set(new ItemStack(EndEquipmentItems.AETERNIUM_SET.ingot, INGOTS));
            menu.getSlot(1).set(new ItemStack(hammer));
        }
        return menu;
    }

    private static Set<Item> cycle(GameTestHelper helper, ServerPlayer player, AnvilMenu menu, int expectedCount) {
        final int count = ((AnvilScreenHandlerExtended) menu).bcl_getRecipeCount();
        if (count != expectedCount) {
            throw helper.assertionException(Component.literal(
                    "Anvil offers " + count + " recipes, expected " + expectedCount
            ));
        }
        final Set<Item> seen = new HashSet<>();
        for (int i = 0; i < count; i++) {
            seen.add(menu.getSlot(2).getItem().getItem());
            menu.clickMenuButton(player, 1);
        }
        return seen;
    }
}
