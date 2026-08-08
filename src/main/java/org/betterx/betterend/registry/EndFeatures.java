package org.betterx.betterend.registry;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.features.EndOreFeatures;
import org.betterx.betterend.registry.features.EndPlacedCaveFeatures;
import org.betterx.betterend.registry.features.EndTerrainFeatures;
import org.betterx.betterend.world.biome.EndBiomeBuilder;
import org.betterx.betterend.world.features.*;
import org.betterx.betterend.world.features.bushes.*;
import org.betterx.betterend.world.features.terrain.*;
import org.betterx.betterend.world.features.terrain.caves.CaveSurfaceCoatFeature;
import org.betterx.betterend.world.features.terrain.caves.StalactiteClusterFeature;
import org.betterx.betterend.world.features.trees.*;
import de.ambertation.wover.feature.api.FeatureManager;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Registers BetterEnd's {@link Feature} <em>types</em> and hands out the instances that are usable as-is.
 * <p>
 * 26.3 collapsed {@code ConfiguredFeature} into {@link Feature}, which splits this class in two:
 * <ul>
 *     <li>{@code BuiltInRegistries.FEATURE} (a registry of stateless {@code Feature<FC>} singletons) became
 *     {@code BuiltInRegistries.FEATURE_TYPE}, a registry of {@code MapCodec<? extends Feature>} - so what
 *     gets registered is always a codec.</li>
 *     <li>A {@link Feature} instance <em>is</em> a configured feature. Features that never had a
 *     configuration therefore still have a singleton here, and it is what gets handed to
 *     {@code FeatureContentManager.withFeature(id, feature)}. Features that do carry a configuration have no
 *     meaningful singleton any more: every use site builds its own instance (see the datagen providers), so
 *     only the type is registered and the constant is the {@link MapCodec} itself.</li>
 * </ul>
 * The old {@code inlineBuild} guard against double registration is gone with the registry it queried:
 * {@code FEATURE_TYPE} is a plain built-in registry that is populated exactly once, at class-init.
 */
public class EndFeatures {
    // --- Features that carry a configuration: only the type is registered. -------------------------------
    public static final MapCodec<StalactiteFeature> STALACTITE_FEATURE = registerType("stalactite_feature", StalactiteFeature.CODEC);
    public static final MapCodec<BuildingListFeature> BUILDING_LIST_FEATURE = registerType("building_list_feature", BuildingListFeature.CODEC);
    public static final MapCodec<VineFeature> VINE_FEATURE = registerType("vine_feature", VineFeature.CODEC);
    public static final MapCodec<WallPlantFeature> WALL_PLANT_FEATURE = registerType("wall_plant_feature", WallPlantFeature.CODEC);
    public static final MapCodec<WallPlantOnLogFeature> WALL_PLANT_ON_LOG_FEATURE = registerType("wall_plant_on_log_feature", WallPlantOnLogFeature.CODEC);
    public static final MapCodec<GlowPillarFeature> GLOW_PILLAR_FEATURE = registerType("glow_pillar_feature", GlowPillarFeature.CODEC);
    public static final MapCodec<HydraluxFeature> HYDRALUX_FEATURE = registerType("hydralux_feature", HydraluxFeature.CODEC);
    public static final MapCodec<LanceleafFeature> LANCELEAF_FEATURE = registerType("lanceleaf_feature", LanceleafFeature.CODEC);
    public static final MapCodec<MengerSpongeFeature> MENGER_SPONGE_FEATURE = registerType("menger_sponge_feature", MengerSpongeFeature.CODEC);
    public static final MapCodec<SinglePlantFeature> SINGLE_PLANT_FEATURE = registerType("single_plant_feature", SinglePlantFeature.CODEC);
    public static final MapCodec<SingleInvertedScatterFeature> SINGLE_INVERTED_SCATTER_FEATURE = registerType("single_inverted_scatter_feature", SingleInvertedScatterFeature.CODEC);
    public static final MapCodec<DoublePlantFeature> DOUBLE_PLANT_FEATURE = registerType("double_plant_feature", DoublePlantFeature.CODEC);
    public static final MapCodec<UnderwaterPlantFeature> UNDERWATER_PLANT_FEATURE = registerType("underwater_plant_feature", UnderwaterPlantFeature.CODEC);
    public static final MapCodec<ArchFeature> ARCH_FEATURE = registerType("arch_feature", ArchFeature.CODEC);
    public static final MapCodec<ThinArchFeature> THIN_ARCH_FEATURE = registerType("thin_arch_feature", ThinArchFeature.CODEC);
    public static final MapCodec<CharniaFeature> CHARNIA_FEATURE = registerType("charnia_feature", CharniaFeature.CODEC);
    public static final MapCodec<BlueVineFeature> BLUE_VINE_FEATURE = registerType("blue_vine_feature", BlueVineFeature.CODEC);
    public static final MapCodec<FilaluxFeature> FILALUX_FEATURE = registerType("filalux_feature", FilaluxFeature.CODEC);
    public static final MapCodec<EndLilyFeature> END_LILY_FEATURE = registerType("end_lily_feature", EndLilyFeature.CODEC);
    public static final MapCodec<EndLotusFeature> END_LOTUS_FEATURE = registerType("end_lotus_feature", EndLotusFeature.CODEC);
    public static final MapCodec<EndLotusLeafFeature> END_LOTUS_LEAF_FEATURE = registerType("end_lotus_leaf_feature", EndLotusLeafFeature.CODEC);
    public static final MapCodec<BushFeature> BUSH_FEATURE = registerType("bush_feature", BushFeature.CODEC);
    public static final MapCodec<SingleBlockFeature> SINGLE_BLOCK_FEATURE = registerType("single_block_feature", SingleBlockFeature.CODEC);
    public static final MapCodec<BushWithOuterFeature> BUSH_WITH_OUTER_FEATURE = registerType("bush_with_outer_feature", BushWithOuterFeature.CODEC);
    public static final MapCodec<StalactiteClusterFeature> STALACTITE_CLUSTER = registerType("stalactite_cluster", StalactiteClusterFeature.CODEC);
    public static final MapCodec<CaveSurfaceCoatFeature> CAVE_SURFACE_COAT = registerType("cave_surface_coat", CaveSurfaceCoatFeature.CODEC);
    public static final MapCodec<OreLayerFeature> LAYERED_ORE_FEATURE = registerType("ore_layer", OreLayerFeature.CODEC);
    public static final MapCodec<IceStarFeature> ICE_STAR_FEATURE = registerType("ice_star", IceStarFeature.CODEC);
    public static final MapCodec<CrashedShipFeature> CRASHED_SHIP_FEATURE = registerType("crashed_ship", CrashedShipFeature.CODEC);

