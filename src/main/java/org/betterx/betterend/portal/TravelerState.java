package org.betterx.betterend.portal;

import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.advancements.BECriteria;
import org.betterx.betterend.blocks.EndPortalBlock;
import org.betterx.betterend.registry.EndPortals;

import net.minecraft.BlockUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TravelerState {
    public final Entity entity;
    private BlockPos portalEntrancePos;
    private boolean isInsidePortal;
    private int portalTime;
    /**
     * Where the last trip through an Eternal Portal ended (dimension and center of the arrival portal), and the
     * portal it started from. Several rituals can share one exit; walking back through it leads to the portal this
     * entity came from.
     */
    @Nullable
    private EternalPortalLinks.Link arrivedAt;
    @Nullable
    private EternalPortalLinks.Link cameFrom;
    @Nullable
    private EternalPortalLinks.Link pendingArrival;
    @Nullable
    private EternalPortalLinks.Link pendingDeparture;

    public TravelerState(Entity entity) {
        this.entity = entity;
        this.portalEntrancePos = null;
        this.isInsidePortal = false;
    }

    public static TravelerState init(Entity e) {
        return new TravelerState(e);
    }

    public void handleInsidePortal(BlockPos blockPos) {
        if (entity.isOnPortalCooldown()) {
            entity.setPortalCooldown();
            return;
        }
        if (!this.level().isClientSide && !blockPos.equals(this.portalEntrancePos)) {
            this.portalEntrancePos = blockPos.immutable();
        }
        if (this.level().isClientSide) {
            if (entity instanceof LocalPlayer) {
                entity.isInsidePortal = true;
            }
        }

        this.isInsidePortal = true;
    }

    public Level level() {
        return this.entity.level();
    }

    @ApiStatus.Internal
    public void portalTick() {
        if (!(this.level() instanceof ServerLevel)) {
            return;
        }

        if (this.isInsidePortal) {
            final int waitTimer = entity.getPortalWaitTime();
            final ServerLevel sourceDimension = (ServerLevel) this.level();

            ServerLevel targetDimension = this.getTargetDimension(sourceDimension);

            if (targetDimension != null && targetDimension != sourceDimension
                    && !entity.isPassenger() && this.portalTime++ >= waitTimer) {
                this.level().getProfiler().push("end_portal");
                this.portalTime = waitTimer;
                entity.setPortalCooldown();

                this.changeDimension(targetDimension);
                this.level().getProfiler().pop();
            }
            this.isInsidePortal = false;
        } else {
            if (this.portalTime > 0) {
                this.portalTime -= 4;
            }
            if (this.portalTime < 0) {
                this.portalTime = 0;
            }
        }
    }

    /**
     * Eternal portals outside the End always lead to the End. Inside the End, the portal id stored in the
     * portal block (picked by the key item of the ritual) selects the dimension configured in
     * {@code portals.json}, so a portal keyed to the Nether really leads to the Nether.
     */
    @Nullable
    private ServerLevel getTargetDimension(ServerLevel sourceDimension) {
        final MinecraftServer server = sourceDimension.getServer();
        if (sourceDimension.dimension() != Level.END) {
            return server.getLevel(Level.END);
        }
        int portalId = -1;
        if (this.portalEntrancePos != null) {
            final BlockState state = sourceDimension.getBlockState(this.portalEntrancePos);
            if (state.hasProperty(EndPortalBlock.PORTAL)) {
                portalId = state.getValue(EndPortalBlock.PORTAL);
            }
        }
        return EndPortals.getWorld(server, portalId);
    }

    public void changeDimension(ServerLevel targetLevel) {
        if (!(this.level() instanceof ServerLevel) || this.entity.isRemoved()) {
            return;
        }
        this.level().getProfiler().push("be_findEntry");
        this.entity.unRide();

        //EternalRitual.findRitualForActivePortal(this.level(), portalEntrancePos);
        PortalInfo portalInfo = this.findDimensionEntryPoint(targetLevel);
        if (portalInfo == null) {
            return;
        }
        this.level().getProfiler().push("be_reposition");
        teleportEntity(targetLevel, portalInfo);

        this.level().getProfiler().pop();
        this.level().getProfiler().pop();
        return;
    }

    private void teleportEntity(ServerLevel serverLevel, PortalInfo portalInfo) {
        final EternalPortalLinks.Link arrival = this.pendingArrival;
        final EternalPortalLinks.Link departure = this.pendingDeparture;
        this.pendingArrival = null;
        this.pendingDeparture = null;
        Entity arrived = entity;
        if (entity instanceof ServerPlayer sp && sp.isCreative()) {
            sp.teleportTo(
                    serverLevel, portalInfo.pos.x + 0.5, portalInfo.pos.y, portalInfo.pos.z + 0.5,
                    entity.getYRot() + 180, entity.getXRot()
            );
            BECriteria.PORTAL_TRAVEL.trigger(sp);
        } else {
            if (entity instanceof ServerPlayer sp) {
                BECriteria.PORTAL_TRAVEL.trigger(sp);
                travelToDimension(sp, serverLevel, portalInfo);
            } else {
                arrived = sendToDimension(entity, serverLevel, portalInfo);
            }

            entity.setPortalCooldown();
        }
        if (arrived instanceof TravelingEntity te && te.be_getTravelerState() != null) {
            te.be_getTravelerState().arrivedAt = arrival;
            te.be_getTravelerState().cameFrom = departure;
        }
    }

    public static Entity sendToDimension(Entity traveler, ServerLevel serverLevel, @NotNull PortalInfo portalInfo) {
        final Level level = traveler.level();
        if (level instanceof ServerLevel sourceDimension && !traveler.isRemoved()) {
            traveler.unRide();

            Entity copy = traveler.getType().create(serverLevel);
            if (copy != null) {
                copy.restoreFrom(traveler);
                copy.moveTo(
                        portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z,
                        portalInfo.yRot, copy.getXRot()
                );
                copy.setDeltaMovement(portalInfo.speed);
                serverLevel.addDuringTeleport(copy);
            }

            traveler.removeAfterChangingDimensions();
            sourceDimension.resetEmptyTime();
            serverLevel.resetEmptyTime();

            return copy;
        }
        return null;
    }

    public static Entity travelToDimension(
            ServerPlayer traveler,
            ServerLevel serverLevel,
            @NotNull PortalInfo portalInfo
    ) {
        final LevelData levelData = serverLevel.getLevelData();
        final ServerLevel sourceDimension = traveler.serverLevel();
        final PlayerList playerList = traveler.server.getPlayerList();

        traveler.isChangingDimension = true;
        traveler.unRide();

        traveler.connection.send(new ClientboundRespawnPacket(
                serverLevel.dimensionTypeId(),
                serverLevel.dimension(),
                BiomeManager.obfuscateSeed(serverLevel.getSeed()),
                traveler.gameMode.getGameModeForPlayer(),
                traveler.gameMode.getPreviousGameModeForPlayer(),
                serverLevel.isDebug(),
                serverLevel.isFlat(),
                ClientboundRespawnPacket.KEEP_ALL_DATA,
                traveler.getLastDeathLocation(),
                traveler.getPortalCooldown()
        ));
        traveler.connection.send(new ClientboundChangeDifficultyPacket(
                levelData.getDifficulty(),
                levelData.isDifficultyLocked()
        ));

        playerList.sendPlayerPermissionLevel(traveler);

        sourceDimension.removePlayerImmediately(traveler, Entity.RemovalReason.CHANGED_DIMENSION);
        traveler.unsetRemoved();
        traveler.setServerLevel(serverLevel);
        traveler.connection.resetPosition();
        serverLevel.addDuringPortalTeleport(traveler);
        traveler.triggerDimensionChangeTriggers(sourceDimension);
        traveler.connection.teleport(
                portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z,
                portalInfo.yRot, portalInfo.xRot
        );

        playerList.sendLevelInfo(traveler, serverLevel);
        playerList.sendAllPlayerInfo(traveler);

        traveler.connection.send(new ClientboundPlayerAbilitiesPacket(traveler.getAbilities()));

        sendPlayerEffects(traveler);

        traveler.connection.send(new ClientboundLevelEventPacket(
                LevelEvent.SOUND_PORTAL_TRAVEL,
                BlockPos.ZERO,
                0,
                false
        ));
        traveler.lastSentExp = -1;
        traveler.lastSentHealth = -1.0F;
        traveler.lastSentFood = -1;

        sourceDimension.resetEmptyTime();
        serverLevel.resetEmptyTime();

        return traveler;
    }

    private static void sendPlayerEffects(ServerPlayer traveler) {
        for (MobEffectInstance mobEffectInstance : traveler.getActiveEffects()) {
            traveler.connection.send(new ClientboundUpdateMobEffectPacket(
                    traveler.getId(),
                    mobEffectInstance
            ));
        }
    }

    // /execute in the_end run tp 49346 61 400
    // /execute in overworld run tp 849 64 891
    @Nullable
    private PortalInfo findDimensionEntryPoint(ServerLevel targetLevel) {
        boolean toEnd = targetLevel.dimension() == Level.END;
        if (this.level().dimension() != Level.END && !toEnd) {
            return null;
        }
        if (this.portalEntrancePos == null) {
            return null;
        }
        final WorldBorder worldBorder = targetLevel.getWorldBorder();
        final BlockPos startingPos = PortalBuilder.getStartingPos(this.level(), targetLevel, entity, worldBorder);

        final BlockPos entranceCenter = this.getEntranceCenter();
        return this.findLinkedPortal(targetLevel, entranceCenter)
                   .or(() -> this.getExitPortal(targetLevel, startingPos, toEnd, worldBorder))
                   .map(foundRectangle -> {
            this.pendingArrival = new EternalPortalLinks.Link(
                    targetLevel.dimension(),
                    PortalBuilder.getPortalCenter(targetLevel, foundRectangle)
            );
            this.pendingDeparture = entranceCenter == null
                    ? null
                    : new EternalPortalLinks.Link(this.level().dimension(), entranceCenter);
            Vec3 vec3;
            Direction.Axis axis;
            BlockState blockState = this.level().getBlockState(this.portalEntrancePos);
            if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
                BlockUtil.FoundRectangle foundRectangle2 = BlockUtil.getLargestRectangleAround(
                        this.portalEntrancePos,
                        axis,
                        21,
                        Direction.Axis.Y,
                        21,
                        blockPos -> this.level().getBlockState(blockPos) == blockState
                );
                vec3 = this.getRelativePortalPosition(axis, foundRectangle2);
            } else {
                axis = Direction.Axis.X;
                vec3 = new Vec3(0.5, 0.0, 0.0);
            }
            return PortalShape.createPortalInfo(
                    targetLevel, foundRectangle, axis, vec3,
                    this.entity, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot()
            );
        }).orElse(null);
    }

    /**
     * The portal that the Eternal Portal the entity stands in is linked to. Exits to the Nether are placed around
     * the 8:1 scaled position, often some blocks away from it; scaled back up, that offset is far larger than the
     * search radius of {@link #getExitPortal}. So the way back follows the stored link to the portal the exit was
     * built for (#329). Rituals activated before links were stored add their links when their chunk loads.
     */
    private Optional<BlockUtil.FoundRectangle> findLinkedPortal(
            ServerLevel targetLevel,
            @Nullable BlockPos sourceCenter
    ) {
        if (sourceCenter == null || !(this.level() instanceof ServerLevel sourceLevel)) return Optional.empty();

        // Back through the portal this entity arrived at: return to the portal it came from
        if (this.cameFrom != null && this.arrivedAt != null
                && this.arrivedAt.equals(new EternalPortalLinks.Link(sourceLevel.dimension(), sourceCenter))) {
            final Optional<BlockUtil.FoundRectangle> back = this.getLinkedPortal(targetLevel, sourceCenter, this.cameFrom);
            if (back.isPresent()) return back;
        }

        return this.getLinkedPortal(targetLevel, sourceCenter, EternalPortalLinks.get(sourceLevel).get(sourceCenter));
    }

    private Optional<BlockUtil.FoundRectangle> getLinkedPortal(
            ServerLevel targetLevel,
            BlockPos sourceCenter,
            @Nullable EternalPortalLinks.Link link
    ) {
        if (link == null || !link.dimension().equals(targetLevel.dimension())) return Optional.empty();

        final BlockPos targetCenter = link.center();
        targetLevel.getChunkSource()
                   .addRegionTicket(TicketType.PORTAL, new ChunkPos(targetCenter), 3, targetCenter);
        if (!targetLevel.getBlockState(targetCenter).is(PortalBuilder.PORTAL)) {
            BetterEnd.LOGGER.warning(
                    "The Eternal Portal at {} linked to {} in {} no longer exists, searching for another one",
                    sourceCenter, targetCenter, targetLevel.dimension().location()
            );
            return Optional.empty();
        }
        return Optional.of(PortalBuilder.getPortalRectangle(targetLevel, targetCenter));
    }

    /**
     * The center of the Eternal Portal the entity stands in, or {@code null} if it is not in one anymore.
     */
    @Nullable
    private BlockPos getEntranceCenter() {
        if (!this.level().getBlockState(this.portalEntrancePos).is(PortalBuilder.PORTAL)) return null;
        return PortalBuilder.getPortalCenter(
                this.level(),
                PortalBuilder.getPortalRectangle(this.level(), this.portalEntrancePos)
        );
    }

    protected Optional<BlockUtil.FoundRectangle> getExitPortal(
            ServerLevel targetLevel,
            BlockPos startingPos,
            boolean bl,
            WorldBorder worldBorder
    ) {
        final PortalBuilder builder = new PortalBuilder(this.level(), targetLevel);
        final Optional<BlockUtil.FoundRectangle> portalRectangle = builder.findPortalAround(
                startingPos,
                worldBorder
        );
        if (portalRectangle.isPresent()) {
            return portalRectangle;
        }

        BetterEnd.LOGGER.error("Unable to locate an active portal");
        return portalRectangle;
    }

    protected Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle foundRectangle) {
        return PortalShape.getRelativePosition(
                foundRectangle,
                axis,
                entity.position(),
                entity.getDimensions(entity.getPose())
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (TravelerState) obj;
        return Objects.equals(this.entity, that.entity) &&
                Objects.equals(this.portalEntrancePos, that.portalEntrancePos) &&
                this.isInsidePortal == that.isInsidePortal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, portalEntrancePos, isInsidePortal);
    }

    @Override
    public String toString() {
        return "TravelerState[" +
                "entity=" + entity + ", " +
                "portalEntrancePos=" + portalEntrancePos + ", " +
                "isInsidePortal=" + isInsidePortal + ']';
    }

}
