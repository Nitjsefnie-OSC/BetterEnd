package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.EndBiomes;
import de.ambertation.wover.test.api.gametest.GameTestAssertions;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * BetterEnd ships {@code waystones:has_structure/end_stone_waystone} as compat data, so Waystones' wild End
 * waystones also generate in BetterEnd's land biomes (Waystones itself only lists the vanilla End biomes). The test
 * suite runs without Waystones: the tag must still load (its entries are optional), contain the land biomes and
 * leave out void/small-island, lake and cave biomes.
 */
public class WaystonesBiomeTagGameTest {
    private static final TagKey<Biome> END_STONE_WAYSTONE = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("waystones", "has_structure/end_stone_waystone")
    );

    private static final List<ResourceKey<Biome>> LAND = List.of(
            EndBiomes.AMBER_LAND.key,
            EndBiomes.BLOSSOMING_SPIRES.key,
            EndBiomes.CHORUS_FOREST.key,
            EndBiomes.CRYSTAL_MOUNTAINS.key,
            EndBiomes.DRAGON_GRAVEYARDS.key,
            EndBiomes.DRY_SHRUBLAND.key,
            EndBiomes.DUST_WASTELANDS.key,
            EndBiomes.FOGGY_MUSHROOMLAND.key,
            EndBiomes.GLOWING_GRASSLANDS.key,
            EndBiomes.LANTERN_WOODS.key,
            EndBiomes.MEGALAKE_GROVE.key,
            EndBiomes.NEON_OASIS.key,
            EndBiomes.PAINTED_MOUNTAINS.key,
            EndBiomes.SHADOW_FOREST.key,
            EndBiomes.SULPHUR_SPRINGS.key,
            EndBiomes.UMBRA_VALLEY.key,
            EndBiomes.UMBRELLA_JUNGLE.key
    );

    private static final List<ResourceKey<Biome>> NOT_LAND = List.of(
            EndBiomes.ICE_STARFIELD.key,
            EndBiomes.FLOWER_ISLETS.key,
            EndBiomes.WATERFALL_PONDS.key,
            EndBiomes.MEGALAKE.key,
            EndBiomes.EMPTY_END_CAVE.key,
            EndBiomes.EMPTY_SMARAGDANT_CAVE.key,
            EndBiomes.LUSH_SMARAGDANT_CAVE.key,
            EndBiomes.EMPTY_AURORA_CAVE.key,
            EndBiomes.LUSH_AURORA_CAVE.key,
            EndBiomes.JADE_CAVE.key
    );

    @GameTest
    public void landBiomesHaveWildWaystones(GameTestHelper helper) {
        final Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        final List<String> failures = new ArrayList<>();

        if (biomes.get(END_STONE_WAYSTONE).isEmpty()) {
            failures.add("#" + END_STONE_WAYSTONE.location() + " did not load");
        }
        for (ResourceKey<Biome> key : LAND) {
            if (!biomes.getOrThrow(key).is(END_STONE_WAYSTONE)) {
                failures.add(key.location() + " is not in #" + END_STONE_WAYSTONE.location());
            }
        }
        for (ResourceKey<Biome> key : NOT_LAND) {
            if (biomes.getOrThrow(key).is(END_STONE_WAYSTONE)) {
                failures.add(key.location() + " should not be in #" + END_STONE_WAYSTONE.location());
            }
        }
        // every BetterEnd biome is classified above, so a new biome gets a deliberate decision
        biomes.registryKeySet().stream()
              .filter(key -> key.location().getNamespace().equals("betterend"))
              .filter(key -> !LAND.contains(key) && !NOT_LAND.contains(key))
              .forEach(key -> failures.add(key.location() + " is neither listed as land nor as non-land biome"));

        GameTestAssertions.failIfAny(helper, "Waystones compat biome tag", failures);
        helper.succeed();
    }
}
