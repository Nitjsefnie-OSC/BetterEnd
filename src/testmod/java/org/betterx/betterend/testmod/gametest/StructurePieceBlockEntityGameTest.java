package org.betterx.betterend.testmod.gametest;

import org.betterx.betterend.registry.block.EndCrystalBlocks;
import org.betterx.betterend.world.structures.piece.BasePiece;
import org.betterx.betterend.world.structures.piece.CrystalMountainPiece;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

/**
 * The crashed ship ({@code CrashedShipFeature}, SURFACE_STRUCTURES) places the vanilla end ship template -
 * dragon head, brewing stand and chests included - into the 3x3 chunks around its own. A neighbouring chunk
 * can still be waiting for its own decoration then; when it runs, its mountain piece (RAW_GENERATION) writes
 * end stone, crystal moss or aurora crystals over the ship with {@code ChunkAccess.setBlockState}. On a
 * {@link ProtoChunk} that only swaps the block state, so the block entities were left behind and the server
 * logged "Trying to set block entity ... but state Block{minecraft:end_stone} does not allow it" or "Failed
 * to create block entity ... got Block{betterend:aurora_crystal}" when the chunk was promoted or saved.
 * <p>
 * The mountain addresses the chunk with chunk-local x/z while block entities are keyed by world position,
 * so the chunk here is deliberately not chunk [0, 0] (where both coincide and a local-position removal
 * would pass by accident).
 * <ul>
 *     <li>{@link #mountainBuriesShipBlockEntities}: the real ordering - terrain, then the ship's block
 *     entity blocks (one live block entity, one pending tag as a chunk reloaded in between holds), then a
 *     real {@link CrystalMountainPiece#postProcess} over them. The blocks must be buried (else the fixture
 *     proves nothing) and their block entities gone.</li>
 *     <li>{@link #setChunkBlockDropsReplacedBlockEntities}: the helper itself with local and world
 *     coordinates, the same-block case, and a control showing the raw ProtoChunk write keeps the block
 *     entity.</li>
 * </ul>
 */
public class StructurePieceBlockEntityGameTest {
    private static final BlockState HEAD = Blocks.DRAGON_WALL_HEAD.defaultBlockState();
    private static final BlockState BREWING = Blocks.BREWING_STAND.defaultBlockState();
    /** Same chunk as the reported (726, 63, -2504). */
    private static final ChunkPos CHUNK = new ChunkPos(45, -157);
    private static final int SURFACE_Y = 61;