    // --- Features without a configuration: the singleton is the configured feature. ----------------------
    public static final MossyGlowshroomFeature MOSSY_GLOWSHROOM_FEATURE = register("mossy_glowshroom", MossyGlowshroomFeature.CODEC, new MossyGlowshroomFeature());
    public static final PythadendronTreeFeature PYTHADENDRON_TREE_FEATURE = register("pythadendron_tree", PythadendronTreeFeature.CODEC, new PythadendronTreeFeature());
    public static final LacugroveFeature LACUGROVE_FEATURE = register("lacugrove", LacugroveFeature.CODEC, new LacugroveFeature());
    public static final DragonTreeFeature DRAGON_TREE_FEATURE = register("dragon_tree", DragonTreeFeature.CODEC, new DragonTreeFeature());
    public static final TenaneaFeature TENANEA_FEATURE = register("tenanea", TenaneaFeature.CODEC, new TenaneaFeature());
    public static final HelixTreeFeature HELIX_TREE_FEATURE = register("helix_tree", HelixTreeFeature.CODEC, new HelixTreeFeature());
    public static final DragonHelixTreeFeature DRAGON_HELIX_TREE_FEATURE = register("dragon_helix_tree", DragonHelixTreeFeature.CODEC, new DragonHelixTreeFeature());
    public static final UmbrellaTreeFeature UMBRELLA_TREE_FEATURE = register("umbrella_tree", UmbrellaTreeFeature.CODEC, new UmbrellaTreeFeature());
    public static final JellyshroomFeature JELLYSHROOM_FEATURE = register("jellyshroom", JellyshroomFeature.CODEC, new JellyshroomFeature());
    public static final GiganticAmaranitaFeature GIGANTIC_AMARANITA_FEATURE = register("gigantic_amaranita", GiganticAmaranitaFeature.CODEC, new GiganticAmaranitaFeature());
    public static final LucerniaFeature LUCERNIA_FEATURE = register("lucernia", LucerniaFeature.CODEC, new LucerniaFeature());
    public static final TenaneaBushFeature TENANEA_BUSH_FEATURE = register("tenanea_bush", TenaneaBushFeature.CODEC, new TenaneaBushFeature());
    public static final Lumecorn LUMECORN_FEATURE = register("lumecorn", Lumecorn.CODEC, new Lumecorn());
    public static final LargeAmaranitaFeature LARGE_AMARANITA_FEATURE = register("large_amaranita", LargeAmaranitaFeature.CODEC, new LargeAmaranitaFeature());
    public static final AmaranitaPatchFeature AMARANITA_PATCH_FEATURE = register("amaranita_patch", AmaranitaPatchFeature.CODEC, new AmaranitaPatchFeature());
    public static final NeonCactusFeature NEON_CACTUS_FEATURE = register("neon_cactus", NeonCactusFeature.CODEC, new NeonCactusFeature());

    //Lakes
    // The old EndLakeFeature is gone: End lakes are now the EndLakeStructure family (see
    // EndStructures.END_LAKE*), which carves per-chunk during the LAKES step and no longer strands
    // trees over the water.
    public static final DesertLakeFeature DESERT_LAKE_FEATURE = register("desert_lake", DesertLakeFeature.CODEC, new DesertLakeFeature());
    public static final SulphuricLakeFeature SULPHURIC_LAKE_FEATURE = register("sulphuric_lake", SulphuricLakeFeature.CODEC, new SulphuricLakeFeature());

