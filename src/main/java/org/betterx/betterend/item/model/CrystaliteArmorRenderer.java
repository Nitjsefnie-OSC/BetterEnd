package org.betterx.betterend.item.model;


import org.betterx.betterend.registry.item.EndEquipmentItems;
import org.betterx.betterend.BetterEnd;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
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
    private final static Identifier FIRST_LAYER = BetterEnd.C.mk(
            "textures/models/armor/crystalite_layer_1.png");
    private final static Identifier SECOND_LAYER = BetterEnd.C.mk(
            "textures/models/armor/crystalite_layer_2.png");

    private static CrystaliteArmorRenderer INSTANCE = null;

    public static void register() {
        if (INSTANCE == null) {
            INSTANCE = new CrystaliteArmorRenderer();
            // CRYSTALITE_ELYTRA deliberately excluded: it now has its own equipment asset
            // (assets/betterend/equipment/elytra_crystalite.json, a "wings" layer only) and
            // renders through vanilla's WingsLayer instead of this custom chestplate-shaped model.
            ArmorRenderer.register(
                    INSTANCE,
                    EndEquipmentItems.CRYSTALITE_HELMET,
                    EndEquipmentItems.CRYSTALITE_CHESTPLATE,
                    EndEquipmentItems.CRYSTALITE_LEGGINGS,
                    EndEquipmentItems.CRYSTALITE_BOOTS
            );
        }
    }

    private Identifier getTextureForSlot(boolean innerLayer) {
        return innerLayer ? SECOND_LAYER : FIRST_LAYER;
    }

    @Override
    public void render(
            PoseStack matrices,
            SubmitNodeCollector collector,
            ItemStack stack,
            HumanoidRenderState renderState,
            EquipmentSlot slot,
            int light,
            HumanoidModel<HumanoidRenderState> contextModel
    ) {
        CrystaliteArmorParts parts = CrystaliteArmorParts.forSlot(renderState, slot);
        if (parts == null) return;

        // No contextModel.setupAnim(renderState) here: the wearer's renderer already posed the model
        // before running its layers, and calling it again would reset poses other mods applied (#603).
        // The pose is baked into the submitted PoseStack right now, so the deferred callbacks only read
        // the (constant) crystal parts, never the shared, mutable context model.
        submitLayer(matrices, collector, parts, contextModel, light, getTextureForSlot(false));
        submitLayer(matrices, collector, parts, contextModel, light, getTextureForSlot(true));
    }

    private static void submitLayer(
            PoseStack matrices,
            SubmitNodeCollector collector,
            CrystaliteArmorParts parts,
            HumanoidModel<HumanoidRenderState> contextModel,
            int light,
            Identifier texture
    ) {
        parts.forEachVisible(contextModel, matrices, (posed, crystal) ->
                collector.submitCustomGeometry(posed, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
                    PoseStack local = new PoseStack();
                    local.last().set(pose);
                    crystal.render(local, buffer, light, OverlayTexture.NO_OVERLAY, -1);
                })
        );
    }
}
