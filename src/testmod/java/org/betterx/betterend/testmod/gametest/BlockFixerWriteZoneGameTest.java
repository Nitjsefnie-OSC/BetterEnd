package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.util.BlockFixer;
import de.ambertation.wover.feature.api.WriteZone;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * 26.3 logs "Detected unsafe terrain read during worldgen" whenever a feature reads a block outside the
 * 3x3 chunks it may write to. Geysers and desert lakes clamped the box they hand to {@link BlockFixer} to
 * that zone, but BlockFixer peeks one block sideways from every column it scans (fluids check their
 * horizontal neighbours, unsupported blocks via {@code canSurvive}) and its chorus flood-fill spreads
 * horizontally - so a box ending on the zone edge still read outside it.
 * <p>
 * A real {@code WorldGenRegion} cannot be built in a test, so this drives BlockFixer against an in-memory
 * {@link WorldGenLevel} (same approach as {@link ConcurrentPlantScatterGameTest}) that records every
 * position it is asked about. The terrain is an endless lake with a sheet of unsupported chorus floating
 * on it - water next to water makes every column look sideways, and the chorus makes the flood-fill run.
 * {@link BlockFixer#fixBlocksInZone} must not read a single block outside the zone, while the old
 * "clamp the box to the zone" call is checked to still over-read, so the fixture provably exercises it.
 */
public class BlockFixerWriteZoneGameTest {
    private static final int FLOOR_Y = 10;
    private static final int WATER_Y = 11;
    private static final int CHORUS_Y = 12;

    /** The 3x3 chunks around chunk [1, 1]: what WriteZone.of() returns while decorating that chunk. */
    private static final WriteZone ZONE = new WriteZone(0, 0, 47, 47);

    @GameTest(maxTicks = 200)
    public void fixBlocksInZoneStaysInsideWriteZone(GameTestHelper helper) {
        final Set<BlockPos> reads = ConcurrentHashMap.newKeySet();
        // A box reaching past the zone on every side, as a large geyser or desert lake asks for.
        BlockFixer.fixBlocksInZone(
                fakeLevel(helper.getLevel(), reads),
                new BlockPos(-6, FLOOR_Y - 1, -6),
                new BlockPos(53, CHORUS_Y + 2, 53),
                ZONE
        );
        final BlockPos outside = firstOutside(reads);
        if (outside != null) {
            throw helper.assertionException(Component.literal(
                    "BlockFixer.fixBlocksInZone read " + outside + ", outside the write zone " + ZONE
            ));
        }

        final Set<BlockPos> clampedReads = ConcurrentHashMap.newKeySet();
        BlockFixer.fixBlocks(
                fakeLevel(helper.getLevel(), clampedReads),
                new BlockPos(ZONE.minX(), FLOOR_Y - 1, ZONE.minZ()),
                new BlockPos(ZONE.maxX(), CHORUS_Y + 2, ZONE.maxZ())
        );
        if (firstOutside(clampedReads) == null) {
            throw helper.assertionException(Component.literal(
                    "The fixture no longer makes a zone-clamped BlockFixer box read outside the zone; "
                            + "the test does not exercise the over-read any more"
            ));
        }
        helper.succeed();
    }

    private static BlockPos firstOutside(Set<BlockPos> reads) {
        for (BlockPos pos : reads) {
            if (!ZONE.contains(pos)) return pos;
        }
        return null;
    }

    /**
     * End stone up to {@link #FLOOR_Y}, water at {@link #WATER_Y}, unsupported chorus at {@link #CHORUS_Y}
     * and air above, everywhere. Writes land in a map; every position read is added to {@code reads}.
     */
    private static WorldGenLevel fakeLevel(ServerLevel level, Set<BlockPos> reads) {
        final Map<BlockPos, BlockState> written = new HashMap<>();
        final InvocationHandler handler = (proxy, method, args) -> {
            final int argc = args == null ? 0 : args.length;
            switch (method.getName()) {
                case "getBlockState", "getFluidState", "isEmptyBlock" -> {
                    final BlockPos pos = ((BlockPos) args[0]).immutable();
                    reads.add(pos);
                    BlockState state = written.get(pos);
                    if (state == null) {
                        if (pos.getY() <= FLOOR_Y) state = Blocks.END_STONE.defaultBlockState();
                        else if (pos.getY() == WATER_Y) state = Blocks.WATER.defaultBlockState();
                        else if (pos.getY() == CHORUS_Y) state = Blocks.CHORUS_PLANT.defaultBlockState();
                        else state = Blocks.AIR.defaultBlockState();
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
                case "scheduleTick" -> {
                    return null;
                }
                case "registryAccess" -> {
                    return level.registryAccess();
                }
                case "dimensionType" -> {
                    return level.dimensionType();
                }
                case "getRandom" -> {
                    return level.getRandom();
                }
                case "getMinY" -> {
                    return 0;
                }
                case "getHeight" -> {
                    if (argc == 0) return 256;
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
                BlockFixerWriteZoneGameTest.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class},
                handler
        );
    }
}
