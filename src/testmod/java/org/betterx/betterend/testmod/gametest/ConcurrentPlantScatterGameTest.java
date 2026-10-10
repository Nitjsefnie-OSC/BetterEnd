package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.EndFeatures;
import org.betterx.betterend.registry.block.EndMushroomBlocks;
import org.betterx.betterend.registry.block.EndPlantBlocks;
import org.betterx.betterend.registry.block.EndTerrainBlocks;
import org.betterx.betterend.registry.block.EndWallPlantBlocks;
import org.betterx.betterend.world.features.DoublePlantFeatureConfig;
import org.betterx.betterend.world.features.WallPlantFeatureConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * #477/#523/#445/#531: every configured feature of one {@link Feature} type shares a single feature
 * instance, and the plant scatter features pass the plant chosen in {@code canGenerate()} to {@code
 * generate()} through an instance field. With worldgen running on several threads (vanilla's worker
 * pool, and far more so with C2ME), a second thread could overwrite that field between the two calls,
 * so e.g. {@code small_jellyshroom} got the wall-plant {@code FACING} set on it, or {@code umbrella_moss}
 * the double-plant {@code ROTATION} - an {@code IllegalArgumentException} that kills the server.
 * <p>
 * This drives the real registered feature instances from many threads at once, half of them with one
 * config and half with another, exactly the mix that crashed. Each thread gets its own in-memory fake
 * {@link WorldGenLevel} (see {@link LakePieceConcurrentHeightCacheGameTest} for why not the live level),
 * so the only thing shared is the feature instance itself. It fails on a thrown exception and also on
 * the silent variant of the race: a thread placing a block that is not from its own config.
 */
public class ConcurrentPlantScatterGameTest {
    private static final int THREADS = 8;
    private static final int ITERATIONS = 150;
    private static final int FLOOR_Y = 20;

    @GameTest(maxTicks = 1200)
    public void wallPlantFeatureIsThreadSafe(GameTestHelper helper) throws Throwable {
        run(
                helper,
                EndFeatures.WALL_PLANT_FEATURE,
                new WallPlantFeatureConfig(EndWallPlantBlocks.CYAN_MOSS, 6),
                Set.of(EndWallPlantBlocks.CYAN_MOSS),
                new WallPlantFeatureConfig(EndMushroomBlocks.SMALL_JELLYSHROOM, 6),
                Set.of(EndMushroomBlocks.SMALL_JELLYSHROOM),
                Blocks.END_STONE.defaultBlockState()
        );
    }

    @GameTest(maxTicks = 1200)
    public void doublePlantFeatureIsThreadSafe(GameTestHelper helper) throws Throwable {
        run(
                helper,
                EndFeatures.DOUBLE_PLANT_FEATURE,
                new DoublePlantFeatureConfig(EndPlantBlocks.UMBRELLA_MOSS, EndPlantBlocks.UMBRELLA_MOSS_TALL, 6),
                Set.of(EndPlantBlocks.UMBRELLA_MOSS, EndPlantBlocks.UMBRELLA_MOSS_TALL),
                new DoublePlantFeatureConfig(
                        EndPlantBlocks.TWISTED_UMBRELLA_MOSS,
                        EndPlantBlocks.TWISTED_UMBRELLA_MOSS_TALL,
                        6
                ),
                Set.of(EndPlantBlocks.TWISTED_UMBRELLA_MOSS, EndPlantBlocks.TWISTED_UMBRELLA_MOSS_TALL),
                EndTerrainBlocks.END_MOSS.defaultBlockState()
        );
    }

