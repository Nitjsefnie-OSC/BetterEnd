package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.BetterEnd;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Survival-path guards for every BetterEnd armor piece (#419, #442, #446, #449, #460, #487, #513, #514,
 * #551, #567, #569). The pieces are discovered from the registry rather than listed, so a new set is
 * covered the moment it is registered. Every check runs against vanilla diamond and netherite through
 * the identical code as a harness control.
 * <p>
 * Enchantability is a three-part contract in 26.x: the {@code minecraft:enchantable} component (without
 * it the enchanting table offers nothing), membership in the vanilla armor tags the
 * {@code #minecraft:enchantable/*} tags are built from (without it {@code Enchantment#canEnchant} - and
 * so the anvil - refuses every book outside creative), and the enchantment's primary-items tag for the
 * table. Creative mode bypasses the anvil check, which is how this stays hidden in a dev world.
 */
public class ArmorEnchantabilityGameTest {
    private record Piece(String name, Item item, EquipmentSlot slot, boolean humanoid) {}

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    );

    private static Piece piece(String name, Item item) {
        final Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
        if (equippable == null || !ARMOR_SLOTS.contains(equippable.slot())) return null;
        // Elytras occupy the chest slot without being humanoid armor; they are deliberately kept out of
        // #minecraft:chest_armor (that would make them trimmable), exactly like vanilla's elytra.
        final boolean humanoid = !item.components().has(DataComponents.GLIDER);
        return new Piece(name, item, equippable.slot(), humanoid);
    }

    private static List<Piece> betterEndArmor() {
        final List<Piece> pieces = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            final var id = BuiltInRegistries.ITEM.getKey(item);
            if (!BetterEnd.MOD_ID.equals(id.getNamespace())) continue;
            final Piece p = piece(id.getPath(), item);
            if (p != null) pieces.add(p);
        }
        return pieces;
    }

    private static List<Piece> controls() {
        return List.of(
                piece("diamond_helmet", Items.DIAMOND_HELMET),
                piece("diamond_chestplate", Items.DIAMOND_CHESTPLATE),
                piece("diamond_leggings", Items.DIAMOND_LEGGINGS),
                piece("diamond_boots", Items.DIAMOND_BOOTS),
                piece("netherite_chestplate", Items.NETHERITE_CHESTPLATE),
                piece("elytra", Items.ELYTRA)
        );
    }

    private static TagKey<Item> slotEnchantableTag(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ItemTags.HEAD_ARMOR_ENCHANTABLE;
            case CHEST -> ItemTags.CHEST_ARMOR_ENCHANTABLE;
            case LEGS -> ItemTags.LEG_ARMOR_ENCHANTABLE;
            case FEET -> ItemTags.FOOT_ARMOR_ENCHANTABLE;
            default -> throw new IllegalArgumentException(slot.toString());
        };
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static void failIfAny(GameTestHelper helper, String headline, List<String> failures) {
        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(headline + ":\n - " + String.join("\n - ", failures)));
        }
        helper.succeed();
    }

    private static void requirePieces(GameTestHelper helper, List<Piece> pieces) {
        // 4 sets x 4 pieces + 2 elytras. Guards against the discovery silently finding nothing.
        if (pieces.size() < 18) {
            throw helper.assertionException(Component.literal(
                    "expected at least 18 BetterEnd armor pieces, discovered " + pieces.stream().map(Piece::name).toList()
            ));
        }
    }

    // ------------------------------------------------------------------------------------------------

    private static void checkTags(Piece piece, List<String> failures) {
        final ItemStack stack = new ItemStack(piece.item());
        final Enchantable enchantable = stack.get(DataComponents.ENCHANTABLE);
        // Like vanilla's elytra, the BetterEnd elytras are anvil-only and carry no enchantable component.
        if (piece.humanoid() && (enchantable == null || enchantable.value() <= 0)) {
            failures.add(piece.name() + ": has no minecraft:enchantable component - the enchanting table offers nothing");
        }
        final List<TagKey<Item>> required = new ArrayList<>(List.of(
                ItemTags.DURABILITY_ENCHANTABLE, ItemTags.EQUIPPABLE_ENCHANTABLE
        ));
        if (piece.humanoid()) {
            required.add(ItemTags.ARMOR_ENCHANTABLE);
            required.add(slotEnchantableTag(piece.slot()));
        }
        for (TagKey<Item> tag : required) {
            if (!stack.is(tag)) failures.add(piece.name() + ": not in #" + tag.location());
        }
    }

    @GameTest
    public void everyArmorPieceIsInTheEnchantableTags(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        controls().forEach(p -> checkTags(p, failures));
        if (!failures.isEmpty()) failIfAny(helper, "Control failed - the assertion itself is wrong", failures);

        final List<Piece> pieces = betterEndArmor();
        requirePieces(helper, pieces);
        pieces.forEach(p -> checkTags(p, failures));
        failIfAny(helper, "BetterEnd armor is not enchantable in survival", failures);
    }

    // ------------------------------------------------------------------------------------------------

    private static void checkEnchantingTable(GameTestHelper helper, Piece piece, List<String> failures) {
        if (!piece.humanoid()) return; // elytras are anvil-only, as in vanilla
        final HolderLookup.RegistryLookup<Enchantment> lookup = helper
                .getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        final ItemStack stack = new ItemStack(piece.item());

        final var offered = EnchantmentHelper.getAvailableEnchantmentResults(
                30, stack, lookup.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE).stream()
        );
        if (offered.isEmpty()) {
            failures.add(piece.name() + ": the enchanting table has nothing to offer at level 30");
            return;
        }
        final Holder<Enchantment> expected = enchantment(helper, Enchantments.PROTECTION);
        if (offered.stream().noneMatch(e -> e.enchantment().equals(expected))) {
            failures.add(piece.name() + ": the enchanting table never offers " + expected.getRegisteredName());
        }
        final var selected = EnchantmentHelper.selectEnchantment(
                RandomSource.create(42), stack, 30, lookup.getOrThrow(EnchantmentTags.IN_ENCHANTING_TABLE).stream()
        );
        if (selected.isEmpty()) {
            failures.add(piece.name() + ": a level-30 enchanting roll selected nothing");
        }
    }

    @GameTest
    public void enchantingTableOffersEnchantmentsForEveryArmorPiece(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        controls().forEach(p -> checkEnchantingTable(helper, p, failures));
        if (!failures.isEmpty()) failIfAny(helper, "Control failed - the assertion itself is wrong", failures);

        betterEndArmor().forEach(p -> checkEnchantingTable(helper, p, failures));
        failIfAny(helper, "Enchanting table regression on BetterEnd armor", failures);
    }

    // ------------------------------------------------------------------------------------------------

    private static ItemStack anvil(Player player, ItemStack left, ItemStack right) {
        final AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(left);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right);
        menu.createResult();
        return menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
    }

    private static void checkAnvil(GameTestHelper helper, Player player, Piece piece, List<String> failures) {
        final Holder<Enchantment> ench = piece.humanoid()
                ? enchantment(helper, Enchantments.PROTECTION)
                : enchantment(helper, Enchantments.UNBREAKING);

        final ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book, m -> m.set(ench, 1));
        final ItemStack withBook = anvil(player, new ItemStack(piece.item()), book);
        if (withBook.isEmpty() || EnchantmentHelper.getItemEnchantmentLevel(ench, withBook) != 1) {
            failures.add(piece.name() + ": the anvil refuses a " + ench.getRegisteredName() + " book");
        }

        final ItemStack enchanted = new ItemStack(piece.item());
        enchanted.enchant(ench, 1);
        final ItemStack damaged = new ItemStack(piece.item());
        damaged.setDamageValue(damaged.getMaxDamage() / 2);
        final ItemStack combined = anvil(player, damaged, enchanted);
        if (combined.isEmpty()) {
            failures.add(piece.name() + ": the anvil cannot combine two of this piece");
        } else if (combined.getDamageValue() >= damaged.getDamageValue()) {
            failures.add(piece.name() + ": combining two in the anvil did not repair it");
        }
    }

    @GameTest
    public void anvilAcceptsBooksAndCombinesEveryArmorPiece(GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (player.hasInfiniteMaterials()) {
            // AnvilMenu skips Enchantment#canEnchant for these, which would hide the very bug under test.
            throw helper.assertionException(Component.literal("harness: the mock survival player has infinite materials"));
        }
        final List<String> failures = new ArrayList<>();
        controls().forEach(p -> checkAnvil(helper, player, p, failures));
        if (!failures.isEmpty()) failIfAny(helper, "Control failed - the assertion itself is wrong", failures);

        betterEndArmor().forEach(p -> checkAnvil(helper, player, p, failures));
        failIfAny(helper, "Anvil regression on BetterEnd armor", failures);
    }

    // ------------------------------------------------------------------------------------------------

    private static void checkAttributes(Piece piece, List<String> failures) {
        final ItemAttributeModifiers modifiers = new ItemStack(piece.item()).get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            failures.add(piece.name() + ": has no attribute modifiers");
            return;
        }
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().is(Attributes.ATTACK_DAMAGE) || entry.attribute().is(Attributes.ATTACK_SPEED)) {
                failures.add(piece.name() + ": carries weapon stat " + entry.attribute().getRegisteredName());
            }
            if (!entry.slot().test(piece.slot())) {
                failures.add(piece.name() + ": " + entry.attribute().getRegisteredName()
                        + " is bound to " + entry.slot() + ", which the piece is never worn in");
            }
        }
        // ItemAttributeModifiers#compute has no attribute filter on this version, so sum the armor entries
        final double armor = modifiers.modifiers().stream()
                .filter(e -> e.attribute().is(Attributes.ARMOR) && e.slot().test(piece.slot()))
                .mapToDouble(e -> e.modifier().amount())
                .sum();
        if (armor <= 0.0) {
            failures.add(piece.name() + ": grants no armor while worn in " + piece.slot());
        }
    }

    @GameTest
    public void everyArmorPieceGrantsArmorInItsOwnSlot(GameTestHelper helper) {
        final List<String> failures = new ArrayList<>();
        controls().stream().filter(Piece::humanoid).forEach(p -> checkAttributes(p, failures));
        if (!failures.isEmpty()) failIfAny(helper, "Control failed - the assertion itself is wrong", failures);

        betterEndArmor().forEach(p -> checkAttributes(p, failures));
        failIfAny(helper, "BetterEnd armor attribute regression", failures);
    }

    /**
     * #567: the durability must stay in the range armor actually lives in. Bounded by twice the netherite
     * chestplate, so a doubled multiplier (the original bug shape) is caught without pinning balance values.
     */
    @GameTest
    public void armorDurabilityIsInTheVanillaRange(GameTestHelper helper) {
        final int bound = 2 * new ItemStack(Items.NETHERITE_CHESTPLATE).getMaxDamage();
        final List<String> failures = new ArrayList<>();
        for (Piece piece : betterEndArmor()) {
            final int max = new ItemStack(piece.item()).getMaxDamage();
            if (max <= 0 || max > bound) {
                failures.add(piece.name() + ": max durability " + max + " (expected 1.." + bound + ")");
            }
        }
        failIfAny(helper, "BetterEnd armor durability out of range", failures);
    }
}
