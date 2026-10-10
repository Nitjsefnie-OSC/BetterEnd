package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.config.Configs;

import de.ambertation.wunderlib.configs.AbstractConfig;
import de.ambertation.wunderlib.configs.ConfigFile;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Sweeps every {@code betterend:} registry entry that shows a name to the player and asserts the
 * translation key it actually resolves to has an {@code en_us} entry (#393: blocks and smithing templates
 * showed raw keys like {@code block.betterend.end_stone_brick_cracked}).
 * <p>
 * Keys are taken from the same place the game takes them (an item's name component, a block's
 * description id, an advancement's display, a config value's screen title, ...), so a block item that
 * forgot {@code useBlockDescriptionPrefix()} or a renamed registry entry shows up here instead of in a bug
 * report. The lookup goes through the server-side {@link Language}, which Fabric's resource loader fills
 * with every mod's {@code en_us.json}; a sanity check guards against that silently stopping.
 */
public class TranslationCoverageGameTest {
    private static final String NS = BetterEnd.C.namespace;

    @GameTest
    @SuppressWarnings("rawtypes")
    public void everyBetterEndNameHasAnEnglishTranslation(GameTestHelper helper) {
        final Language language = Language.getInstance();
        if (!language.has("itemGroup.betterend.blocks")) {
            throw helper.assertionException(Component.literal(
                    "server-side Language does not contain BetterEnd's en_us.json - sweep would be meaningless"
            ));
        }

        final Set<String> missing = new TreeSet<>();
        final BiConsumer<String, String> require = (source, key) -> {
            if (!language.has(key)) missing.add(key + "  (" + source + ")");
        };

        forEach(BuiltInRegistries.BLOCK, (id, block) -> require.accept("block " + id, block.getDescriptionId()));
        forEach(BuiltInRegistries.ITEM, (id, item) -> requireComponent(
                "item " + id, new ItemStack(item).getHoverName(), require
        ));
        forEach(BuiltInRegistries.ENTITY_TYPE, (id, type) -> require.accept("entity " + id, type.getDescriptionId()));
        forEach(BuiltInRegistries.MOB_EFFECT, (id, effect) -> require.accept("effect " + id, effect.getDescriptionId()));
        forEach(BuiltInRegistries.ATTRIBUTE, (id, attr) -> require.accept("attribute " + id, attr.getDescriptionId()));
        // game rules are not a registry on this version, and BetterEnd registers none here
        forEach(BuiltInRegistries.CREATIVE_MODE_TAB, (id, tab) -> requireComponent(
                "creative tab " + id, tab.getDisplayName(), require
        ));

        for (Holder.Reference<Potion> potion : BuiltInRegistries.POTION.listElements().toList()) {
            if (!potion.key().location().getNamespace().equals(NS)) continue;
            for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION, Items.TIPPED_ARROW)) {
                requireComponent(
                        "potion " + potion.key().location(),
                        PotionContents.createItemStack(container, potion).getHoverName(),
                        require
                );
            }
        }

        final var registries = helper.getLevel().registryAccess();
        registries.lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(enchantment -> {
            final ResourceLocation id = enchantment.key().location();
            if (!id.getNamespace().equals(NS)) return;
            requireComponent("enchantment " + id, enchantment.value().description(), require);
            // read by enchantment-description mods; every BetterEnd enchantment ships one
            if (enchantment.value().description().getContents() instanceof TranslatableContents t) {
                require.accept("enchantment description " + id, t.getKey() + ".desc");
            }
        });
        registries.lookupOrThrow(Registries.BIOME).listElements().forEach(biome -> {
            final ResourceLocation id = biome.key().location();
            if (id.getNamespace().equals(NS)) require.accept("biome " + id, id.toLanguageKey("biome"));
        });

        for (AdvancementHolder advancement : helper.getLevel().getServer().getAdvancements().getAllAdvancements()) {
            if (!advancement.id().getNamespace().equals(NS)) continue;
            advancement.value().display().ifPresent(display -> {
                requireComponent("advancement " + advancement.id(), display.getTitle(), require);
                requireComponent("advancement " + advancement.id(), display.getDescription(), require);
            });
        }

        for (ConfigFile config : List.<ConfigFile>of(Configs.GENERATOR_CONFIG, Configs.CLIENT_CONFIG)) {
            // getAllValues() is @Environment(CLIENT) and stripped on this dedicated server; the values
            // are the config class's public fields, which is how they register themselves anyway
            for (Field field : config.getClass().getFields()) {
                final Object o;
                try {
                    o = field.get(config);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                if (!(o instanceof AbstractConfig.Value)) continue;
                final AbstractConfig.Value value = (AbstractConfig.Value) o;
                if (value.isHiddenInUI()) continue;
                // the same three types the config screen renders (wunderlib ConfigScreen#fromConfig)
                if (!(o instanceof AbstractConfig.BooleanValue
                        || o instanceof AbstractConfig.IntValue
                        || o instanceof AbstractConfig.FloatValue)) continue;
                require.accept(
                        "config " + config.category,
                        "title.config." + config.category + "." + value.token.path() + "." + value.token.key()
                );
                final AbstractConfig.Group group = value.getGroup();
                if (group != null && group.modID().equals(NS)) {
                    require.accept("config group", "title.config.group." + group.modID() + "." + group.title());
                }
            }
        }

        if (!missing.isEmpty()) {
            throw helper.assertionException(Component.literal(
                    missing.size() + " betterend names without an en_us translation:\n - "
                            + String.join("\n - ", missing)
            ));
        }
        helper.succeed();
    }

    private static <T> void forEach(Registry<T> registry, BiConsumer<ResourceLocation, T> action) {
        for (ResourceLocation id : registry.keySet()) {
            if (id.getNamespace().equals(NS)) action.accept(id, registry.getValue(id));
        }
    }

    private static void requireComponent(String source, Component component, BiConsumer<String, String> require) {
        if (component.getContents() instanceof TranslatableContents t) {
            require.accept(source, t.getKey());
            for (Object arg : t.getArgs()) {
                if (arg instanceof Component c) requireComponent(source, c, require);
            }
        }
        for (Component sibling : component.getSiblings()) requireComponent(source, sibling, require);
    }
}
