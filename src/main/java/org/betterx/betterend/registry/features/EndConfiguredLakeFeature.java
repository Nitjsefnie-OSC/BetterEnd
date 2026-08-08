package org.betterx.betterend.registry.features;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.world.features.terrain.SulphuricLakeFeature;
import de.ambertation.wover.feature.api.configured.FeatureKey;
import de.ambertation.wover.feature.api.configured.FeatureContentManager;
import de.ambertation.wover.feature.api.configured.configurators.WithFeature;

public class EndConfiguredLakeFeature {
    public static final FeatureKey<WithFeature<SulphuricLakeFeature>> SULPHURIC_LAKE = FeatureContentManager
            .withFeature(BetterEnd.C.mk("sulphuric_lake"), EndFeatures.SULPHURIC_LAKE_FEATURE);
}
