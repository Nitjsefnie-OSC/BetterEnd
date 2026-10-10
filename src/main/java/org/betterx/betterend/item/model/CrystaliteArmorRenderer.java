package org.betterx.betterend.item.model;


import org.betterx.betterend.registry.item.EndEquipmentItems;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.registry.EndItems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;

/**
 * Renders the crystalite armor directly on top of the wearer's own model parts, see
 * {@link CrystaliteArmorParts}. The wearer's model already carries the final pose when the armor layer
 * runs (vanilla calls {@code setupAnim} right before the layers, and pose mods such as Entity Model
 * Features / Fresh Animations apply theirs on top of that), so the armor follows whatever pose is
 * actually rendered (#603).
 */
@Environment(EnvType.CLIENT)
public class CrystaliteArmorRenderer implements ArmorRenderer {
    private final static ResourceLocation FIRST_LAYER = BetterEnd.C.mk(
            "textures/models/armor/crystalite_layer_1.png");
    private final static ResourceLocation SECOND_LAYER = BetterEnd.C.mk(
            "textures/models/armor/crystalite_layer_2.png");
    private static CrystaliteArmorRenderer INSTANCE = null;

    public static void register() {
        if (INSTANCE == null) {
            INSTANCE = new CrystaliteArmorRenderer();
            // CRYSTALITE_ELYTRA deliberately excluded: it now has its own equipment asset
            // (assets/betterend/equipment/elytra_crystalite.json, a "wings" layer only) and
            // renders through vanilla's wings layer instead of this custom chestplate-shaped model.
            ArmorRenderer.register(
                    INSTANCE,
                    EndEquipmentItems.CRYSTALITE_HELMET,
                    EndEquipmentItems.CRYSTALITE_CHESTPLATE,
                    EndEquipmentItems.CRYSTALITE_LEGGINGS,
                    EndEquipmentItems.CRYSTALITE_BOOTS
            );
        }
    }

    private ResourceLocation getTextureForSlot(boolean innerLayer) {
        return innerLayer ? SECOND_LAYER : FIRST_LAYER;
    }

    @Override
    public void render(
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            ItemStack stack,
            HumanoidRenderState renderState,
            EquipmentSlot slot,
            int light,
            HumanoidModel<HumanoidRenderState> contextModel
    ) {
        CrystaliteArmorParts parts = CrystaliteArmorParts.forSlot(renderState, slot);
        if (parts == null) return;

        // Do not copy the wearer's pose onto a separate crystal model: draw the crystal parts under the
        // wearer's own part transforms, so poses other mods applied on top of setupAnim are kept (#603).
        renderLayer(matrices, vertexConsumers, stack, parts, contextModel, light, getTextureForSlot(false));
        renderLayer(matrices, vertexConsumers, stack, parts, contextModel, light, getTextureForSlot(true));
    }

    private static void renderLayer(
            PoseStack matrices,
            MultiBufferSource vertexConsumers,
            ItemStack stack,
            CrystaliteArmorParts parts,
            HumanoidModel<HumanoidRenderState> contextModel,
            int light,
            ResourceLocation texture
    ) {
        // Same buffer ArmorRenderer.renderPart used before
        VertexConsumer buffer = ItemRenderer.getArmorFoilBuffer(
                vertexConsumers,
                RenderType.armorCutoutNoCull(texture),
                stack.hasFoil()
        );
        parts.forEachVisible(contextModel, matrices, (posed, crystal) ->
                crystal.render(posed, buffer, light, OverlayTexture.NO_OVERLAY, -1)
        );
    }
}
