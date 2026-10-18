package org.betterx.betterend.world.features.trees;

import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.sdf.SDF;
import org.betterx.bclib.sdf.operator.*;
import org.betterx.bclib.sdf.primitive.SDFCappedCone;
import org.betterx.bclib.sdf.primitive.SDFSphere;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.MHelper;
import org.betterx.bclib.util.SplineHelper;
import org.betterx.betterend.blocks.MossyGlowshroomCapBlock;
import org.betterx.betterend.blocks.basis.FurBlock;
import org.betterx.betterend.noise.OpenSimplexNoise;
import org.betterx.betterend.registry.EndBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

public class MossyGlowshroomFeature extends DefaultFeature {
    private static final Function<BlockState, Boolean> REPLACE;
    private static final OpenSimplexNoise CAP_NOISE = new OpenSimplexNoise(1234);

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
        final RandomSource random = featureConfig.random();
        final BlockPos blockPos = featureConfig.origin();
        final WorldGenLevel world = featureConfig.level();
        BlockState down = world.getBlockState(blockPos.below());
        if (!down.is(EndBlocks.END_MYCELIUM) && !down.is(EndBlocks.END_MOSS)) return false;

        float height = MHelper.randRange(10F, 25F, random);
        int count = MHelper.floor(height / 4);
        List<Vector3f> spline = SplineHelper.makeSpline(0, 0, 0, 0, height, 0, count);
        SplineHelper.offsetParts(spline, random, 1F, 0, 1F);
        SDF sdf = SplineHelper.buildSDF(spline, 2.1F, 1.5F, (pos) -> {
            return EndBlocks.MOSSY_GLOWSHROOM.getLog().defaultBlockState();
        });
        Vector3f pos = spline.get(spline.size() - 1);
        float scale = MHelper.randRange(0.75F, 1.1F, random);

        if (!SplineHelper.canGenerate(spline, scale, blockPos, world, REPLACE)) {
            return false;
        }
        BlocksHelper.setWithoutUpdate(world, blockPos, AIR);

        // The cap/roots shape used to be one static SDF graph whose translate, angle, centre and trunk were
        // re-set on every placement - by every worldgen thread at once with parallel worldgen (C2ME), so a
        // mushroom could get another mushroom's trunk or cap position. Each placement builds its own graph now.
        final SDFBinary function = buildShape(
                new Vector3f(blockPos.getX(), 0, blockPos.getZ()),
                pos,
                random.nextFloat() * MHelper.PI2
        );
        function.setSourceA(sdf);

        new SDFScale().setScale(scale).setSource(function).setReplaceFunction(REPLACE).addPostProcess((info) -> {
            if (EndBlocks.MOSSY_GLOWSHROOM.isTreeLog(info.getState())) {
                if (random.nextBoolean() && info.getStateUp().getBlock() == EndBlocks.MOSSY_GLOWSHROOM_CAP) {
                    info.setState(EndBlocks.MOSSY_GLOWSHROOM_CAP.defaultBlockState()
                                                                .setValue(MossyGlowshroomCapBlock.TRANSITION, true));
                    return info.getState();
                } else if (!EndBlocks.MOSSY_GLOWSHROOM.isTreeLog(info.getStateUp()) || !EndBlocks.MOSSY_GLOWSHROOM.isTreeLog(
                        info.getStateDown())) {
                    info.setState(EndBlocks.MOSSY_GLOWSHROOM.getBark().defaultBlockState());
                    return info.getState();
                }
            } else if (info.getState().getBlock() == EndBlocks.MOSSY_GLOWSHROOM_CAP) {
                if (EndBlocks.MOSSY_GLOWSHROOM.isTreeLog(info.getStateDown().getBlock())) {
                    info.setState(EndBlocks.MOSSY_GLOWSHROOM_CAP.defaultBlockState()
                                                                .setValue(MossyGlowshroomCapBlock.TRANSITION, true));
                    return info.getState();
                }

                info.setState(EndBlocks.MOSSY_GLOWSHROOM_CAP.defaultBlockState());
                return info.getState();
            } else if (info.getState().getBlock() == EndBlocks.MOSSY_GLOWSHROOM_HYMENOPHORE) {
                for (Direction dir : BlocksHelper.HORIZONTAL) {
                    if (info.getState(dir) == AIR) {
                        info.setBlockPos(
                                info.getPos().relative(dir),
                                EndBlocks.MOSSY_GLOWSHROOM_FUR.defaultBlockState().setValue(FurBlock.FACING, dir)
                        );
                    }
                }

                if (info.getStateDown().getBlock() != EndBlocks.MOSSY_GLOWSHROOM_HYMENOPHORE) {
                    info.setBlockPos(
                            info.getPos().below(),
                            EndBlocks.MOSSY_GLOWSHROOM_FUR.defaultBlockState().setValue(FurBlock.FACING, Direction.DOWN)
                    );
                }
            }
            return info.getState();
        }).fillRecursive(world, blockPos);

