package org.betterx.betterend.config;

import org.betterx.bclib.BCLib;
import org.betterx.bclib.config.IgnoredConfigEntries;
import org.betterx.bclib.config.PathConfig;
import org.betterx.betterend.BetterEnd;

public class Configs {
    static {
        // Blocks and items can no longer be disabled (quiqueck/BetterEnd#281). blocks.json still holds the flower pot
        // ids, so only its block switches are removed; items.json is no longer read at all.
        IgnoredConfigEntries.removeRootSwitches(BetterEnd.MOD_ID, "blocks");
        IgnoredConfigEntries.warnIfDisabled(BetterEnd.MOD_ID, "items");
        // biomes.json only worked partially: "hasCaves" was ignored for BetterEnd's own biomes (their caves come from
        // the generated biome data) and "enabled" only applied to the BYG integration. recipes.json has not been read
        // for a long time (use a datapack to remove recipes). Both files are no longer read.
        IgnoredConfigEntries.warnIfDisabled(BetterEnd.MOD_ID, "biomes", "biomes and their caves");
        IgnoredConfigEntries.warnIfDisabled(BetterEnd.MOD_ID, "recipes", "recipes in this file");
    }

    public static final PathConfig ENTITY_CONFIG = new PathConfig(BetterEnd.MOD_ID, "entities");
    /**
     * Only holds the ids of potted plants and soils ({@code flower_pots}), blocks can no longer be disabled.
     */
    public static final PathConfig BLOCK_CONFIG = new PathConfig(BetterEnd.MOD_ID, "blocks");
    public static final PathConfig GENERATOR_CONFIG = new PathConfig(BetterEnd.MOD_ID, "generator", false);
    public static final PathConfig ENCHANTMENT_CONFIG = new PathConfig(BetterEnd.MOD_ID, "enchantments");

    public static final PathConfig CLENT_CONFIG = new PathConfig(BetterEnd.MOD_ID, "client", false);

    public static void saveConfigs() {
        ENTITY_CONFIG.saveChanges();
        BLOCK_CONFIG.saveChanges();
        GENERATOR_CONFIG.saveChanges();
        ENCHANTMENT_CONFIG.saveChanges();

        if (BCLib.isClient()) {
            CLENT_CONFIG.saveChanges();
        }
    }
}
