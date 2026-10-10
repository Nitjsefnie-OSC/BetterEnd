package org.betterx.betterend.trait.block;

import de.ambertation.wover.block.api.render.BlockRenderTraits;
import org.betterx.betterend.BetterEnd;
import de.ambertation.wover.block.api.BlockDefinition;
import de.ambertation.wover.block.api.model.ModelTraitLibrary;
import de.ambertation.wover.block.api.trait.BlockTraitKey;
import de.ambertation.wover.block.api.trait.BlockTraits;
import de.ambertation.wover.block.api.trait.GenericBlockTrait;
import de.ambertation.wover.block.impl.trait.BlockTraitImpl;
import de.ambertation.wover.tag.api.predefined.MineableTags;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class IceBlockTrait extends BlockTraitImpl<Block, GenericBlockTrait> {
    private static final BlockTraitKey KEY = BlockTraitKey.ofUnique(BetterEnd.C, "ice");

    public final Block baseIceBlock;

    public static IceBlockTrait withBase(Block baseIceBlock) {
        return new IceBlockTrait(baseIceBlock);
    }

    private IceBlockTrait(Block baseIceBlock) {
        super();
        this.baseIceBlock = baseIceBlock;
    }

    @Override
    public BlockTraitKey key() {
        return KEY;
    }

    @Override
    public void configure(BlockDefinition<Block, ? extends BlockDefinition<Block, ?>> definition) {
        definition
                .replacePropertiesWithCopy(baseIceBlock)
                .addTags(MineableTags.PICKAXE)
                .addTrait(BlockTraits.LOOT_TABLE.dropWithSilktouch())
                .addTrait(ModelTraitLibrary.cube());

        // Only clear ice is see-through. Packed and blue ice (the bases of dense and ancient emerald ice) are
        // solid in vanilla and their emerald textures are fully opaque; rendering them translucent made the
        // neighbouring translucent faces get culled away, leaving holes (#397).
        if (baseIceBlock == Blocks.ICE) {
            definition.addTrait(BlockRenderTraits.RENDER_LAYER.translucent());
        }
    }
}
