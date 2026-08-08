package org.betterx.betterend.blocks;

import org.betterx.bclib.blocks.FeatureSaplingBlock;
import org.betterx.betterend.registry.features.EndConfiguredVegetation;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class DragonTreeSaplingBlock extends FeatureSaplingBlock {
    public DragonTreeSaplingBlock(BlockBehaviour.Properties props) {
        super(props, (level, pos, state, rnd) -> EndConfiguredVegetation.DRAGON_TREE.placeInWorld(level, pos, rnd));
    }
}
