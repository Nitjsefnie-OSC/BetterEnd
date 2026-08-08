package org.betterx.betterend.registry.features;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.world.features.CavePumpkinFeature;
import org.betterx.betterend.world.features.VineFeature;
import org.betterx.betterend.world.features.bushes.BushFeature;
import org.betterx.betterend.world.features.terrain.*;
import org.betterx.betterend.world.features.terrain.caves.CaveSurfaceCoatFeature;
import org.betterx.betterend.world.features.terrain.caves.StalactiteClusterFeature;
import de.ambertation.wover.feature.api.configured.FeatureContentManager;
import de.ambertation.wover.feature.api.configured.FeatureKey;
import de.ambertation.wover.feature.api.configured.configurators.WithFeature;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.VegetationPatchFeature;

/**
 * 26.3 collapsed {@code ConfiguredFeature} into {@link Feature}, so a key no longer pairs a stateless
 * feature singleton with a configuration - it names one fully configured {@link Feature}.
 * <p>
 * Keys for features that never had a configuration therefore hold the singleton from {@link EndFeatures}
 * directly. The rest are declared with {@link #lateBound(Identifier)}: their configuration is only known
 * during datagen (the stalactite clusters even need the {@code BootstrapContext} to resolve their
 * sub-feature holders), so the instance is supplied there via
 * {@link WithFeature#feature(Feature) bootstrap(ctx).feature(...)}.
 */
public class EndConfiguredCaveFeatures {
    public static final FeatureKey<WithFeature<SmaragdantCrystalFeature>> SMARAGDANT_CRYSTAL = FeatureContentManager.withFeature(BetterEnd.C.mk("smaragdant_crystal"), EndFeatures.SMARAGDANT_CRYSTAL_FEATURE);
    public static final FeatureKey<WithFeature<SingleBlockFeature>> SMARAGDANT_CRYSTAL_SHARD = lateBound(BetterEnd.C.mk("smaragdant_crystal_shard"));
    public static final FeatureKey<WithFeature<BigAuroraCrystalFeature>> BIG_AURORA_CRYSTAL = FeatureContentManager.withFeature(BetterEnd.C.mk("big_aurora_crystal"), EndFeatures.BIG_AURORA_CRYSTAL_FEATURE);
    public static final FeatureKey<WithFeature<BushFeature>> CAVE_BUSH = lateBound(BetterEnd.C.mk("cave_bush"));
    public static final FeatureKey<WithFeature<SingleBlockFeature>> CAVE_GRASS = lateBound(BetterEnd.C.mk("cave_grass"));
    public static final FeatureKey<WithFeature<VineFeature>> RUBINEA = lateBound(BetterEnd.C.mk("rubinea"));
    public static final FeatureKey<WithFeature<VineFeature>> MAGNULA = lateBound(BetterEnd.C.mk("magnula"));
    public static final FeatureKey<WithFeature<StalactiteFeature>> END_STONE_STALACTITE = lateBound(BetterEnd.C.mk("end_stone_stalactite"));
    public static final FeatureKey<WithFeature<StalactiteFeature>> END_STONE_STALAGMITE = lateBound(BetterEnd.C.mk("end_stone_stalagmite"));
    public static final FeatureKey<WithFeature<StalactiteFeature>> END_STONE_STALACTITE_CAVEMOSS = lateBound(BetterEnd.C.mk("end_stone_stalactite_cavemoss"));
    public static final FeatureKey<WithFeature<StalactiteFeature>> END_STONE_STALAGMITE_CAVEMOSS = lateBound(BetterEnd.C.mk("end_stone_stalagmite_cavemoss"));
    public static final FeatureKey<WithFeature<StalactiteFeature>> END_STONE_WITH_CAVEMOSS_STALACTITE = lateBound(BetterEnd.C.mk("end_stone_with_cavemoss_stalactite"));
    public static final FeatureKey<WithFeature<StalactiteFeature>> END_STONE_WITH_CAVEMOSS_STALAGMITE = lateBound(BetterEnd.C.mk("end_stone_with_cavemoss_stalagmite"));

    public static final FeatureKey<WithFeature<CavePumpkinFeature>> CAVE_PUMPKIN = FeatureContentManager.withFeature(BetterEnd.C.mk("cave_pumpkin"), EndFeatures.CAVE_PUMPKIN_FEATURE);

    // WP4.3: dripstone-style stalactite clusters (own their per-column floor/ceiling placement).
    public static final FeatureKey<WithFeature<StalactiteClusterFeature>> STALACTITE_CLUSTER_PLAIN = lateBound(BetterEnd.C.mk("stalactite_cluster_plain"));
    public static final FeatureKey<WithFeature<StalactiteClusterFeature>> STALACTITE_CLUSTER_CAVEMOSS = lateBound(BetterEnd.C.mk("stalactite_cluster_cavemoss"));

    // Cave surface coat: the decoration-time replacement for the deleted CaveSurfaceCoater. One instance
    // for every cave biome and every face - the shell pass coats floor, wall and ceiling in one run, and
    // the materials come from the EndCaveBiome at placement time (the floor/ceiling pair this replaced
    // could only ever find floors and ceilings, which is why cave walls stayed End stone).
    public static final FeatureKey<WithFeature<CaveSurfaceCoatFeature>> CAVE_SURFACE_COAT = lateBound(BetterEnd.C.mk("cave_surface_coat"));

    // WP4.4: lush cave vegetation patches (vanilla VegetationPatchFeature, data-only).
    public static final FeatureKey<WithFeature<VegetationPatchFeature>> CAVE_LUSH_FLOOR_PATCH = lateBound(BetterEnd.C.mk("cave_lush_floor_patch"));
    public static final FeatureKey<WithFeature<VegetationPatchFeature>> CAVE_LUSH_CEILING_PATCH = lateBound(BetterEnd.C.mk("cave_lush_ceiling_patch"));

    /**
     * Creates a key whose {@link Feature} is supplied at bootstrap time rather than here.
     * <p>
     * {@code WithFeatureImpl} only validates the feature when the configurator builds it, so the key may
     * start out without one; datagen then calls {@code bootstrap(ctx).feature(...)}. This replaces 26.1's
     * {@code configuration(id, featureSingleton)} + {@code .configuration(config)} pair, which no longer has
     * a meaning now that an instance <em>is</em> the configuration.
     *
     * @param id  the id of the feature
     * @param <F> the feature type
     * @return the new key
     */
    private static <F extends Feature> FeatureKey<WithFeature<F>> lateBound(Identifier id) {
        return FeatureContentManager.withFeature(id, null);
    }
}
