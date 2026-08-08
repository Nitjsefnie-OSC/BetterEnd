package org.betterx.betterend.blocks;

import org.betterx.ui.ColorUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class EndstoneDustBlock extends FallingBlock {
    private static final int COLOR = ColorUtil.color(226, 239, 168);

    // 26.3 removed block codecs entirely: Block#codec()/BlockBehaviour#simpleCodec no longer exist,
    // because blocks are no longer described by a dispatch codec. The CODEC constant and the codec()
    // override are dropped, not replaced.

    public EndstoneDustBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Environment(EnvType.CLIENT)
    public int getDustColor(BlockState state, BlockGetter world, BlockPos pos) {
        return COLOR;
    }
}
