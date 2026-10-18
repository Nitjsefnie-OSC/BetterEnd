package org.betterx.betterend.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import com.google.common.collect.Maps;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Remembers, per dimension, which Eternal Portal (identified by its center, the bottom middle portal block) leads
 * to which portal in another dimension.
 * <p>
 * Portals to the Nether use the 8:1 coordinate scale. An exit is often built a few blocks away from the scaled
 * position, and scaled back up that offset grows by a factor of 8 - far beyond the portal search radius. So the
 * way back can not be found by searching around the scaled position; it uses the link stored here instead
 * (quiqueck/BetterEnd#329).
 */
public class EternalPortalLinks extends SavedData {
    private static final String NAME = "betterend_eternal_portal_links";

    public record Link(ResourceKey<Level> dimension, BlockPos center) {
    }

    private final Map<BlockPos, Link> links = Maps.newHashMap();

    public static EternalPortalLinks get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(EternalPortalLinks::load, EternalPortalLinks::new, NAME);
    }

    @Nullable
    public Link get(BlockPos center) {
        return links.get(center);
    }

    public void put(BlockPos center, ResourceKey<Level> targetDimension, BlockPos targetCenter) {
        final Link link = new Link(targetDimension, targetCenter.immutable());
        if (!link.equals(links.put(center.immutable(), link))) {
            setDirty();
        }
    }

    public void remove(BlockPos center) {
        if (links.remove(center) != null) {
            setDirty();
        }
    }

    /**
     * Links the ritual portal at {@code sourceCenter} to the exit at {@code exitCenter} and the exit back to the
     * ritual portal. The ritual side always points to its current exit. An exit that is shared by several rituals
     * (a ritual reuses an existing exit nearby) keeps leading back to the ritual it was first linked with, unless
     * that ritual was disabled in the meantime.
     */
    public static void link(ServerLevel source, BlockPos sourceCenter, ServerLevel target, BlockPos exitCenter) {
        get(source).put(sourceCenter, target.dimension(), exitCenter);
        final EternalPortalLinks targetLinks = get(target);
        if (targetLinks.get(exitCenter) == null) {
            targetLinks.put(exitCenter, source.dimension(), sourceCenter);
        }
    }

    /**
     * Forgets the links of a disabled ritual portal. The exit only forgets its way back when it led to this ritual.
     */
    public static void unlink(ServerLevel source, BlockPos sourceCenter, @Nullable ServerLevel target, @Nullable BlockPos exitCenter) {
        get(source).remove(sourceCenter);
        if (target != null && exitCenter != null) {
            final EternalPortalLinks targetLinks = get(target);
            final Link back = targetLinks.get(exitCenter);
            if (back != null && back.dimension().equals(source.dimension()) && back.center().equals(sourceCenter)) {
                targetLinks.remove(exitCenter);
            }
        }
    }

    public static EternalPortalLinks load(CompoundTag tag) {
        final EternalPortalLinks result = new EternalPortalLinks();
        final ListTag list = tag.getList("links", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            final ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("dimension"));
            if (dimension == null) continue;
            result.links.put(
                    BlockPos.of(entry.getLong("center")),
                    new Link(ResourceKey.create(Registries.DIMENSION, dimension), BlockPos.of(entry.getLong("target")))
            );
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        final ListTag list = new ListTag();
        links.forEach((center, link) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putLong("center", center.asLong());
            entry.putString("dimension", link.dimension().location().toString());
            entry.putLong("target", link.center().asLong());
            list.add(entry);
        });
        tag.put("links", list);
        return tag;
    }
}
