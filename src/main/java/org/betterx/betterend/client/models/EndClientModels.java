package org.betterx.betterend.client.models;

import org.betterx.betterend.blocks.BulbVineLanternBlock;
import org.betterx.betterend.blocks.ChandelierBlock;
import org.betterx.betterend.blocks.EndPedestal;
import org.betterx.betterend.blocks.FlowerPotBlock;
import org.betterx.betterend.blocks.basis.PedestalBlock;
import org.betterx.betterend.blocks.basis.StoneLanternBlock;
import org.betterx.betterend.complexmaterials.CrystalSubblocksMaterial;
import org.betterx.betterend.integration.FlamboyantRefabricatedIntegration;
import de.ambertation.wover.client.api.WoverClientTraitEntrypoint;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Registers the client generators behind BetterEnd's model keys. Runs through the {@code wover.client.traits}
 * entrypoint, so the generators are present in both the client and the datagen launch.
 */
@Environment(EnvType.CLIENT)
public class EndClientModels implements WoverClientTraitEntrypoint {
    @Override
    public void registerClientTraits() {
        BulbVineLanternBlock.registerClientModels();
        ChandelierBlock.registerClientModels();
        EndPedestal.registerClientModels();
        FlowerPotBlock.registerClientModels();
        PedestalBlock.registerClientModels();
        StoneLanternBlock.registerClientModels();
        EndModelTraits.registerClientModels();
        CrystalSubblocksMaterial.registerClientModels();
        FlamboyantRefabricatedIntegration.registerClientModels();
    }
}