        return true;
    }

    private static SDFBinary buildShape(Vector3f center, Vector3f headPos, float rootsAngle) {
        SDFCappedCone cone1 = new SDFCappedCone().setHeight(2.5F).setRadius1(1.5F).setRadius2(2.5F);
        SDFCappedCone cone2 = new SDFCappedCone().setHeight(3F).setRadius1(2.5F).setRadius2(13F);
        cone1.setBlock(EndBlocks.MOSSY_GLOWSHROOM_CAP);
        cone2.setBlock(EndBlocks.MOSSY_GLOWSHROOM_CAP);
        SDF posedCone2 = new SDFTranslate().setTranslate(0, 5, 0).setSource(cone2);
        SDF posedCone3 = new SDFTranslate().setTranslate(0, 12F, 0)
                                           .setSource(new SDFScale().setScale(2).setSource(cone2));
        SDF upCone = new SDFSubtraction().setSourceA(posedCone2).setSourceB(posedCone3);
        SDF wave = new SDFFlatWave().setRaysCount(12).setIntensity(1.3F).setSource(upCone);
        SDF cones = new SDFSmoothUnion().setRadius(3).setSourceA(cone1).setSourceB(wave);

        SDF innerCone = new SDFTranslate().setTranslate(0, 1.25F, 0).setSource(upCone);
        innerCone = new SDFScale3D().setScale(1.2F, 1F, 1.2F).setSource(innerCone);
        cones = new SDFUnion().setSourceA(cones).setSourceB(innerCone);

        SDFCappedCone coneGlow = new SDFCappedCone().setHeight(3F).setRadius1(2F).setRadius2(12.5F);
        coneGlow.setBlock(EndBlocks.MOSSY_GLOWSHROOM_HYMENOPHORE);
        SDF glowCone = new SDFTranslate().setTranslate(0, 4.25F, 0).setSource(coneGlow);
        glowCone = new SDFSubtraction().setSourceA(glowCone).setSourceB(posedCone3);

        cones = new SDFUnion().setSourceA(cones).setSourceB(glowCone);

        cones = new SDFCoordModify().setFunction((pos) -> {
            float dist = MHelper.length(pos.x(), pos.z());
            float y = pos.y() + (float) CAP_NOISE.eval(
                    pos.x() * 0.1 + center.x(),
                    pos.z() * 0.1 + center.z()
            ) * dist * 0.3F - dist * 0.15F;
            pos.set(pos.x(), y, pos.z());
        }).setSource(cones);

        SDFTranslate head = (SDFTranslate) new SDFTranslate().setSource(new SDFTranslate().setTranslate(0, 2.5F, 0)
                                                                                         .setSource(cones));
        head.setTranslate(headPos.x(), headPos.y(), headPos.z());

        SDFSphere rootsSphere = new SDFSphere().setRadius(4F);
        rootsSphere.setBlock(EndBlocks.MOSSY_GLOWSHROOM.getBark());
        SDF roots = new SDFScale3D().setScale(1, 0.7F, 1).setSource(rootsSphere);
        SDFFlatWave rootsRot = (SDFFlatWave) new SDFFlatWave().setRaysCount(5).setIntensity(1.5F).setSource(roots);
        rootsRot.setAngle(rootsAngle);

        return new SDFSmoothUnion().setRadius(4).setSourceB(new SDFUnion().setSourceA(head).setSourceB(rootsRot));
    }

    static {
        REPLACE = BlocksHelper::replaceableOrPlant;
    }
}