    private static <FC extends FeatureConfiguration> void run(
            GameTestHelper helper,
            Feature<FC> feature,
            FC cfgA,
            Set<Block> allowedA,
            FC cfgB,
            Set<Block> allowedB,
            BlockState ground
    ) throws Throwable {
        final AtomicInteger placed = new AtomicInteger();
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            final List<Callable<Object>> tasks = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                final boolean useA = (t & 1) == 0;
                final FC cfg = useA ? cfgA : cfgB;
                final Set<Block> allowed = useA ? allowedA : allowedB;
                final long seed = t;
                tasks.add(() -> {
                    final RandomSource random = RandomSource.create(seed);
                    for (int i = 0; i < ITERATIONS; i++) {
                        final Map<BlockPos, BlockState> written = new HashMap<>();
                        final WorldGenLevel world = fakeLevel(ground, written);
                        feature.place(new FeaturePlaceContext<>(
                                Optional.empty(),
                                world,
                                null,
                                random,
                                new BlockPos(0, FLOOR_Y, 0),
                                cfg
                        ));
                        for (Map.Entry<BlockPos, BlockState> e : written.entrySet()) {
                            if (!allowed.contains(e.getValue().getBlock())) {
                                throw new IllegalStateException(
                                        "placed " + e.getValue() + " at " + e.getKey() + " for a config of " + allowed
                                );
                            }
                        }
                        placed.addAndGet(written.size());
                    }
                    return null;
                });
            }

            final List<Future<Object>> results = pool.invokeAll(tasks, 60, TimeUnit.SECONDS);
            final List<String> failures = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                final Future<Object> result = results.get(i);
                if (result.isCancelled()) {
                    failures.add("worker " + i + " did not finish within the timeout");
                    continue;
                }
                try {
                    result.get();
                } catch (ExecutionException e) {
                    failures.add("worker " + i + " threw " + e.getCause());
                }
            }
            if (!failures.isEmpty()) {
                throw helper.assertionException(Component.literal(
                        "Concurrent placement of " + feature + " failed:\n - " + String.join("\n - ", failures)
                ));
            }
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (placed.get() == 0) {
            throw helper.assertionException(Component.literal(
                    feature + " placed nothing in the fake level; the test does not exercise placement"
            ));
        }
        helper.succeed();
    }

    /**
     * A floor of {@code ground} below {@link #FLOOR_Y}, plus an end stone wall filling every {@code x < 0},
     * so floor plants find ground and wall plants find a wall at {@code x = 0}. Writes land in {@code
     * written}; every method not stubbed here falls through to its interface default or throws.
     */
    private static WorldGenLevel fakeLevel(BlockState ground, Map<BlockPos, BlockState> written) {
        final BlockState wall = Blocks.END_STONE.defaultBlockState();
        final BlockState air = Blocks.AIR.defaultBlockState();
        final InvocationHandler handler = (proxy, method, args) -> {
            final int argc = args == null ? 0 : args.length;
            switch (method.getName()) {
                case "getBlockState", "getFluidState", "isEmptyBlock" -> {
                    final BlockPos pos = (BlockPos) args[0];
                    BlockState state = written.get(pos);
                    if (state == null) {
                        if (pos.getX() < 0) state = wall;
                        else if (pos.getY() < FLOOR_Y) state = ground;
                        else state = air;
                    }
                    return switch (method.getName()) {
                        case "getFluidState" -> state.getFluidState();
                        case "isEmptyBlock" -> state.isAir();
                        default -> state;
                    };
                }
                case "setBlock" -> {
                    written.put(((BlockPos) args[0]).immutable(), (BlockState) args[1]);
                    return true;
                }
                case "getHeight" -> {
                    if (argc == 3) return ((Integer) args[1]) < 0 ? 256 : FLOOR_Y;
                    if (argc == 0) return 256;
                }
                case "getMinY" -> {
                    return 0;
                }
                case "equals" -> {
                    return proxy == (argc > 0 ? args[0] : null);
                }
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "toString" -> {
                    return "FakeWorldGenLevel";
                }
                default -> {
                }
            }
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            throw new UnsupportedOperationException("WorldGenLevel." + method.getName() + "() not stubbed for this test");
        };
        return (WorldGenLevel) Proxy.newProxyInstance(
                ConcurrentPlantScatterGameTest.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class},
                handler
        );
    }
}
