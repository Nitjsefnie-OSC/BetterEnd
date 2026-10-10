package org.betterx.betterend.item.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

/**
 * The geometry of one crystalite armor piece, split into the crystal parts that cover one body part each
 * (helmet -> head, chestplate -> body + both shoulder pads, ...).
 * <p>
 * Each crystal part is drawn under the transform of the wearer's matching body part - root, then the body
 * part itself, exactly the chain {@link ModelPart#render} applies - so it follows whatever pose the wearer's
 * model has. The crystal parts carry a constant identity pose and are never mutated after construction,
 * which keeps them safe to read from deferred render callbacks of any number of wearers.
 * <p>
 * Kept free of anything that needs a running game so its geometry can be checked in a plain unit test.
 */
@Environment(EnvType.CLIENT)
final class CrystaliteArmorParts {
    static final CrystaliteArmorParts HELMET = helmet(CrystaliteHelmetModel.createModel(null));
    static final CrystaliteArmorParts CHESTPLATE = chestplate(CrystaliteChestplateModel.createRegularModel(null));
    static final CrystaliteArmorParts CHESTPLATE_SLIM = chestplate(CrystaliteChestplateModel.createThinModel(null));
    static final CrystaliteArmorParts LEGGINGS = leggings(CrystaliteLeggingsModel.createModel(null));
    static final CrystaliteArmorParts BOOTS = boots(CrystaliteBootsModel.createModel(null));

    /**
     * One crystal part and the wearer's body part it is attached to.
     *
     * @param bone      selects the wearer's body part (a direct child of the model root in every
     *                  {@link HumanoidModel}, its constructor looks them up that way)
     * @param insideHat also apply the wearer's {@code hat} transform (the helmet geometry has always been
     *                  posed like the hat layer, which is a child of the head)
     * @param crystal   the crystal geometry, relative to the body part's pivot
     */
    private record Attachment(Function<HumanoidModel<?>, ModelPart> bone, boolean insideHat, ModelPart crystal) {
    }

    private final List<Attachment> attachments;

    private CrystaliteArmorParts(Attachment... attachments) {
        this.attachments = List.of(attachments);
    }

    static @Nullable CrystaliteArmorParts forSlot(HumanoidRenderState renderState, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD) return HELMET;
        if (slot == EquipmentSlot.LEGS) return LEGGINGS;
        if (slot == EquipmentSlot.FEET) return BOOTS;
        if (slot == EquipmentSlot.CHEST) {
            if (renderState instanceof PlayerRenderState playerRenderState
                    && playerRenderState.skin.model() == PlayerSkin.Model.SLIM) {
                return CHESTPLATE_SLIM;
            } else {
                return CHESTPLATE;
            }
        }
        return null;
    }

    /**
     * Pushes the wearer's transform for every visible crystal part and hands the pose stack together with
     * that crystal part to {@code sink}.
     * <p>
     * A crystal part is skipped when the wearer's body part (or the root) is hidden. First-person body
     * renderers (e.g. BetterCombat's attack animations) draw the local player with head and body hidden;
     * without this the crystal helmet and shoulder pads were drawn into the camera (#602). The hat's own
     * visibility is ignored on purpose: players hide it through their skin customisation, which must not
     * hide the helmet.
     */
    void forEachVisible(HumanoidModel<?> contextModel, PoseStack matrices, BiConsumer<PoseStack, ModelPart> sink) {
        ModelPart root = contextModel.root();
        if (!root.visible) return;
        for (Attachment attachment : attachments) {
            ModelPart bone = attachment.bone().apply(contextModel);
            if (!bone.visible) continue;

            matrices.pushPose();
            root.translateAndRotate(matrices);
            bone.translateAndRotate(matrices);
            if (attachment.insideHat()) {
                contextModel.hat.translateAndRotate(matrices);
            }
            sink.accept(matrices, attachment.crystal());
            matrices.popPose();
        }
    }

    private static Attachment attach(Function<HumanoidModel<?>, ModelPart> bone, ModelPart crystal) {
        return new Attachment(bone, false, detach(crystal));
    }

    /**
     * The baked layer definitions place some crystal parts at a pivot of their own (shoulder pads,
     * leggings, boots). That pivot used to be overwritten with the wearer's part pose every frame; now the
     * wearer's part supplies the whole transform, so the crystal part has to sit at the identity pose.
     */
    private static ModelPart detach(ModelPart crystal) {
        crystal.setInitialPose(PartPose.ZERO);
        crystal.resetPose();
        crystal.visible = true;
        return crystal;
    }

    private static CrystaliteArmorParts helmet(CrystaliteHelmetModel model) {
        return new CrystaliteArmorParts(new Attachment(m -> m.head, true, detach(model.myHat)));
    }

    private static CrystaliteArmorParts chestplate(CrystaliteChestplateModel model) {
        return new CrystaliteArmorParts(
                attach(m -> m.body, model.body),
                attach(m -> m.rightArm, model.rightShoulder),
                attach(m -> m.leftArm, model.leftShoulder)
        );
    }

    private static CrystaliteArmorParts leggings(CrystaliteLeggingsModel model) {
        return new CrystaliteArmorParts(
                attach(m -> m.body, model.body),
                attach(m -> m.rightLeg, model.rightLeg),
                attach(m -> m.leftLeg, model.leftLeg)
        );
    }

    private static CrystaliteArmorParts boots(CrystaliteBootsModel model) {
        return new CrystaliteArmorParts(
                attach(m -> m.rightLeg, model.rightBoot),
                attach(m -> m.leftLeg, model.leftBoot)
        );
    }
}
