package org.betterx.betterend.world.structures.piece;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public abstract class BasePiece extends StructurePiece {
    protected BasePiece(StructurePieceType type, int i, BoundingBox boundingBox) {
        super(type, i, boundingBox);
    }

    protected BasePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        fromNbt(tag);
    }

    protected abstract void fromNbt(CompoundTag tag);

    /**
     * The first free y above the base terrain at a column, i.e. what {@code WORLD_SURFACE_WG} reports
     * for a chunk that is still being generated - independent of any blocks features or structure pieces
     * have placed since.
     * <p>
     * Do not use {@code world.getHeight(WORLD_SURFACE_WG, ...)} or
     * {@code chunk.getOrCreateHeightmapUnprimed(WORLD_SURFACE_WG)} for this: the worldgen heightmaps are
     * only persisted up to the SURFACE status, so a chunk that was saved at CARVERS or later (every chunk
     * left half-generated when the world is closed, and every finished neighbour) comes back without
     * them. {@code getOrCreateHeightmapUnprimed} then hands out an empty heightmap (every column at the
     * world bottom), and {@code getHeight} re-primes it from the chunk's current blocks - including a
     * mountain or lake this very piece already placed there before the reload. Either way the piece
     * shapes itself differently in chunks generated after a reload than in the ones generated before
     * it, which shows up as hard cut-offs along chunk borders (#556). A chunk that still has its
     * worldgen heightmap keeps using it; otherwise the generator's noise height, which is what that
     * heightmap was primed from in the first place, stands in for it.
     */
    protected static int worldgenSurfaceHeight(WorldGenLevel world, int x, int z) {
        final ChunkAccess chunk = world.getChunk(
                SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(z)
        );
        if (chunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE_WG)) {
            return chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + 1;
        }
        final ServerChunkCache source = world.getLevel().getChunkSource();
        return source.getGenerator()
                     .getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, world, source.randomState());
    }

    /**
     * Writes {@code state} straight into {@code chunk} like {@link ChunkAccess#setBlockState}, but also
     * drops the block entity of the block it replaces.
     * <p>
     * {@code ProtoChunk.setBlockState} only swaps the block state - unlike {@code WorldGenRegion.setBlock}
     * it never touches the chunk's block entities. Pieces run in their chunk's own decoration step, which
     * can come after a feature of a neighbouring chunk already wrote into this chunk: the crashed ship
     * ({@code CrashedShipFeature}, SURFACE_STRUCTURES) regularly reaches into a chunk whose mountain piece
     * (RAW_GENERATION) has not run yet. The mountain then buries the ship's dragon head under end stone or
     * an aurora crystal, the {@code SkullBlockEntity} stays behind, and the server logs "Trying to set
     * block entity ... but state ... does not allow" or "Failed to create block entity" once the chunk is
     * promoted or saved. A ship's brewing stand or chests can end up the same way.
     * <p>
     * Several pieces (the mountains among them) address the chunk with chunk-local x/z, which
     * {@code setBlockState} accepts because it masks them to the section anyway. The chunk's block
     * entities, however, are keyed by world position, so the position is converted before removing one -
     * removing at the local position silently misses it.
     */
    protected static void setChunkBlock(ChunkAccess chunk, BlockPos pos, BlockState state) {
        final BlockState old = chunk.setBlockState(pos, state, Block.UPDATE_ALL);
        if (old != null && old.hasBlockEntity() && !old.is(state.getBlock())) {
            chunk.removeBlockEntity(new BlockPos(
                    chunk.getPos().getBlockX(pos.getX() & 15),
                    pos.getY(),
                    chunk.getPos().getBlockZ(pos.getZ() & 15)
            ));
        }
    }

    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext structurePieceSerializationContext,
            CompoundTag compoundTag
    ) {
        addAdditionalSaveData(compoundTag);
    }
}