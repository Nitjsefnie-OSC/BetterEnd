package org.betterx.betterend.mixin.client;

import org.betterx.bclib.items.elytra.BCLElytraItem;
import org.betterx.bclib.items.elytra.BCLElytraUtils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public class CapeLayerMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void be_checkCustomElytra(
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int i,
            AbstractClientPlayer abstractClientPlayer,
            float f,
            float g,
            float h,
            float j,
            float k,
            float l,
            CallbackInfo info
    ) {
        // vanilla only hides the cape for Items.ELYTRA; do the same for every custom elytra (armored AND crystalite,
        // quiqueck/BetterEnd#295), including one worn in an elytra slot provided by an integration (e.g. Trinkets)
        ItemStack itemStack = abstractClientPlayer.getItemBySlot(EquipmentSlot.CHEST);
        if (itemStack.getItem() instanceof BCLElytraItem) {
            info.cancel();
            return;
        }
        if (BCLElytraUtils.slotProvider != null) {
            ItemStack elytra = BCLElytraUtils.slotProvider.getElytra(
                    abstractClientPlayer,
                    abstractClientPlayer::getItemBySlot
            );
            if (elytra != null && elytra.getItem() instanceof BCLElytraItem) {
                info.cancel();
            }
        }
    }
}
