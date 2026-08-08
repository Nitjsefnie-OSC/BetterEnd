package org.betterx.betterend.blocks;

import org.betterx.bclib.blocks.FeatureSaplingBlock;
import org.betterx.betterend.registry.features.EndConfiguredVegetation;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class PythadendronSaplingBlock extends FeatureSaplingBlock {
    public PythadendronSaplingBlock(BlockBehaviour.Properties props) {
        super(
                props,
                (level, pos, state, rnd)
                        -> EndConfiguredVegetation.PYTHADENDRON_TREE.placeInWorld(level, pos, rnd)
        );
    }
}
