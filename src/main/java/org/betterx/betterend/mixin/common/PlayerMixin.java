package org.betterx.betterend.mixin.common;


import org.betterx.betterend.registry.block.EndFunctionalBlocks;
import org.betterx.bclib.util.BlocksHelper;
import org.betterx.bclib.util.MHelper;
import org.betterx.betterend.registry.EndBlocks;
import de.ambertation.wover.block.api.BlockProperties;
import de.ambertation.wover.block.api.BlockProperties.TripleShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = ServerPlayer.class, priority = 200)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    private static Direction[] horizontal;

    @Inject(method = "findRespawnAndUseSpawnBlock", at = @At(value = "HEAD"), cancellable = true)
    private static void be_findRespawnAndUseSpawnBlock(
            ServerLevel world,
            ServerPlayer.RespawnConfig config,
            boolean bl2,
            CallbackInfoReturnable<Optional<ServerPlayer.RespawnPosAngle>> info
    ) {
        BlockPos pos = config.pos();
        BlockState blockState = world.getBlockState(pos);
        if (blockState.is(EndFunctionalBlocks.RESPAWN_OBELISK)) {
            info.setReturnValue(be_obeliskRespawnPosition(world, pos, config.angle(), blockState));
            info.cancel();
        }
    }

    private static Optional<ServerPlayer.RespawnPosAngle> be_obeliskRespawnPosition(
            ServerLevel world,
            BlockPos pos,
            float angle,
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
                        return Optional.of(new ServerPlayer.RespawnPosAngle(standUp, angle));
                    }
                }
            }
        }
        return Optional.empty();
    }
}