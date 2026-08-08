package org.betterx.betterend.blocks;

import org.betterx.bclib.blocks.FeatureSaplingBlock;
import org.betterx.betterend.registry.features.EndConfiguredVegetation;

public class UmbrellaTreeSaplingBlock extends FeatureSaplingBlock {
    public UmbrellaTreeSaplingBlock(Properties props) {
        super(props, (level, pos, state, rnd) -> EndConfiguredVegetation.UMBRELLA_TREE.placeInWorld(level, pos, rnd));
    }
}
