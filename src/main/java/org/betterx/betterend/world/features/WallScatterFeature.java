package org.betterx.betterend.world.features;

import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public abstract class WallScatterFeature<FC extends ScatterFeatureConfig> extends Feature<FC> {
    private static final Direction[] DIR = BlocksHelper.makeHorizontal();

    public WallScatterFeature(Codec<FC> codec) {
        super(codec);
    }

    public abstract boolean canGenerate(FC cfg, WorldGenLevel world, RandomSource random, BlockPos pos, Direction dir);

    public abstract void generate(FC cfg, WorldGenLevel world, RandomSource random, BlockPos pos, Direction dir);

    @Override
    public boolean place(FeaturePlaceContext<FC> featureConfig) {
        FC cfg = featureConfig.config();
        final RandomSource random = featureConfig.random();
        final BlockPos center = featureConfig.origin();
        final WorldGenLevel world = featureConfig.level();
        int maxY = world.getHeight(Heightmap.Types.WORLD_SURFACE, center.getX(), center.getZ());
        int minY = BlocksHelper.upRay(world, new BlockPos(center.getX(), 0, center.getZ()), maxY);
        if (maxY < 10 || maxY < minY) {
            return false;
        }
        int py = MHelper.randRange(minY, maxY, random);

        // WallPlantFeature keeps the plant chosen in canGenerate() in a field for generate() to use, and the
        // feature instance is shared by every worldgen thread. Unsynchronized, a second thread could swap
        // that field between the two calls, and FACING was set on a block without it. Same lock as
        // ScatterFeature.place(). The direction order is shuffled in a per-call copy: the static DIR used
        // to be shuffled in place by every thread at once, which could lose a direction for good.
        final Direction[] dirs = DIR.clone();
        synchronized (this) {
            MutableBlockPos mut = new MutableBlockPos();
            for (int x = -cfg.radius; x <= cfg.radius; x++) {
                mut.setX(center.getX() + x);
                for (int y = -cfg.radius; y <= cfg.radius; y++) {
                    mut.setY(py + y);
                    for (int z = -cfg.radius; z <= cfg.radius; z++) {
                        mut.setZ(center.getZ() + z);
                        // plants (and the blocks next to them) stay inside the chunks this feature may access
                        if (random.nextInt(4) == 0 && GenerationArea.contains(world, mut, 1) && world.isEmptyBlock(mut)) {
                            shuffle(random, dirs);
                            for (Direction dir : dirs) {
                                if (canGenerate(cfg, world, random, mut, dir)) {
                                    generate(cfg, world, random, mut, dir);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private static void shuffle(RandomSource random, Direction[] dirs) {
        for (int i = 0; i < 4; i++) {
            int j = random.nextInt(4);
            Direction d = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = d;
        }
    }
}
