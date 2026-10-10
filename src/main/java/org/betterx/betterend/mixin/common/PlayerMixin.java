package org.betterx.betterend.mixin.common;


import org.betterx.betterend.registry.block.EndFunctionalBlocks;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.registry.EndBlocks;
import de.ambertation.wover.block.api.BlockProperties;
import de.ambertation.wover.block.api.BlockProperties.TripleShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

// 26.1 retarget: the private static findRespawnAndUseSpawnBlock(ServerLevel, RespawnConfig, boolean) it
// used to hook returns the package-private ServerPlayer.RespawnPosAngle, which is no longer constructible
// from here (its old (Vec3, float) constructor was replaced by a private (Vec3, float, float) one). We hook
// the public wrapper findRespawnPositionAndUseSpawnBlock instead and build a TeleportTransition (public)
// directly - the wrapper is the sole caller of the private helper, so every respawn path is still covered.
@Mixin(value = ServerPlayer.class, priority = 200)
public abstract class PlayerMixin {
    private static Direction[] horizontal;

    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At(value = "HEAD"), cancellable = true)
    private void be_findRespawnAndUseSpawnBlock(
            boolean useSpawnBlock,
            TeleportTransition.PostTeleportTransition postTeleportTransition,
            CallbackInfoReturnable<TeleportTransition> info
    ) {
        final ServerPlayer self = (ServerPlayer) (Object) this;
        final ServerPlayer.RespawnConfig config = self.getRespawnConfig();
        if (config == null) return;

        final MinecraftServer server = self.level().getServer();
        if (server == null) return;

        final ServerLevel world = server.getLevel(config.respawnData().dimension());
        if (world == null) return;

        final BlockPos pos = config.respawnData().pos();
        final BlockState blockState = world.getBlockState(pos);
        if (blockState.is(EndFunctionalBlocks.RESPAWN_OBELISK)) {
            Optional<Vec3> op = be_obeliskRespawnPosition(world, pos, blockState);
            if (op.isEmpty()) return;
            info.setReturnValue(new TeleportTransition(
                    world,
                    op.get(),
                    Vec3.ZERO,
                    config.respawnData().yaw(),
                    0.0F,
                    postTeleportTransition
            ));
        }
    }

    private static Optional<Vec3> be_obeliskRespawnPosition(
            ServerLevel world,
            BlockPos pos,
            BlockState state
    ) {
        if (state.getValue(BlockProperties.TRIPLE_SHAPE) == TripleShape.TOP) {
            pos = pos.below(2);
        } else if (state.getValue(BlockProperties.TRIPLE_SHAPE) == TripleShape.MIDDLE) {
            pos = pos.below();
        }
        if (horizontal == null) {
            horizontal = BlocksHelper.makeHorizontal();
        }
        MHelper.shuffle(horizontal, world.getRandom());
        // Same checks as the vanilla respawn anchor (DismountHelper): the whole player box (feet and head) must
        // be free of collisions and there must be a floor to stand on. Like the anchor, a first pass also
        // rejects dangerous blocks (fire, lava, ...) and only if that finds nothing a second pass accepts them.
        // Neighbours one block lower are tried as well, so an obelisk standing on a step still works.
        for (boolean avoidDanger : new boolean[]{true, false}) {
            for (int dy = 0; dy >= -1; dy--) {
                for (Direction dir : horizontal) {
                    Vec3 standUp = DismountHelper.findSafeDismountLocation(
                            EntityType.PLAYER,
                            world,
                            pos.relative(dir).above(dy),
                            avoidDanger
                    );
                    if (standUp != null) {
                        return Optional.of(standUp);
                    }
                }
            }
        }
        return Optional.empty();
    }
}