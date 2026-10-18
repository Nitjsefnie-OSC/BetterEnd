package org.betterx.betterend.world.features.terrain;

import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.SDFCoordModify;
import org.betterx.bclib.sdf.operator.SDFScale3D;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.MHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public class OreLayerFeature extends Feature<OreLayerFeatureConfig> {

    public OreLayerFeature() {
        super(OreLayerFeatureConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreLayerFeatureConfig> featureConfig) {
        final OreLayerFeatureConfig cfg = featureConfig.config();
        final RandomSource random = featureConfig.random();
        final BlockPos pos = featureConfig.origin();
        final WorldGenLevel world = featureConfig.level();
        float radius = cfg.radius * 0.5F;
        int r = MHelper.floor(radius + 1);
        int posX = MHelper.randRange(Math.max(r - 16, 0), Math.min(31 - r, 15), random) + pos.getX();
        int posZ = MHelper.randRange(Math.max(r - 16, 0), Math.min(31 - r, 15), random) + pos.getZ();
        int posY = MHelper.randRange(cfg.minY, cfg.maxY, random);


        // The SDF used to be one static graph whose radius, block and noise origin were re-set on every
        // placement - from every worldgen thread at once with parallel worldgen (C2ME), so a layer could be
        // placed with another layer's ore or position. Each placement builds its own graph now.
        SDF body = new SDFSphere().setRadius(radius).setBlock(cfg.state);
        body = new SDFScale3D().setScale(1, 0.2F, 1).setSource(body);
        body = new SDFCoordModify().setFunction((vec) -> {
            double x = (vec.x() + pos.getX()) * 0.1;
            double z = (vec.z() + pos.getZ()) * 0.1;
            double offset = cfg.getNoise(world.getSeed()).eval(x, z);
            vec.set(vec.x(), vec.y() + (float) offset * 8, vec.z());
        }).setSource(body);
        body.setReplaceFunction((state) -> state.is(Blocks.END_STONE));
        body.fillRecursive(world, new BlockPos(posX, posY, posZ));
        return true;
    }
}
