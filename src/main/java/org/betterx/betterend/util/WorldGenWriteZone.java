package org.betterx.betterend.util;

import de.ambertation.wover.feature.api.WriteZone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * @deprecated moved to {@link WriteZone} in {@code wover-feature-api}. BetterNether had a second,
 * independent copy of the same idea in {@code BlocksHelper.decorationBounds}/{@code isInsideHorizontally};
 * the semantics that matter here - Y is ignored, the radius is probed rather than hard-coded, and there is
 * no restriction at all outside a {@code WorldGenRegion} - have to be identical in both mods, which is
 * only true for as long as there is one implementation. This class is a thin delegate and will be removed
 * in a future release; use {@link WriteZone} directly.
 */
@Deprecated(forRemoval = true)
public record WorldGenWriteZone(int minX, int minZ, int maxX, int maxZ) {
    public static final WorldGenWriteZone UNBOUNDED = from(WriteZone.UNBOUNDED);

    public static WorldGenWriteZone of(LevelAccessor level) {
        return from(WriteZone.of(level));
    }

    private static WorldGenWriteZone from(WriteZone zone) {
        return new WorldGenWriteZone(zone.minX(), zone.minZ(), zone.maxX(), zone.maxZ());
    }

    public WriteZone toWriteZone() {
        return new WriteZone(minX, minZ, maxX, maxZ);
    }

    public boolean contains(int x, int z) {
        return toWriteZone().contains(x, z);
    }

    public boolean contains(BlockPos pos) {
        return toWriteZone().contains(pos);
    }

    public int clampX(int x) {
        return toWriteZone().clampX(x);
    }

    public int clampZ(int z) {
        return toWriteZone().clampZ(z);
    }

    public boolean isDisjoint(int boxMinX, int boxMinZ, int boxMaxX, int boxMaxZ) {
        return toWriteZone().isDisjoint(boxMinX, boxMinZ, boxMaxX, boxMaxZ);
    }

    public BoundingBox clip(BoundingBox box) {
        return toWriteZone().clip(box);
    }

    public BoundingBox toBoundingBox() {
        return toWriteZone().toBoundingBox();
    }
}