    //Terrain
    public static final SurfaceVentFeature SURFACE_VENT_FEATURE = register("surface_vent", SurfaceVentFeature.CODEC, new SurfaceVentFeature());
    public static final SulphurHillFeature SULPHUR_HILL_FEATURE = register("sulphur_hill", SulphurHillFeature.CODEC, new SulphurHillFeature());
    public static final ObsidianPillarBasementFeature OBSIDIAN_PILLAR_FEATURE = register("obsidian_pillar_basement", ObsidianPillarBasementFeature.CODEC, new ObsidianPillarBasementFeature());
    public static final ObsidianBoulderFeature OBSIDIAN_BOULDER_FEATURE = register("obsidian_boulder", ObsidianBoulderFeature.CODEC, new ObsidianBoulderFeature());
    public static final FallenPillarFeature FALLEN_PILLAR_FEATURE = register("fallen_pillar", FallenPillarFeature.CODEC, new FallenPillarFeature());
    public static final SilkMothNestFeature SILK_MOTH_NEST_FEATURE = register("silk_moth_nest", SilkMothNestFeature.CODEC, new SilkMothNestFeature());
    public static final SpireFeature SPIRE_FEATURE = register("spire", SpireFeature.CODEC, new SpireFeature());
    public static final FloatingSpireFeature FLOATING_SPIRE_FEATURE = register("floating_spire", FloatingSpireFeature.CODEC, new FloatingSpireFeature());
    public static final GeyserFeature GEYSER_FEATURE = register("geyser", GeyserFeature.CODEC, new GeyserFeature());
    public static final PondWithWaterfallFeature POND_WITH_WATERFALL_FEATURE = register("pond_with_waterfall", PondWithWaterfallFeature.CODEC, new PondWithWaterfallFeature());
    public static final BiomeIslandFeature OVERWORLD_ISLAND = register("overworld_island", BiomeIslandFeature.CODEC, new BiomeIslandFeature());

    // Caves
    public static final SmaragdantCrystalFeature SMARAGDANT_CRYSTAL_FEATURE = register("smaragdant_crystal", SmaragdantCrystalFeature.CODEC, new SmaragdantCrystalFeature());
    public static final BigAuroraCrystalFeature BIG_AURORA_CRYSTAL_FEATURE = register("big_aurora_crystal", BigAuroraCrystalFeature.CODEC, new BigAuroraCrystalFeature());
    public static final CavePumpkinFeature CAVE_PUMPKIN_FEATURE = register("cave_pumpkin", CavePumpkinFeature.CODEC, new CavePumpkinFeature());

    /**
     * Registers a feature type (its {@link MapCodec}) in {@code BuiltInRegistries.FEATURE_TYPE}.
     *
     * @param name  the path of the feature type's id
     * @param codec the codec for the feature type
     * @param <F>   the feature type
     * @return the same codec that was passed in
     */
    public static <F extends Feature> MapCodec<F> registerType(String name, MapCodec<F> codec) {
        final Identifier l = BetterEnd.C.mk(name);
        return FeatureManager.register(l, codec);
    }

    /**
     * Registers a feature type and returns a configured instance of it, so that touching any constant
     * guarantees the corresponding type is registered.
     *
     * @param name    the path of the feature type's id
     * @param codec   the codec for the feature type
     * @param feature the configured instance to hand out
     * @param <F>     the feature type
     * @return {@code feature}
     */
    public static <F extends Feature> F register(String name, MapCodec<F> codec, F feature) {
        registerType(name, codec);
        return feature;
    }

    // NOTE: hasCaves no longer selects any feature-based cave carving here (the legacy
    // RoundCaveFeature/TunelCaveFeature pair was removed once real WorldCarvers landed, see
    // EndCaveCarver/EndTunnelCarver + EndCarvers). The parameter is kept only because it mirrors
    // the biome-data "hasCaves" flag that EndCaveBiomeDecider consumes when deciding whether a
    // land biome gets a vertical cave-biome counterpart; it is intentionally unused in this method.
    public static void addDefaultFeatures(
            EndBiomeBuilder builder, boolean hasCaves
    ) {
        builder.feature(EndOreFeatures.THALLASIUM_ORE);
        builder.feature(EndOreFeatures.ENDER_ORE);
        builder.feature(EndTerrainFeatures.CRASHED_SHIP);
        // The cave surface coat goes on EVERY End biome, land ones included, even though it only ever paints
        // cave columns. It is not a per-biome decoration: one placement sweeps the whole chunk and decides
        // per column what to do (see CaveSurfaceCoatFeature). What it needs from the placement is simply to
        // RUN in every chunk that holds part of the cave band - and a placed feature only runs where the
        // biome at the rolled position declared it. Declaring it on the cave biomes alone left that to
        // chance: in a chunk that is part cave-biome and part land, the roll lands in a land column often
        // enough that whole chunks of cave went uncoated (a census found one such chunk in 25, with 142
        // uncoated cave-surface blocks). Declaring it everywhere makes the roll always succeed; in a chunk
        // with no cave columns the pass costs 324 biome lookups and stops.
        builder.feature(EndPlacedCaveFeatures.CAVE_SURFACE_COAT);
    }

    public static void register() {
    }
}
