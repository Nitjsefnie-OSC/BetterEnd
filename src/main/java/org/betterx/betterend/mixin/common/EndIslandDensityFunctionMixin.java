package org.betterx.betterend.mixin.common;

import org.betterx.betterend.world.generator.VoidRing;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Vanilla End terrain keeps the island cells closer than 1024 blocks to the center empty (the hard-coded 4096 =
 * 64 cells of 16 blocks, squared). Use the void ring radius stored with the world instead (quiqueck/BetterEnd#288);
 * for the default radius this is exactly 4096 again.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction")
public class EndIslandDensityFunctionMixin {
    @ModifyConstant(method = "getHeightValue", constant = @Constant(longValue = 4096L))
    private static long be_voidRingLimit(long original) {
        return VoidRing.vanillaIslandLimit();
    }
}
