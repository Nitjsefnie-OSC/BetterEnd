package org.betterx.betterend.registry;


import org.betterx.betterend.registry.block.EndFunctionalBlocks;
import org.betterx.betterend.registry.block.EndStoneBlocks;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.blocks.EndStoneSmelter;
import org.betterx.betterend.blocks.FlowerPotBlock;
import org.betterx.betterend.blocks.basis.PedestalBlock;
import org.betterx.betterend.blocks.entities.*;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public class EndBlockEntities {
    public final static BlockEntityType<EndStoneSmelterBlockEntity> END_STONE_SMELTER = registerBlockEntity(
            EndStoneSmelter.ID,
            FabricBlockEntityTypeBuilder.create(EndStoneSmelterBlockEntity::new, EndFunctionalBlocks.END_STONE_SMELTER)
    );
    public final static BlockEntityType<PedestalBlockEntity> PEDESTAL = registerBlockEntity(
            "pedestal",
            FabricBlockEntityTypeBuilder.create(PedestalBlockEntity::new, getPedestals())
    );
    public final static BlockEntityType<EternalPedestalEntity> ETERNAL_PEDESTAL = registerBlockEntity(
            "eternal_pedestal",
            FabricBlockEntityTypeBuilder.create(EternalPedestalEntity::new, EndFunctionalBlocks.ETERNAL_PEDESTAL)
    );
    public final static BlockEntityType<InfusionPedestalEntity> INFUSION_PEDESTAL = registerBlockEntity(
            "infusion_pedestal",
            FabricBlockEntityTypeBuilder.create(InfusionPedestalEntity::new, EndFunctionalBlocks.INFUSION_PEDESTAL)
    );
    public final static BlockEntityType<BlockEntityHydrothermalVent> HYDROTHERMAL_VENT = registerBlockEntity(
            "hydrother_malvent",
            FabricBlockEntityTypeBuilder.create(BlockEntityHydrothermalVent::new, EndStoneBlocks.HYDROTHERMAL_VENT)
    );
    public final static BlockEntityType<FlowerPotBlockEntity> FLOWER_POT = registerBlockEntity(
            "flower_pot",
            FabricBlockEntityTypeBuilder.create(FlowerPotBlockEntity::new, getFlowerPots())
    );

    public static <T extends BlockEntity> BlockEntityType<T> registerBlockEntity(
            String id,
            FabricBlockEntityTypeBuilder<T> builder
    ) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, BetterEnd.C.mk(id), builder.build(null));

        //return Registry.register(Registry.BLOCK_ENTITY_TYPE, BetterEnd.makeID(id), builder.build(null));
    }

    public static void register() {
    }

    /**
     * The types above are built when this class is loaded, which {@code BetterEnd.onInitialize} does
     * before {@link EndBlocks#ensureStaticallyLoaded()}. At that point only the block classes they
     * reference (and whatever those pull in) have registered their blocks, so {@link #getPedestals()}
     * and {@link #getFlowerPots()} missed every pedestal or pot defined elsewhere - e.g. the smaragdant
     * crystal pedestal from {@code EndCrystalBlocks} - and placing one failed to create its block entity.
     * Called once all blocks are registered; re-adding a block that is already valid is a no-op.
     */
    public static void addLateRegisteredBlocks() {
        for (Block block : getPedestals()) {
            PEDESTAL.addSupportedBlock(block);
        }
        for (Block block : getFlowerPots()) {
            FLOWER_POT.addSupportedBlock(block);
        }
    }

    static Block[] getPedestals() {
        return EndBlocks.getModBlocks()
                        .stream()
                        .filter(block -> block instanceof PedestalBlock && !((PedestalBlock) block).hasUniqueEntity())
                        .toArray(Block[]::new);
    }

    static Block[] getFlowerPots() {
        return EndBlocks.getModBlocks()
                        .stream()
                        .filter(block -> block instanceof FlowerPotBlock)
                        .toArray(Block[]::new);
    }
}
