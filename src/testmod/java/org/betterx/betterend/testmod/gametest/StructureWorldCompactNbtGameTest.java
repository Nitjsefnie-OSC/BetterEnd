package org.betterx.betterend.testmod.gametest;

import org.betterx.bclib.api.v2.levelgen.structures.StructureWorld;
import org.betterx.bclib.sdf.SDF;
import org.betterx.betterend.world.structures.features.GiantIceStarStructure;
import org.betterx.betterend.world.structures.features.GiantMossyGlowshroomStructure;
import org.betterx.betterend.world.structures.piece.VoxelPiece;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * Regression test for BetterEnd #582: giant ice stars and giant mossy glowshrooms produced structure starts of
 * several MB, because BCLib's {@link StructureWorld} wrote one compound (position array + full block state, its
 * palette never deduplicated) per block. The compact format (palette + one packed int per block) must keep the
 * voxel content bit-for-bit identical, and worlds saved in the old format must still load identically.
 * <p>
 * For several seeds, the real structure SDFs are filled into a {@link VoxelPiece}; the piece is then
 * <ul>
 *     <li>round-tripped through its (new, compact) NBT,</li>
 *     <li>converted to the old per-block NBT layout (as the old writer produced it) and loaded from that,</li>
 * </ul>
 * and both results are compared to the original - voxel by voxel inside {@link StructureWorld}, and block by
 * block after placing them into fresh chunks. The serialized piece must also stay below 1 MiB.
 */
public class StructureWorldCompactNbtGameTest {
    private static final long[] SEEDS = {1L, 582L, 1234567L, -42L};
    private static final int MAX_BYTES = 1 << 20;

    @GameTest(maxTicks = 6000)
    public void giantIceStarNbtIsCompactAndLossless(GameTestHelper helper) {
        check(helper, "giant_ice_star", GiantIceStarStructure.class, seed -> {
            final RandomSource r = RandomSource.create(seed);
            return new BlockPos(8 + r.nextInt(16), 32 + r.nextInt(97), 8 + r.nextInt(16));
        });
        helper.succeed();
    }

    @GameTest(maxTicks = 6000)
    public void giantMossyGlowshroomNbtIsCompactAndLossless(GameTestHelper helper) {
        check(helper, "giant_mossy_glowshroom", GiantMossyGlowshroomStructure.class, seed -> {
            final RandomSource r = RandomSource.create(seed);
            return new BlockPos(8 + r.nextInt(16), 60 + r.nextInt(40), 8 + r.nextInt(16));
        });
        helper.succeed();
    }

    private interface StartFor {
        BlockPos get(long seed);
    }

    private record SetBlock(BlockPos pos, BlockState state) {
    }

