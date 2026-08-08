package org.betterx.betterend.blocks;

import org.betterx.bclib.blocks.FeatureSaplingBlock;
import org.betterx.betterend.registry.features.EndConfiguredVegetation;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class MossyGlowshroomSaplingBlock extends FeatureSaplingBlock {
    public MossyGlowshroomSaplingBlock(BlockBehaviour.Properties properties) {
        super(
                properties,
                (level, pos, state, rnd) -> EndConfiguredVegetation.MOSSY_GLOWSHROOM.placeInWorld(level, pos, rnd)
        );
    }
}
