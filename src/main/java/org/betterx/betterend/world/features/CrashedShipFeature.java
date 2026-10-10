package org.betterx.betterend.world.features;

import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.util.MHelper;
import org.betterx.bclib.util.StructureErode;
import de.ambertation.wover.structure.api.StructureNBT;
import org.betterx.betterend.util.BlockFixer;
import de.ambertation.wover.feature.api.WriteZone;
import de.ambertation.wover.tag.api.predefined.CommonBlockTags;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;


public class CrashedShipFeature extends NBTFeature<NBTFeatureConfig> {
    private static final StructureProcessor REPLACER;
    private static final Identifier STRUCTURE_ID = Identifier.withDefaultNamespace("end_city/ship");
    private StructureTemplate structure;

    public CrashedShipFeature() {
        super(NBTFeatureConfig.CODEC);
    }

    @Override
    protected StructureTemplate getStructure(
            NBTFeatureConfig cfg,
            WorldGenLevel world,
            BlockPos pos,
            RandomSource random
    ) {
        if (structure == null) {
            structure = world
                    .getLevel()
                    .getStructureManager()
                    .getOrCreate(STRUCTURE_ID);
            if (structure == null) {
                structure = StructureNBT.readTemplate(STRUCTURE_ID);
            }
        }
        return structure;
    }

    @Override
    protected boolean canSpawn(WorldGenLevel world, BlockPos pos, RandomSource random) {
        long x = pos.getX() >> 4;
        long z = pos.getX() >> 4;
        if (x * x + z * z < 3600) {
            return false;
        }
        return pos.getY() > 5 && world.getBlockState(pos.below()).is(CommonBlockTags.END_STONES);
    }

    @Override
    protected Rotation getRotation(WorldGenLevel world, BlockPos pos, RandomSource random) {
        return Rotation.getRandom(random);
    }

    @Override
    protected Mirror getMirror(WorldGenLevel world, BlockPos pos, RandomSource random) {
        return Mirror.values()[random.nextInt(3)];
    }

    @Override
    protected int getYOffset(StructureTemplate structure, WorldGenLevel world, BlockPos pos, RandomSource random) {
        int min = structure.getSize().getY() >> 3;
        int max = structure.getSize().getY() >> 2;
        return -MHelper.randRange(min, max, random);
    }

    @Override
    protected TerrainMerge getTerrainMerge(WorldGenLevel world, BlockPos pos, RandomSource random) {
        return TerrainMerge.NONE;
    }

    @Override
    public boolean place(FeaturePlaceContext<NBTFeatureConfig> featureConfig) {
        final RandomSource random = featureConfig.random();
        BlockPos center = featureConfig.origin();
        final WorldGenLevel world = featureConfig.level();
        center = new BlockPos(((center.getX() >> 4) << 4) | 8, 128, ((center.getZ() >> 4) << 4) | 8);
        center = getGround(world, center);
        BoundingBox bounds = makeBox(center);

        if (!canSpawn(world, center, random)) {
            return false;
        }

        StructureTemplate structure = getStructure(featureConfig.config(), world, center, random);
        Rotation rotation = getRotation(world, center, random);
        Mirror mirror = getMirror(world, center, random);
        BlockPos offset = StructureTemplate.transform(
                new BlockPos(structure.getSize()),
                mirror,
                rotation,
                BlockPos.ZERO
        );
        center = center.offset(0, (int) (getYOffset(structure, world, center, random) + 0.5), 0);
        StructurePlaceSettings placementData = new StructurePlaceSettings().setRotation(rotation).setMirror(mirror);
        center = center.offset((int) (-offset.getX() * 0.5), 0, (int) (-offset.getZ() * 0.5));

        BoundingBox structB = structure.getBoundingBox(placementData, center);
        bounds = intersectBoxes(bounds, structB);

        addStructureData(placementData);
        structure.placeInWorld(world, center, center, placementData.setBoundingBox(bounds), random, 2);

        StructureErode.erodeIntense(world, bounds, random);
        // makeBox() is exactly the 3x3 chunks a feature may touch, and BlockFixer peeks one block sideways
        // from every column it scans - keep that scan inside the write zone. See WriteZone.
        BlockFixer.fixBlocksInZone(
                world,
                new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
                new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()),
                WriteZone.of(world)
        );

        return true;
    }

    @Override
    protected void addStructureData(StructurePlaceSettings data) {
        data.addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR).addProcessor(REPLACER).setIgnoreEntities(true);
    }

    /**
     * The codec {@link #REPLACER} hands back from {@code codec()}.
     * <p>
     * 26.2 removed {@code KeyDispatchDataCodec} and turned {@link StructureProcessor} into an interface
     * whose only abstract method is {@code codec()}, returning a plain {@link MapCodec}. Up to 26.1 this
     * processor returned {@code StructureProcessorType.NOP}, which was only ever a placeholder - it is
     * built here in Java, is never serialized and is not registered in
     * {@code BuiltInRegistries.STRUCTURE_PROCESSOR_TYPE}. The processor is stateless, so a unit codec is
     * the exact equivalent; it still cannot round-trip through JSON until someone registers the type.
     * <p>
     * The supplier overload is used deliberately: this constant is initialised before the
     * {@link #REPLACER} static block runs, so capturing the value eagerly would capture {@code null}. The
     * supplier goes through {@link #replacer()} rather than reading the field directly, because a blank
     * {@code static final} is not definitely assigned yet at this point in the initializer sequence and
     * javac rejects a simple-name read of it - even from inside a lambda body.
     */
    private static final MapCodec<StructureProcessor> REPLACER_CODEC = MapCodec.unit(CrashedShipFeature::replacer);

    private static StructureProcessor replacer() {
        return REPLACER;
    }

    static {
        REPLACER = new StructureProcessor() {
            /**
             * 26.2 replaced the original {@code StructureBlockInfo} parameter with just its position.
             * Nothing is lost here: this processor only ever read {@code structureBlockInfo2}, the
             * already-processed info.
             */
            @Override
            public StructureBlockInfo processBlock(
                    LevelReader worldView,
                    BlockPos pos,
                    BlockPos blockPos,
                    BlockPos originalPos,
                    StructureBlockInfo structureBlockInfo2,
                    StructurePlaceSettings structurePlacementData
            ) {
                BlockState state = structureBlockInfo2.state();
                if (state.is(Blocks.SPAWNER) || state.getSoundType() == SoundType.WOOL) {
                    return new StructureBlockInfo(structureBlockInfo2.pos(), DefaultFeature.AIR, null);
                }
                return structureBlockInfo2;
            }

            @Override
            public MapCodec<? extends StructureProcessor> codec() {
                return REPLACER_CODEC;
            }
        };
    }

    private static BoundingBox intersectBoxes(BoundingBox box1, BoundingBox box2) {
        int x1 = MHelper.max(box1.minX(), box2.minX());
        int y1 = MHelper.max(box1.minY(), box2.minY());
        int z1 = MHelper.max(box1.minZ(), box2.minZ());

        int x2 = MHelper.min(box1.maxX(), box2.maxX());
        int y2 = MHelper.min(box1.maxY(), box2.maxY());
        int z2 = MHelper.min(box1.maxZ(), box2.maxZ());

        return BoundingBox.fromCorners(new Vec3i(x1, y1, z1), new Vec3i(x2, y2, z2));
    }
}
