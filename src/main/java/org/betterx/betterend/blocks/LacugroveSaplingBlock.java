package org.betterx.betterend.blocks;

import org.betterx.bclib.blocks.FeatureSaplingBlock;
import org.betterx.betterend.registry.features.EndConfiguredVegetation;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class LacugroveSaplingBlock extends FeatureSaplingBlock {
    public LacugroveSaplingBlock(BlockBehaviour.Properties props) {
        super(
                props,
                (level, pos, state, rnd)
                        -> EndConfiguredVegetation.LACUGROVE.placeInWorld(level, pos, rnd)
        );
    }
}