    private static void check(GameTestHelper helper, String name, Class<?> structure, StartFor startFor) {
        final ServerLevel level = helper.getLevel();
        final StructurePieceSerializationContext context = StructurePieceSerializationContext.fromLevel(level);
        final List<String> failures = new ArrayList<>();

        for (long seed : SEEDS) {
            final BlockPos start = startFor.get(seed);
            final SDF sdf = sdf(structure, start, RandomSource.create(seed));

            // Record exactly what the SDF writes, then replay it into the piece's own StructureWorld.
            final List<SetBlock> calls = new ArrayList<>();
            sdf.fillRecursive(new StructureWorld() {
                @Override
                public void setBlock(BlockPos pos, BlockState state) {
                    calls.add(new SetBlock(pos.immutable(), state));
                }
            }, start);
            final Map<ChunkPos, Map<BlockPos, BlockState>> expected = new HashMap<>();
            for (SetBlock c : calls) {
                expected.computeIfAbsent(new ChunkPos(c.pos), k -> new HashMap<>())
                        .put(new BlockPos(c.pos.getX() & 15, c.pos.getY(), c.pos.getZ() & 15), c.state);
            }
            final VoxelPiece original = new VoxelPiece(w -> calls.forEach(c -> w.setBlock(c.pos, c.state)), 0);

            final CompoundTag newTag = original.createTag(context);
            final CompoundTag oldTag = newTag.copy();
            oldTag.put("world", toLegacyFormat(newTag.getCompoundOrEmpty("world")));
            final int newBytes = size(newTag);
            final int oldBytes = size(oldTag);
            final int blockCount = expected.values().stream().mapToInt(Map::size).sum();
            System.out.printf(
                    "[StructureWorldCompactNbt] %s seed=%d start=%s chunks=%d blocks=%d old=%d bytes new=%d bytes (%.1fx smaller)%n",
                    name, seed, start.toShortString(), expected.size(), blockCount, oldBytes, newBytes,
                    (double) oldBytes / newBytes
            );

            final String tag = name + " seed " + seed;
            if (blockCount < 1000) failures.add(tag + ": only " + blockCount + " blocks, test is not meaningful");
            if (newBytes >= MAX_BYTES) failures.add(tag + ": new NBT is " + newBytes + " bytes (>= 1 MiB)");
            final ListTag parts = newTag.getCompoundOrEmpty("world").getListOrEmpty("parts");
            for (int i = 0; i < parts.size(); i++) {
                final CompoundTag part = parts.getCompoundOrEmpty(i);
                if (part.getIntArray("packed").isEmpty() || part.contains("blocks")) {
                    failures.add(tag + ": part " + i + " was not written in the compact format");
                    break;
                }
            }

            final StructurePiece fromNew = original.getType().load(context, newTag);
            final StructurePiece fromOld = original.getType().load(context, oldTag);

            compareVoxels(tag + " original", expected, original, failures);
            compareVoxels(tag + " new-format", expected, fromNew, failures);
            compareVoxels(tag + " old-format", expected, fromOld, failures);
            if (!original.getBoundingBox().equals(fromNew.getBoundingBox())
                    || !original.getBoundingBox().equals(fromOld.getBoundingBox())) {
                failures.add(tag + ": bounding box changed " + original.getBoundingBox() + " / "
                        + fromNew.getBoundingBox() + " / " + fromOld.getBoundingBox());
            }

            final Map<ChunkPos, ProtoChunk> reference = place(level, original, expected);
            comparePlaced(tag + " new-format", level, reference, place(level, fromNew, expected), failures);
            comparePlaced(tag + " old-format", level, reference, place(level, fromOld, expected), failures);
        }

        if (!failures.isEmpty()) {
            throw helper.assertionException(Component.literal(name + ":\n - " + String.join("\n - ", failures)));
        }
    }

