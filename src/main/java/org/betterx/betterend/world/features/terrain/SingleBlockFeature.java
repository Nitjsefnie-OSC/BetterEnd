package org.betterx.betterend.world.features.terrain;

import org.betterx.bclib.util.BlocksHelper;
import de.ambertation.wover.tag.api.predefined.CommonBlockTags;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Places a single block on End stone, waterlogging it when it sits in a fluid.
 * <p>
 * 26.3 collapsed {@code ConfiguredFeature} into {@link Feature} and deleted the
 * {@code ...feature.configurations} package, which is where {@code SimpleBlockConfiguration} lived. Its only
 * field is therefore held directly here, under the {@code to_place} name that vanilla's replacement
 * {@code SimpleBlockFeature} uses, so the generated JSON keeps the same key - it just sits next to
 * {@code "type"} instead of inside a {@code "config"} object.
 */
public class SingleBlockFeature implements Feature {
    public static final MapCodec<SingleBlockFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
            .group(
                    BlockStateProvider.CODEC.fieldOf("to_place").forGetter(o -> o.toPlace)
            )
            .apply(instance, SingleBlockFeature::new));

    public final BlockStateProvider toPlace;

    public SingleBlockFeature(BlockStateProvider toPlace) {
        this.toPlace = toPlace;
    }

    @Override
    public MapCodec<SingleBlockFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(
            WorldGenLevel level,
            ChunkGenerator generator,
            RandomSource random,
            BlockPos origin
    ) {
        if (!level.getBlockState(origin.below()).is(CommonBlockTags.END_STONES)) {
            return false;
        }

        BlockState state = toPlace.getState(level, random, origin);
        if (state.getBlock().getStateDefinition().getProperty("waterlogged") != null) {
            boolean waterlogged = !level.getFluidState(origin).isEmpty();
            state = state.setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        }
        BlocksHelper.setWithoutUpdate(level, origin, state);

        return true;
    }
}
