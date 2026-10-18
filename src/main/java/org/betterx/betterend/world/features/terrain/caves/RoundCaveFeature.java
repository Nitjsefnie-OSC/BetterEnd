package org.betterx.betterend.world.features.terrain.caves;

import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.noise.OpenSimplexNoise;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.stream.IntStream;

public class RoundCaveFeature extends EndCaveFeature {
    @Override
    protected Set<BlockPos> generate(WorldGenLevel world, BlockPos center, int radius, RandomSource random) {
        OpenSimplexNoise noise = new OpenSimplexNoise(MHelper.getSeed(534, center.getX(), center.getZ()));

        // Only touch the chunks this feature may access; other worldgen threads may be generating the ones further away
        int x1 = Math.max(center.getX() - radius - 5, GenerationArea.minX(world));
        int z1 = Math.max(center.getZ() - radius - 5, GenerationArea.minZ(world));
        int x2 = Math.min(center.getX() + radius + 5, GenerationArea.maxX(world));
        int z2 = Math.min(center.getZ() + radius + 5, GenerationArea.maxZ(world));
        int y1 = MHelper.floor(center.getY() - (radius + 5) / 1.6);
        int y2 = MHelper.floor(center.getY() + (radius + 5) / 1.6);

        double hr = radius * 0.75;
        double nr = radius * 0.25;

        int dx = x2 - x1 + 1;
        int dz = z2 - z1 + 1;
        int count = dx * dz;
        final CaveSession session = session(world);
        Set<BlockPos> blocks = Sets.newConcurrentHashSet();
        IntStream.range(0, count).parallel().forEach(index -> {
            MutableBlockPos bpos = new MutableBlockPos();
            int x = (index % dx) + x1;
            int z = (index / dx) + z1;
            bpos.setX(x);
            bpos.setZ(z);
            int xsq = MHelper.sqr(x - center.getX());
            int zsq = MHelper.sqr(z - center.getZ());
            int dxz = xsq + zsq;
            BlockState state;
            for (int y = y1; y <= y2; y++) {
                int ysq = (int) MHelper.sqr((y - center.getY()) * 1.6);
                double r = noise.eval(x * 0.1, y * 0.1, z * 0.1) * nr + hr;
                double dist = dxz + ysq;
                if (dist < r * r) {
                    bpos.setY(y);
                    state = world.getBlockState(bpos);
                    if (isReplaceable(session, state, bpos) && !isWaterNear(world, bpos)) {
                        blocks.add(bpos.immutable());

                        while (state.is(BlockTags.LEAVES)) {
                            bpos.setY(bpos.getY() + 1);
                            state = world.getBlockState(bpos);
                        }

                        bpos.setY(y - 1);
                        while (state.is(BlockTags.LEAVES)) {
                            bpos.setY(bpos.getY() - 1);
                            state = world.getBlockState(bpos);
                        }
                    }
                }
            }
        });
        blocks.forEach(bpos -> {
            BlocksHelper.setWithoutUpdate(world, bpos, CAVE_AIR);
            session.markCarved(bpos);
        });

        return blocks;
    }

    private boolean isReplaceable(CaveSession session, BlockState state, BlockPos pos) {
        return session.isCaveStone(state, pos) ||
                BlocksHelper.replaceableOrPlant(state) ||
                state.is(BlockTags.LEAVES);
    }
}
