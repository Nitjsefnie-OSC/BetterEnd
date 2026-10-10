package org.betterx.betterend.testmod.gametest;

import de.ambertation.wover.biome.api.data.BiomeData;
import de.ambertation.wover.biome.api.data.BiomeDataRegistry;
import de.ambertation.wover.biome.impl.data.BiomeDataRegistryImpl;
import de.ambertation.wover.generator.api.biomesource.WoverBiomePicker;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.List;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * BetterEnd Biomes are picked slightly more often (x1.25) than vanilla Biomes and Biomes of other mods, while
 * vanilla and other mods share WoVer's default weight. Vanilla used to get 0.01, which made it vanish as soon
 * as another mod added Biomes to the same dimension. The End's void ring is a deliberate exception, see
 * {@link #ourVoidBiomesWeighHalfOfVanillaVoid}.
 */
public class BiomeWeightGameTest {
    private static final float FACTOR = 1.25f;
    private static final float EPSILON = 1e-5f;
    private static final int SAMPLES = 40_000;
    private static final long SEED = 263L;

    private static final ResourceKey<Biome> OURS = key("betterend", "foggy_mushroomland");
    private static final ResourceKey<Biome> VANILLA = Biomes.END_HIGHLANDS;
    // stand-in for a Biome of another mod: a bound Biome without any WoVer data
    private static final ResourceKey<Biome> FOREIGN = Biomes.PLAINS;

    @GameTest
    public void ourBiomesWeighSlightlyMoreThanVanillaAndForeign(GameTestHelper helper) {
        final Registry<BiomeData> data = dataRegistry(helper);
        final float foreign = BiomeData.tempOf(FOREIGN).genChance();
        final float vanilla = BiomeDataRegistryImpl.getFromRegistryOrTemp(data, VANILLA).genChance();

        if (Math.abs(vanilla - foreign) > EPSILON) {
            helper.fail(VANILLA.identifier() + " has genChance " + vanilla + ", Biomes of other mods " + foreign);
            return;
        }
        for (String id : List.of("foggy_mushroomland", "chorus_forest", "megalake", "crystal_mountains", "shadow_forest", "amber_land")) {
            final BiomeData d = BiomeDataRegistryImpl.getFromRegistryOrTemp(data, key("betterend", id));
            if (d.isTemp() || Math.abs(d.genChance() - FACTOR * foreign) > EPSILON) {
                helper.fail("betterend:" + id + " has genChance " + d.genChance() + ", expected " + FACTOR * foreign);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest
    public void pickerPrefersOurBiomesSlightly(GameTestHelper helper) {
        final Registry<BiomeData> data = dataRegistry(helper);
        final HolderLookup<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        final WoverBiomePicker picker = new WoverBiomePicker(biomes, VANILLA);
        picker.addBiome(BiomeDataRegistryImpl.getFromRegistryOrTemp(data, OURS));
        picker.addBiome(BiomeDataRegistryImpl.getFromRegistryOrTemp(data, VANILLA));
        picker.addBiome(BiomeData.tempOf(FOREIGN));
        picker.rebuild();

        int ours = 0, vanilla = 0, foreign = 0;
        final WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(SEED));
        for (int i = 0; i < SAMPLES; i++) {
            final ResourceKey<Biome> picked = picker.getBiome(random).biomeData.biomeKey;
            if (picked.equals(OURS)) ours++;
            else if (picked.equals(VANILLA)) vanilla++;
            else if (picked.equals(FOREIGN)) foreign++;
        }

        final String counts = OURS.identifier() + "=" + ours + ", " + VANILLA.identifier() + "=" + vanilla
                + ", " + FOREIGN.identifier() + "=" + foreign;
        // expected 1.25 : 1 : 1 (38.5% : 30.8% : 30.8%)
        if (Math.abs(vanilla - foreign) > SAMPLES * 0.03) {
            helper.fail("Vanilla and foreign Biomes should be picked equally often: " + counts);
            return;
        }
        final double ratio = ours / (double) Math.max(1, (vanilla + foreign) / 2);
        if (ratio < 1.15 || ratio > 1.35) {
            helper.fail("Our Biome should be picked ~1.25x as often as the others, got " + ratio + ": " + counts);
            return;
        }
        helper.succeed();
    }

    private static final List<ResourceKey<Biome>> OUR_VOID = List.of(
            key("betterend", "flower_islets"),
            key("betterend", "waterfall_ponds"),
            key("betterend", "ice_starfield")
    );

    /**
     * Deliberate exception to the x1.25 rule: the outer void ring should stay mostly empty vanilla void with
     * BetterEnd's islands mixed in. Every BetterEnd void Biome weighs half of vanilla {@code small_end_islands}
     * (0.4 x 1.25 = 0.5 against 1.0), so the ring is ~2/5 vanilla and ~1/5 each BetterEnd void Biome.
     */
    @GameTest
    public void ourVoidBiomesWeighHalfOfVanillaVoid(GameTestHelper helper) {
        final Registry<BiomeData> data = dataRegistry(helper);
        final float vanilla = BiomeDataRegistryImpl.getFromRegistryOrTemp(data, Biomes.SMALL_END_ISLANDS).genChance();
        final float foreign = BiomeData.tempOf(FOREIGN).genChance();
        if (Math.abs(vanilla - foreign) > EPSILON) {
            helper.fail("minecraft:small_end_islands has genChance " + vanilla + ", Biomes of other mods " + foreign);
            return;
        }
        for (ResourceKey<Biome> id : OUR_VOID) {
            final BiomeData d = BiomeDataRegistryImpl.getFromRegistryOrTemp(data, id);
            if (d.isTemp() || Math.abs(d.genChance() - 0.5f * vanilla) > EPSILON) {
                helper.fail(id.identifier() + " has genChance " + d.genChance() + ", expected " + 0.5f * vanilla);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * The void ring as WoVer builds it without other mods: BetterEnd's three void Biomes against vanilla
     * {@code small_end_islands}. Expected shares 40% vanilla and 20% each BetterEnd void Biome.
     */
    @GameTest
    public void voidRingIsMostlyVanillaVoidWithOurIslandsMixedIn(GameTestHelper helper) {
        final Registry<BiomeData> data = dataRegistry(helper);
        final HolderLookup<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        final WoverBiomePicker picker = new WoverBiomePicker(biomes, Biomes.SMALL_END_ISLANDS);
        for (ResourceKey<Biome> id : OUR_VOID) {
            picker.addBiome(BiomeDataRegistryImpl.getFromRegistryOrTemp(data, id));
        }
        picker.addBiome(BiomeDataRegistryImpl.getFromRegistryOrTemp(data, Biomes.SMALL_END_ISLANDS));
        picker.rebuild();

        final int[] counts = new int[OUR_VOID.size()];
        int vanilla = 0;
        final WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(SEED));
        for (int i = 0; i < SAMPLES; i++) {
            final ResourceKey<Biome> picked = picker.getBiome(random).biomeData.biomeKey;
            final int idx = OUR_VOID.indexOf(picked);
            if (idx >= 0) counts[idx]++;
            else if (picked.equals(Biomes.SMALL_END_ISLANDS)) vanilla++;
        }

        final StringBuilder sb = new StringBuilder("small_end_islands=" + vanilla);
        for (int i = 0; i < counts.length; i++) sb.append(", ").append(OUR_VOID.get(i).identifier()).append("=").append(counts[i]);
        if (Math.abs(vanilla - SAMPLES * 0.4) > SAMPLES * 0.03) {
            helper.fail("small_end_islands should fill ~40% of the void ring: " + sb);
            return;
        }
        for (int i = 0; i < counts.length; i++) {
            if (Math.abs(counts[i] - SAMPLES * 0.2) > SAMPLES * 0.03) {
                helper.fail(OUR_VOID.get(i).identifier() + " should fill ~20% of the void ring: " + sb);
                return;
            }
            if (counts[i] >= vanilla) {
                helper.fail("small_end_islands should stay the most common void Biome: " + sb);
                return;
            }
        }
        helper.succeed();
    }

    private static Registry<BiomeData> dataRegistry(GameTestHelper helper) {
        return helper.getLevel().registryAccess().lookupOrThrow(BiomeDataRegistry.BIOME_DATA_REGISTRY);
    }

    private static ResourceKey<Biome> key(String ns, String path) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(ns, path));
    }
}
