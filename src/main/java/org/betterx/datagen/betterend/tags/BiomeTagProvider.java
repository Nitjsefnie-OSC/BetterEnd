package org.betterx.datagen.betterend.tags;

import org.betterx.betterend.registry.EndBiomes;
import org.betterx.betterend.registry.EndStructures;
import org.betterx.betterend.registry.EndTags;
import de.ambertation.wover.core.api.ModCore;
import de.ambertation.wover.datagen.api.WoverTagProvider;
import de.ambertation.wover.tag.api.event.context.TagBootstrapContext;
import de.ambertation.wover.tag.api.predefined.CommonBiomeTags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class BiomeTagProvider extends WoverTagProvider.ForBiomes {
    /**
     * Waystones places its wild End waystones (end stone style) only in the biomes of this tag. Waystones itself
     * lists just the vanilla End biomes, which BetterEnd's biomes mostly replace. This is compat data: the entries
     * are optional, and the tag is simply unused when Waystones is not installed.
     */
    public static final TagKey<Biome> WAYSTONES_END_STONE_WAYSTONE = TagKey.create(
            Registries.BIOME,
            Identifier.fromNamespaceAndPath("waystones", "has_structure/end_stone_waystone")
    );

    public BiomeTagProvider(ModCore modCore) {
        super(modCore);
    }

    @Override
    public void prepareTags(TagBootstrapContext<Biome> context) {
        context.add(
                EndStructures.ETERNAL_PORTAL.biomeTag(),
                Biomes.END_BARRENS,
                Biomes.END_MIDLANDS,
                Biomes.END_HIGHLANDS
        );

        context.add(
                EndTags.IS_END_HIGH_OR_MIDLAND,
                CommonBiomeTags.IS_END_HIGHLAND,
                CommonBiomeTags.IS_END_MIDLAND
        );

        // Land biomes only: no void/small-island biomes (ice starfield, flower islets, waterfall ponds) where a
        // waystone would end up on a speck of land over the void, no megalake (mostly water) and no cave biomes
        // (the waystone is placed on the surface, not in the cave).
        context.addOptional(
                WAYSTONES_END_STONE_WAYSTONE,
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
    }
}
