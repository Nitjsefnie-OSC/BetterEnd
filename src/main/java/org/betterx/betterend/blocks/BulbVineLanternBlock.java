package org.betterx.betterend.blocks;

import org.betterx.bclib.client.models.BCLModels;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.blocks.basis.EndLanternBlock;
import org.betterx.betterend.client.models.EndModelKeys;
import de.ambertation.wover.block.api.client.model.ClientBlockModelRegistry;
import de.ambertation.wover.block.api.client.trait.BlockModelTrait.ModelFactory;
import de.ambertation.wover.block.api.model.BlockModelBinding;
import de.ambertation.wover.block.api.model.ModelKey;
import de.ambertation.wover.block.api.trait.BlockTraitLookup;
import de.ambertation.wover.sets.api.blocks.BlockSet;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class BulbVineLanternBlock extends EndLanternBlock {
    private static final VoxelShape SHAPE_CEIL = Block.box(4, 4, 4, 12, 16, 12);
    private static final VoxelShape SHAPE_FLOOR = Block.box(4, 0, 4, 12, 12, 12);

    public BulbVineLanternBlock(Properties settings) {
        super(settings);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext ePos) {
        return state.getValue(IS_FLOOR) ? SHAPE_FLOOR : SHAPE_CEIL;
    }

    protected static String getMetalTexture(ResourceLocation blockId) {
        String name = blockId.getPath();
        name = name.substring(0, name.indexOf('_'));
        return name + "_bulb_vine_lantern_metal";
    }

    protected String getGlowTexture() {
        return "bulb_vine_lantern_bulb";
    }

    /**
     * Kept in a separate class file (not just an @Environment(CLIENT) method) since this Block is
     * always loaded on the server; a lambda body's synthetic method does not inherit the
     * annotation from its enclosing method, so leaving it here would strand vanilla client-only
     * type references in a class file the server actually has to verify. See PathBlockTrait/
     * StoneLanternBlock for the same fix applied elsewhere.
     */
    public static BlockModelBinding buildModel(BlockSet<?> set, BlockTraitLookup traitLookup) {
        return BlockModelBinding.of(MODEL, null);
    }

    private static final ModelKey<Void> MODEL = EndModelKeys.key("bulb_vine_lantern_block");

    /**
     * Registers the client generators behind this class's model keys. Called from the mod's
     * {@code wover.client.traits} entrypoint.
     */
    @Environment(EnvType.CLIENT)
    public static void registerClientModels() {
        ClientModel.register();
    }

    @Environment(EnvType.CLIENT)
    private static class ClientModel {
        private static void register() {
            ClientBlockModelRegistry.register(
                    MODEL,
                    (key, block, generator, payload) -> build().provideBlockModels(key, block, generator)
            );
        }


        private static ModelFactory build() {
            return (key, block, generator) -> {
                //get id of this block from registry
                final var id = BuiltInRegistries.BLOCK.getKey(block);

                final var mapping = new TextureMapping()
                        .put(BCLModels.GLOW, BetterEnd.C.mk("bulb_vine_lantern_bulb").withPrefix("block/"))
                        .put(BCLModels.METAL, BetterEnd.C.mk(getMetalTexture(id)).withPrefix("block/"));

                final var floorModel = BCLModels.BULB_LANTERN_FLOOR.createWithSuffix(
                        block,
                        "_floor",
                        mapping,
                        generator.vanillaGenerator.modelOutput
                );
                final var ceilModel = BCLModels.BULB_LANTERN_CEIL.create(
                        block,
                        mapping,
                        generator.vanillaGenerator.modelOutput
                );

                final var floorCeilDispatch = PropertyDispatch
                        .modify(IS_FLOOR)
                        .select(
                                true,
                                (variant) -> BlockModelGenerators.plainModel(floorModel)
                        )
                        .select(
                                false,
                                (variant) -> BlockModelGenerators.plainModel(ceilModel)
                        );

                generator.acceptBlockState(MultiVariantGenerator
                        .dispatch(block, BlockModelGenerators.plainVariant(ceilModel))
                        .with(floorCeilDispatch));
                generator.delegateItemModel(block, floorModel);
            };
        }
    }
}