    private static SDF sdf(Class<?> structure, BlockPos start, RandomSource random) {
        try {
            final Method m = structure.getDeclaredMethod("getSDF", BlockPos.class, RandomSource.class);
            m.setAccessible(true);
            return (SDF) m.invoke(null, start, random);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Rewrites a compact StructureWorld tag exactly the way the old BCLib writer laid it out. */
    private static CompoundTag toLegacyFormat(CompoundTag world) {
        final CompoundTag result = world.copy();
        final ListTag oldParts = new ListTag();
        final ListTag parts = world.getListOrEmpty("parts");
        for (int i = 0; i < parts.size(); i++) {
            final CompoundTag part = parts.getCompoundOrEmpty(i);
            if (part.getIntArray("packed").isEmpty()) {
                oldParts.add(part.copy()); // already in the old layout
                continue;
            }
            final ListTag palette = part.getListOrEmpty("states");
            final CompoundTag oldPart = new CompoundTag();
            oldPart.putInt("x", part.getIntOr("x", 0));
            oldPart.putInt("z", part.getIntOr("z", 0));
            final ListTag blocks = new ListTag();
            final ListTag states = new ListTag();
            oldPart.put("blocks", blocks);
            oldPart.put("states", states);
            // the old writer never found a state in its palette, so every block got its own entry
            for (int value : part.getIntArray("packed").orElseThrow()) {
                final CompoundTag block = new CompoundTag();
                block.put("pos", new IntArrayTag(new int[]{(value >>> 28) & 15, (value << 8) >> 20, (value >>> 24) & 15}));
                block.putInt("state", states.size());
                states.add(palette.getCompoundOrEmpty(value & 0xFFF).copy());
                blocks.add(block);
            }
            oldParts.add(oldPart);
        }
        result.put("parts", oldParts);
        return result;
    }

    private static int size(CompoundTag tag) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            NbtIo.write(tag, out);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.size();
    }

    @SuppressWarnings("unchecked")
    private static Map<ChunkPos, Map<BlockPos, BlockState>> voxels(StructurePiece piece) {
        try {
            final Field worldField = VoxelPiece.class.getDeclaredField("world");
            worldField.setAccessible(true);
            final StructureWorld world = (StructureWorld) worldField.get(piece);
            final Field partsField = StructureWorld.class.getDeclaredField("parts");
            partsField.setAccessible(true);
            final Map<ChunkPos, Object> parts = (Map<ChunkPos, Object>) partsField.get(world);
            final Map<ChunkPos, Map<BlockPos, BlockState>> result = new HashMap<>();
            for (Map.Entry<ChunkPos, Object> e : parts.entrySet()) {
                final Field blocksField = e.getValue().getClass().getDeclaredField("blocks");
                blocksField.setAccessible(true);
                result.put(e.getKey(), new HashMap<>((Map<BlockPos, BlockState>) blocksField.get(e.getValue())));
            }
            return result;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void compareVoxels(
            String scenario,
            Map<ChunkPos, Map<BlockPos, BlockState>> expected,
            StructurePiece piece,
            List<String> failures
    ) {
        final Map<ChunkPos, Map<BlockPos, BlockState>> actual = voxels(piece);
        if (!actual.equals(expected)) {
            int diff = 0;
            for (ChunkPos key : expected.keySet()) {
                final Map<BlockPos, BlockState> a = actual.getOrDefault(key, Map.of());
                for (Map.Entry<BlockPos, BlockState> e : expected.get(key).entrySet()) {
                    if (a.get(e.getKey()) != e.getValue()) diff++;
                }
                diff += (int) a.keySet().stream().filter(p -> !expected.get(key).containsKey(p)).count();
            }
            failures.add(scenario + ": voxel content differs (" + diff + " voxels, " + actual.size() + " vs "
                    + expected.size() + " chunks)");
        }
    }

    private static Map<ChunkPos, ProtoChunk> place(
            ServerLevel level,
            StructurePiece piece,
            Map<ChunkPos, Map<BlockPos, BlockState>> expected
    ) {
        final Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
        final Map<ChunkPos, ProtoChunk> chunks = new HashMap<>();
        for (ChunkPos pos : expected.keySet()) {
            chunks.put(pos, new ProtoChunk(pos, UpgradeData.EMPTY, level, biomes, null));
        }
        final WorldGenLevel world = fakeWorld(level, chunks);
        for (ChunkPos pos : expected.keySet()) {
            final BoundingBox box = new BoundingBox(
                    pos.getMinBlockX(), level.getMinY(), pos.getMinBlockZ(),
                    pos.getMaxBlockX(), level.getMaxY(), pos.getMaxBlockZ()
            );
            piece.postProcess(world, null, null, RandomSource.create(0), box, pos, BlockPos.ZERO);
        }
        return chunks;
    }

    private static void comparePlaced(
            String scenario,
            ServerLevel level,
            Map<ChunkPos, ProtoChunk> expected,
            Map<ChunkPos, ProtoChunk> actual,
            List<String> failures
    ) {
        int diff = 0;
        int placed = 0;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (Map.Entry<ChunkPos, ProtoChunk> e : expected.entrySet()) {
            final ChunkAccess a = e.getValue();
            final ChunkAccess b = actual.get(e.getKey());
            for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        pos.set(e.getKey().getMinBlockX() + x, y, e.getKey().getMinBlockZ() + z);
                        final BlockState sa = a.getBlockState(pos);
                        if (!sa.isAir()) placed++;
                        if (sa != b.getBlockState(pos)) diff++;
                    }
                }
            }
        }
        if (placed == 0) failures.add(scenario + ": reference placement produced no blocks");
        if (diff > 0) failures.add(scenario + ": placed blocks differ in " + diff + " positions (of " + placed + ")");
    }

    private static WorldGenLevel fakeWorld(ServerLevel level, Map<ChunkPos, ProtoChunk> chunks) {
        final InvocationHandler handler = (proxy, method, args) -> {
            final int argc = args == null ? 0 : args.length;
            switch (method.getName()) {
                case "getChunk":
                    if (argc >= 2 && args[0] instanceof Integer x && args[1] instanceof Integer z) {
                        return chunks.get(new ChunkPos(x, z));
                    }
                    if (argc == 1 && args[0] instanceof BlockPos p) {
                        return chunks.get(new ChunkPos(
                                SectionPos.blockToSectionCoord(p.getX()),
                                SectionPos.blockToSectionCoord(p.getZ())
                        ));
                    }
                    break;
                case "getMinY":
                    return level.getMinY();
                case "getMaxY":
                    return level.getMaxY();
                case "getHeight":
                    if (argc == 0) return level.getHeight();
                    break;
                case "getLevel":
                    return level;
                case "registryAccess":
                    return level.registryAccess();
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "StructureWorldCompactNbtGameTest.FakeWorldGenLevel";
            }
            throw new UnsupportedOperationException("WorldGenLevel." + method.getName() + " not stubbed for this test");
        };
        return (WorldGenLevel) Proxy.newProxyInstance(
                StructureWorldCompactNbtGameTest.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class},
                handler
        );
    }
}