    @GameTest(maxTicks = 200)
    public void mountainBuriesShipBlockEntities(GameTestHelper helper) {
        final ProtoChunk chunk = newChunk(helper);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 40; y < SURFACE_Y; y++) {
                    chunk.setBlockState(new BlockPos(x, y, z), Blocks.END_STONE.defaultBlockState(), 3);
                }
            }
        }
        // The worldgen heightmap is primed from the base terrain, before any feature writes.
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG));

        // What the ship leaves: a live block entity (template placement) and a pending tag (chunk saved
        // and reloaded before the mountain ran).
        final BlockPos head = new BlockPos(CHUNK.getBlockX(6), SURFACE_Y, CHUNK.getBlockZ(8));
        chunk.setBlockState(head, HEAD, 3);
        chunk.setBlockEntity(new SkullBlockEntity(head, HEAD));
        final BlockPos brewing = new BlockPos(CHUNK.getBlockX(10), SURFACE_Y, CHUNK.getBlockZ(4));
        chunk.setBlockState(brewing, BREWING, 3);
        chunk.setBlockEntityNbt(pendingTag("minecraft:brewing_stand", brewing));
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.WORLD_SURFACE));

        final Holder<Biome> biome = helper.getLevel()
                                          .registryAccess()
                                          .lookupOrThrow(Registries.BIOME)
                                          .getOrThrow(Biomes.END_HIGHLANDS);
        final CrystalMountainPiece piece = new CrystalMountainPiece(
                new BlockPos(CHUNK.getBlockX(8), SURFACE_Y, CHUNK.getBlockZ(8)),
                60,
                60,
                RandomSource.create(8633573328124670870L),
                biome
        );
        final BoundingBox chunkBox = new BoundingBox(
                CHUNK.getMinBlockX(), chunk.getMinY(), CHUNK.getMinBlockZ(),
                CHUNK.getMaxBlockX(), chunk.getMaxY(), CHUNK.getMaxBlockZ()
        );
        piece.postProcess(
                fakeLevel(chunk, biome),
                null,
                null,
                RandomSource.create(42),
                chunkBox,
                CHUNK,
                CHUNK.getMiddleBlockPosition(SURFACE_Y)
        );

        for (BlockPos pos : new BlockPos[]{head, brewing}) {
            final BlockState now = chunk.getBlockState(pos);
            if (now.hasBlockEntity()) {
                throw helper.assertionException(Component.literal(
                        "The mountain did not bury " + now + " at " + pos + " - the fixture no longer exercises the bug"
                ));
            }
            if (chunk.getBlockEntitiesPos().contains(pos)) {
                throw helper.assertionException(Component.literal(
                        "A ship block entity survived the mountain burying it under " + now + " at " + pos
                ));
            }
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 200)
    public void setChunkBlockDropsReplacedBlockEntities(GameTestHelper helper) throws Exception {
        final ProtoChunk chunk = newChunk(helper);
        final Method setChunkBlock = BasePiece.class.getDeclaredMethod(
                "setChunkBlock", ChunkAccess.class, BlockPos.class, BlockState.class
        );
        setChunkBlock.setAccessible(true);

        // Control: the raw ProtoChunk write keeps the stale block entity - this is the bug.
        final BlockPos control = world(2, 64, 2);
        placeHead(chunk, control);
        chunk.setBlockState(control, Blocks.END_STONE.defaultBlockState(), 3);
        if (chunk.getBlockEntity(control) == null) {
            throw helper.assertionException(Component.literal(
                    "ProtoChunk.setBlockState now drops block entities itself - the fixture no longer reproduces the bug"
            ));
        }

        // World coordinates, live block entity.
        final BlockPos live = world(6, 63, 6);
        placeHead(chunk, live);
        setChunkBlock.invoke(null, chunk, live, Blocks.END_STONE.defaultBlockState());
        assertNoBlockEntity(helper, chunk, live, "end stone (world coordinates)");

        // Chunk-local coordinates (as the mountain pieces write), live block entity.
        final BlockPos local = world(8, 62, 3);
        placeHead(chunk, local);
        setChunkBlock.invoke(null, chunk, new BlockPos(8, 62, 3), Blocks.END_STONE.defaultBlockState());
        assertNoBlockEntity(helper, chunk, local, "end stone (chunk-local coordinates)");

        // Chunk-local coordinates, pending block entity tag, under an aurora crystal.
        final BlockPos pending = world(10, 61, 10);
        chunk.setBlockState(pending, HEAD, 3);
        chunk.setBlockEntityNbt(pendingTag("minecraft:skull", pending));
        setChunkBlock.invoke(
                null, chunk, new BlockPos(10, 61, 10),
                EndCrystalBlocks.AURORA_CRYSTAL.defaultBlockState()
        );
        assertNoBlockEntity(helper, chunk, pending, "an aurora crystal (pending tag)");

        // Re-writing the same block (e.g. a rotated head) keeps its block entity.
        final BlockPos same = world(12, 64, 4);
        placeHead(chunk, same);
        setChunkBlock.invoke(null, chunk, same, HEAD);
        if (chunk.getBlockEntity(same) == null) {
            throw helper.assertionException(Component.literal(
                    "setChunkBlock dropped the block entity of a block that was written over with the same block"
            ));
        }

        helper.succeed();
    }

    private static ProtoChunk newChunk(GameTestHelper helper) {
        return new ProtoChunk(
                CHUNK,
                UpgradeData.EMPTY,
                helper.getLevel(),
                helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME),
                null
        );
    }

    private static BlockPos world(int localX, int y, int localZ) {
        return new BlockPos(CHUNK.getBlockX(localX), y, CHUNK.getBlockZ(localZ));
    }

    private static CompoundTag pendingTag(String id, BlockPos pos) {
        final CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        return tag;
    }

    private static void placeHead(ProtoChunk chunk, BlockPos pos) {
        chunk.setBlockState(pos, HEAD, 3);
        chunk.setBlockEntity(new SkullBlockEntity(pos, HEAD));
    }

    private static void assertNoBlockEntity(GameTestHelper helper, ProtoChunk chunk, BlockPos pos, String what) {
        if (chunk.getBlockEntitiesPos().contains(pos)) {
            throw helper.assertionException(Component.literal(
                    "A skull block entity survived being overwritten with " + what + " at " + pos
            ));
        }
    }

    /**
     * Everything the mountain piece asks the level for: the chunk (every chunk, so the radius-12 height
     * sampling around the border columns sees the same flat terrain) and the piece's own biome. Anything
     * else fails loudly so a change in what the piece reads shows up here.
     */
    private static WorldGenLevel fakeLevel(ProtoChunk chunk, Holder<Biome> biome) {
        final InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getChunk" -> chunk;
            case "getBiome" -> biome;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "StructurePieceBlockEntityGameTest.fakeLevel";
            default -> throw new UnsupportedOperationException("fake level: " + method);
        };
        return (WorldGenLevel) Proxy.newProxyInstance(
                StructurePieceBlockEntityGameTest.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class},
                handler
        );
    }
}
