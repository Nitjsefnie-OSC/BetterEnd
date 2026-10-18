package org.betterx.betterend.world.features.terrain.caves;

import org.betterx.bclib.api.v2.generator.BiomePicker;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiome;
import org.betterx.bclib.api.v2.levelgen.biomes.BCLBiomeRegistry;
import org.betterx.bclib.api.v2.levelgen.biomes.BiomeAPI;
import org.betterx.bclib.api.v2.levelgen.features.features.DefaultFeature;
import org.betterx.bclib.config.Configs;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.GenerationArea;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.EndBiomes;
import org.betterx.betterend.util.BlockFixer;
import org.betterx.betterend.world.biome.EndBiome;
import org.betterx.betterend.world.biome.cave.EndCaveBiome;
import org.betterx.worlds.together.tag.v3.CommonBlockTags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class EndCaveFeature extends DefaultFeature {
    private static int errCounter = 0;
    protected static final BlockState CAVE_AIR = Blocks.CAVE_AIR.defaultBlockState();
    protected static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
    protected static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final Vec3i[] SPHERE;

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> featureConfig) {
        final RandomSource random = featureConfig.random();
        final BlockPos pos = featureConfig.origin();
        final WorldGenLevel world = featureConfig.level();
        if (pos.getX() * pos.getX() + pos.getZ() * pos.getZ() <= 2500) {
            return false;
        }

        if (biomeMissingCaves(world, pos)) {
            return false;
        }

        int radius = MHelper.randRange(10, 30, random);
        BlockPos center = findPos(world, featureConfig.chunkGenerator(), pos, radius, random);

        if (center == null) {
            return false;
        }

        BiomePicker.ActualBiome biome = EndBiomes.getCaveBiome(pos.getX(), pos.getZ());
        Set<BlockPos> caveBlocks = inPositionOrder(generate(world, center, radius, random));
        if (!caveBlocks.isEmpty()) {
            if (biome != null) {
                ChunkGenerator generator = featureConfig.chunkGenerator();
                setBiomes(world, biome, caveBlocks);
                Set<BlockPos> floorFound = Sets.newConcurrentHashSet();
                Set<BlockPos> ceilFound = Sets.newConcurrentHashSet();
                caveBlocks.parallelStream().forEach((bpos) -> {
                    if (world.getBlockState(bpos).canBeReplaced()) {
                        BlockPos side = bpos.below();
                        if (world.getBlockState(side).is(CommonBlockTags.GEN_END_STONES)) {
                            floorFound.add(side);
                        }
                        side = bpos.above();
                        if (world.getBlockState(side).is(CommonBlockTags.GEN_END_STONES)) {
                            ceilFound.add(side);
                        }
                    }
                });

                final Set<BlockPos> floorPositions = inPositionOrder(floorFound);
                final Set<BlockPos> ceilPositions = inPositionOrder(ceilFound);

                BlockState surfaceBlock = EndBiome.findTopMaterial(biome.bclBiome);
                if (biome.bclBiome instanceof EndCaveBiome caveBiome) {
                    placeFloor(world, generator, (EndCaveBiome) biome.bclBiome, floorPositions, random, surfaceBlock);
                    placeCeil(world, generator, (EndCaveBiome) biome.bclBiome, ceilPositions, random);
                    placeWalls(world, generator, (EndCaveBiome) biome.bclBiome, caveBlocks, random);
                } else if (Configs.MAIN_CONFIG.verboseLogging() && errCounter < 25) {
                    errCounter++;
                    BetterEnd.LOGGER.error(biome.bclBiome.getID() + " is not an EndCaveBiome.");
                }
            }
            fixBlocks(world, caveBlocks);
        }

        return true;
    }

    private static final Comparator<BlockPos> POSITION_ORDER = Comparator
            .comparingInt((BlockPos p) -> p.getY())
            .thenComparingInt(p -> p.getX())
            .thenComparingInt(p -> p.getZ());

    /**
     * The positions are collected by parallel streams into concurrent sets, whose iteration order depends on the
     * thread timing. Decorating them consumes the feature's random in that order, so return them in a fixed order
     * to generate the same caves every time.
     */
    protected static Set<BlockPos> inPositionOrder(Set<BlockPos> positions) {
        final Set<BlockPos> result = new LinkedHashSet<>(positions.size() * 2);
        positions.stream()
                 .sorted(POSITION_ORDER)
                 .forEachOrdered(result::add);
        return result;
    }

    protected abstract Set<BlockPos> generate(WorldGenLevel world, BlockPos center, int radius, RandomSource random);

    protected void placeFloor(
            WorldGenLevel world,
            ChunkGenerator generator,
            EndCaveBiome biome,
            Set<BlockPos> floorPositions,
            RandomSource random,
            BlockState surfaceBlock
    ) {
        float density = biome.getFloorDensity();
        floorPositions.forEach((pos) -> {
            if (!surfaceBlock.is(Blocks.END_STONE)) {
                BlocksHelper.setWithoutUpdate(world, pos, surfaceBlock);
            }
            if (density > 0 && random.nextFloat() <= density) {
                Holder<? extends ConfiguredFeature<?, ?>> feature = biome.getFloorFeature(random);
                if (feature != null && feature.isBound()) {
                    feature.value().place(world, generator, random, pos.above());
                }
            }
        });
    }

    protected void placeCeil(
            WorldGenLevel world,
            ChunkGenerator generator,
            EndCaveBiome biome,
            Set<BlockPos> ceilPositions,
            RandomSource random
    ) {
        float density = biome.getCeilDensity();
        ceilPositions.forEach((pos) -> {
            BlockState ceilBlock = biome.getCeil(pos);
            if (ceilBlock != null) {
                BlocksHelper.setWithoutUpdate(world, pos, ceilBlock);
            }
            if (density > 0 && random.nextFloat() <= density) {
                Holder<? extends ConfiguredFeature<?, ?>> feature = biome.getCeilFeature(random);
                if (feature != null && feature.isBound()) {
                    feature.value().place(world, generator, random, pos.below());
                }
            }
        });
    }

    protected void placeWalls(
            WorldGenLevel world,
            ChunkGenerator generator,
            EndCaveBiome biome,
            Set<BlockPos> positions,
            RandomSource random
    ) {
        Set<BlockPos> placed = Sets.newHashSet();
        positions.forEach(pos -> {
            if (random.nextInt(4) == 0 && hasOpenSide(pos, positions)) {
                BlockState wallBlock = biome.getWall(pos);
                if (wallBlock != null) {
                    for (Vec3i offset : SPHERE) {
                        BlockPos wallPos = pos.offset(offset);
                        if (!positions.contains(wallPos) && !placed.contains(wallPos) && GenerationArea
                                .getBlockState(world, wallPos)
                                .is(CommonBlockTags.GEN_END_STONES)) {
                            wallBlock = biome.getWall(wallPos);
                            BlocksHelper.setWithoutUpdate(world, wallPos, wallBlock);
                            placed.add(wallPos);
                        }
                    }
                }
            }
        });
    }

    private boolean hasOpenSide(BlockPos pos, Set<BlockPos> positions) {
        for (Direction dir : BlocksHelper.DIRECTIONS) {
            if (!positions.contains(pos.relative(dir))) {
                return true;
            }
        }
        return false;
    }

    protected void setBiomes(WorldGenLevel world, BiomePicker.ActualBiome biome, Set<BlockPos> blocks) {
        blocks.forEach((pos) -> setBiome(world, pos, biome));
    }

    protected void setBiome(WorldGenLevel world, BlockPos pos, BiomePicker.ActualBiome biome) {
        final ChunkAccess chunk = world.getChunk(pos);
        BiomeAPI.setBiome(chunk, pos, session(world).caveBiomeFor(chunk, pos, biome.biome));
    }

    private static final ThreadLocal<CaveSession> SESSION = new ThreadLocal<>();

    /**
     * The caves of the chunk that is currently decorated on this thread (all features of a chunk share one
     * {@link WorldGenLevel} and run one after the other on the same thread).
     */
    protected static CaveSession session(WorldGenLevel world) {
        CaveSession session = SESSION.get();
        if (session == null || session.level.get() != world) {
            session = new CaveSession(world);
            SESSION.set(session);
        }
        return session;
    }

    /**
     * Caves of neighbouring chunks overlap, and which chunk is decorated first depends on the order the chunks are
     * generated in. To get the same caves and cave biomes in every order, a cave treats what the caves of other
     * chunks did as if it did not happen yet: their cave air counts as stone, and where they already wrote a cave
     * biome the cave biome with the lowest id is kept instead of the one written last. Within one chunk nothing
     * changes, so the caves of a chunk still see and overwrite each other in their (fixed) feature order.
     */
    protected static final class CaveSession {
        private final WeakReference<WorldGenLevel> level;
        private final LongOpenHashSet carved = new LongOpenHashSet();
        // the cave biome another chunk wrote into a cell before this chunk touched it (null: none)
        private final Long2ObjectOpenHashMap<Holder<Biome>> foreignCaveBiomes = new Long2ObjectOpenHashMap<>();

        private CaveSession(WorldGenLevel level) {
            this.level = new WeakReference<>(level);
        }

        /**
         * Blocks a cave may carve: end stones, and cave air the caves of other chunks carved there. Safe to call from
         * several threads as long as no block is marked as carved at the same time.
         */
        public boolean isCaveStone(BlockState state, BlockPos pos) {
            return state.is(CommonBlockTags.GEN_END_STONES)
                    || (state.is(Blocks.CAVE_AIR) && !carved.contains(pos.asLong()));
        }

        public void markCarved(BlockPos pos) {
            carved.add(pos.asLong());
        }

        private Holder<Biome> caveBiomeFor(ChunkAccess chunk, BlockPos pos, Holder<Biome> biome) {
            final long cell = BlockPos.asLong(
                    QuartPos.fromBlock(pos.getX()),
                    QuartPos.fromBlock(pos.getY()),
                    QuartPos.fromBlock(pos.getZ())
            );
            if (!foreignCaveBiomes.containsKey(cell)) {
                final Holder<Biome> current = chunk.getNoiseBiome(
                        QuartPos.fromBlock(pos.getX()),
                        QuartPos.fromBlock(pos.getY()),
                        QuartPos.fromBlock(pos.getZ())
                );
                foreignCaveBiomes.put(
                        cell,
                        current.unwrapKey().isPresent() && BiomeAPI.getBiome(current) instanceof EndCaveBiome
                                ? current
                                : null
                );
            }
            final Holder<Biome> foreign = foreignCaveBiomes.get(cell);
            if (foreign != null && biome.unwrapKey().isPresent()
                    && foreign.unwrapKey().get().location().toString()
                              .compareTo(biome.unwrapKey().get().location().toString()) < 0) {
                return foreign;
            }
            return biome;
        }
    }

    /**
     * The biome the biome source generated at the position, ignoring the cave biomes other caves already wrote into
     * the chunks (which depends on the order the chunks are generated in).
     */
    protected static Holder<Biome> getGeneratedBiome(WorldGenLevel world, BlockPos pos) {
        if (world instanceof WorldGenRegion region) {
            final ServerLevel serverLevel = region.getLevel();
            final BiomeSource source = serverLevel.getChunkSource().getGenerator().getBiomeSource();
            final Climate.Sampler sampler = serverLevel.getChunkSource().randomState().sampler();
            final int minQuartY = QuartPos.fromBlock(region.getMinBuildHeight());
            final int maxQuartY = minQuartY + QuartPos.fromBlock(region.getHeight()) - 1;
            return region.getBiomeManager()
                         .withDifferentSource((qx, qy, qz) -> source.getNoiseBiome(
                                 qx,
                                 Mth.clamp(qy, minQuartY, maxQuartY),
                                 qz,
                                 sampler
                         ))
                         .getBiome(pos);
        }
        return GenerationArea.getBiome(world, pos);
    }

    private BlockPos findPos(
            WorldGenLevel world,
            ChunkGenerator generator,
            BlockPos pos,
            int radius,
            RandomSource random
    ) {
        // Look at the column as the noise generated it: features of the neighbouring chunks (caves, lakes, ...) may
        // already have changed it or not, depending on the order the chunks are generated in.
        final NoiseColumn column = generator.getBaseColumn(
                pos.getX(),
                pos.getZ(),
                world,
                world.getLevel().getChunkSource().randomState()
        );
        int y = world.getMaxBuildHeight() - 1;
        BlockState state = column.getBlock(y);
        while (!state.is(CommonBlockTags.GEN_END_STONES) && y > 5) {
            state = column.getBlock(--y);
        }
        if (y < 10) {
            return null;
        }
        int top = (int) (y - (radius * 1.3F + 5));

        while ((state.is(CommonBlockTags.GEN_END_STONES) || !state.getFluidState().isEmpty() && y > 5)
                && y > world.getMinBuildHeight()) {
            state = column.getBlock(--y);
        }
        int bottom = (int) (y + radius * 1.3F + 5);

        if (top <= bottom) {
            return null;
        }

        return new BlockPos(pos.getX(), MHelper.randRange(bottom, top, random), pos.getZ());
    }

    protected void fixBlocks(WorldGenLevel world, Set<BlockPos> caveBlocks) {
        BlockPos pos = caveBlocks.iterator().next();
        MutableBlockPos start = new MutableBlockPos().set(pos);
        MutableBlockPos end = new MutableBlockPos().set(pos);
        caveBlocks.forEach((bpos) -> {
            if (bpos.getX() < start.getX()) {
                start.setX(bpos.getX());
            }
            if (bpos.getX() > end.getX()) {
                end.setX(bpos.getX());
            }

            if (bpos.getY() < start.getY()) {
                start.setY(bpos.getY());
            }
            if (bpos.getY() > end.getY()) {
                end.setY(bpos.getY());
            }

            if (bpos.getZ() < start.getZ()) {
                start.setZ(bpos.getZ());
            }
            if (bpos.getZ() > end.getZ()) {
                end.setZ(bpos.getZ());
            }
        });
        BlockFixer.fixBlocks(world, start.offset(-2, -2, -2), end.offset(2, 2, 2));
    }

    protected boolean isWaterNear(WorldGenLevel world, BlockPos pos) {
        for (Direction dir : BlocksHelper.DIRECTIONS) {
            if (!GenerationArea.getFluidState(world, pos.relative(dir, 5)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    protected boolean biomeMissingCaves(WorldGenLevel world, BlockPos pos) {
        for (int x = -2; x < 3; x++) {
            for (int z = -2; z < 3; z++) {
                Holder<Biome> biome = getGeneratedBiome(world, pos.offset(x << 4, 0, z << 4));
                BCLBiome bclBiome = BiomeAPI.getBiome(biome);
                boolean hasCaves = true;
                if (bclBiome instanceof EndBiome endBiome)
                    hasCaves = endBiome.hasCaves();

                if (!hasCaves
                        && !BCLBiomeRegistry.isEmptyBiome(bclBiome)
                        && BiomeAPI.wasRegisteredAsEndLandBiome(bclBiome.getID())) {
                    return true;
                }
            }
        }
        return false;
    }

    static {
        List<Vec3i> prePos = Lists.newArrayList();
        int radius = 5;
        int r2 = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            int x2 = x * x;
            for (int y = -radius; y <= radius; y++) {
                int y2 = y * y;
                for (int z = -radius; z <= radius; z++) {
                    int z2 = z * z;
                    if (x2 + y2 + z2 < r2) {
                        prePos.add(new Vec3i(x, y, z));
                    }
                }
            }
        }
        SPHERE = prePos.toArray(new Vec3i[]{});
    }
}
