package org.betterx.betterend.item.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the geometry of the crystalite armor renderer (#603) without a running client: ModelPart and
 * PoseStack are plain classes, so the crystal parts can be "rendered" into a vertex-capturing buffer.
 * <p>
 * The renderer draws each crystal part under the wearer's own body part transform. Before #603 it
 * copied the wearer's part poses onto a separate full crystal model ({@code HumanoidModel#copyPropertiesTo},
 * shoulder pads / boot cuffs taking the arm / leg pose) and rendered that model. Both must put every
 * vertex at the same place, for the standing pose and for arbitrary poses of the six main body parts.
 * <p>
 * {@code copyPropertiesTo} never copied the root or the hat layer, so poses that move those are not
 * compared here: the old path ignored them, the new one follows them on purpose.
 */
class CrystaliteArmorPoseTest {
    private static final float EPSILON = 1.0e-5f;

    private static HumanoidModel<HumanoidRenderState> wearer() {
        return new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f), 64, 64)
                                                  .bakeRoot());
    }

    private static List<Consumer<HumanoidModel<?>>> poses() {
        return List.of(
                m -> {
                }, // standing
                m -> {
                    m.rightArm.xRot = -1.3f;
                    m.rightArm.zRot = 0.2f;
                    m.leftArm.xRot = 0.7f;
                    m.leftArm.yRot = -0.4f;
                },
                m -> {
                    m.head.xRot = 0.5f;
                    m.head.yRot = -0.9f;
                    m.head.zRot = 0.1f;
                },
                m -> {
                    // crouch-like: everything shifted and tilted
                    m.body.xRot = 0.5f;
                    m.body.y = 3.2f;
                    m.rightArm.y = 5.2f;
                    m.leftArm.y = 5.2f;
                    m.rightLeg.z = 4.0f;
                    m.leftLeg.z = 4.0f;
                    m.rightLeg.y = 12.2f;
                    m.leftLeg.y = 12.2f;
                    m.rightLeg.xRot = 0.9f;
                    m.leftLeg.xRot = -0.6f;
                    m.leftLeg.zRot = 0.3f;
                }
        );
    }

    @Test
    void crystalGeometryMatchesTheCopyPropertiesPath() {
        record Piece(
                String name,
                HumanoidModel<HumanoidRenderState> oldModel,
                CrystaliteArmorParts attachments,
                List<ModelPart> oldVisible
        ) {
        }
        CrystaliteHelmetModel helmet = CrystaliteHelmetModel.createModel(null);
        CrystaliteChestplateModel chest = CrystaliteChestplateModel.createRegularModel(null);
        CrystaliteChestplateModel slim = CrystaliteChestplateModel.createThinModel(null);
        CrystaliteLeggingsModel legs = CrystaliteLeggingsModel.createModel(null);
        CrystaliteBootsModel boots = CrystaliteBootsModel.createModel(null);
        List<Piece> pieces = List.of(
                new Piece("helmet", helmet, CrystaliteArmorParts.HELMET, List.of(helmet.head, helmet.hat)),
                new Piece(
                        "chestplate", chest, CrystaliteArmorParts.CHESTPLATE,
                        List.of(chest.body, chest.rightArm, chest.leftArm)
                ),
                new Piece(
                        "chestplate_slim", slim, CrystaliteArmorParts.CHESTPLATE_SLIM,
                        List.of(slim.body, slim.rightArm, slim.leftArm)
                ),
                new Piece("leggings", legs, CrystaliteArmorParts.LEGGINGS, List.of(legs.body, legs.leftLeg, legs.rightLeg)),
                new Piece("boots", boots, CrystaliteArmorParts.BOOTS, List.of(boots.rightLeg, boots.leftLeg))
        );

        for (Piece piece : pieces) {
            int poseIndex = 0;
            for (Consumer<HumanoidModel<?>> pose : poses()) {
                HumanoidModel<HumanoidRenderState> wearer = wearer();
                pose.accept(wearer);

                List<float[]> expected = renderOldPath(piece.oldModel(), piece.oldVisible(), wearer);
                List<float[]> actual = renderNewPath(piece.attachments(), wearer);

                String context = piece.name() + " pose #" + poseIndex++;
                assertTrue(expected.size() > 0, context + ": nothing rendered");
                assertEquals(expected.size(), actual.size(), context + ": vertex count");
                for (int i = 0; i < expected.size(); i++) {
                    float[] e = expected.get(i);
                    float[] a = actual.get(i);
                    for (int c = 0; c < e.length; c++) {
                        assertEquals(e[c], a[c], EPSILON, context + ": vertex " + i + " component " + c);
                    }
                }
            }
        }
    }

    @Test
    void hiddenBodyPartsHideTheirCrystalPart() {
        HumanoidModel<HumanoidRenderState> wearer = wearer();
        wearer.head.visible = false;
        wearer.body.visible = false;
        assertEquals(0, renderNewPath(CrystaliteArmorParts.HELMET, wearer).size());
        // body hidden, arms still visible: only the two shoulder pads remain
        assertEquals(2 * 24, renderNewPath(CrystaliteArmorParts.CHESTPLATE, wearer).size());

        // the hat layer is hidden by skin customisation, which must not hide the helmet
        HumanoidModel<HumanoidRenderState> noHat = wearer();
        noHat.hat.visible = false;
        assertEquals(24, renderNewPath(CrystaliteArmorParts.HELMET, noHat).size());

        HumanoidModel<HumanoidRenderState> hiddenRoot = wearer();
        hiddenRoot.root().visible = false;
        assertEquals(0, renderNewPath(CrystaliteArmorParts.BOOTS, hiddenRoot).size());
    }

    /**
     * What the renderer did before #603, minus the visibility inheritance (all parts visible here):
     * copyPropertiesTo, CopyExtraState, setAllVisible(false) + the slot's parts. The shoulder pads and
     * boot cuffs are not among the parts setAllVisible touches, so they stayed visible.
     */
    private static List<float[]> renderOldPath(
            HumanoidModel<HumanoidRenderState> model,
            List<ModelPart> visible,
            HumanoidModel<HumanoidRenderState> wearer
    ) {
        wearer.copyPropertiesTo(model);
        // the old CopyExtraState
        if (model instanceof CrystaliteChestplateModel chest) {
            chest.leftShoulder.copyFrom(chest.leftArm);
            chest.rightShoulder.copyFrom(chest.rightArm);
        }
        if (model instanceof CrystaliteBootsModel boots) {
            boots.leftBoot.copyFrom(boots.leftLeg);
            boots.rightBoot.copyFrom(boots.rightLeg);
        }
        model.setAllVisible(false);
        visible.forEach(part -> part.visible = true);

        CapturingBuffer buffer = new CapturingBuffer();
        model.renderToBuffer(entityPose(), buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        return buffer.sorted();
    }

    private static List<float[]> renderNewPath(
            CrystaliteArmorParts attachments,
            HumanoidModel<HumanoidRenderState> wearer
    ) {
        CapturingBuffer buffer = new CapturingBuffer();
        attachments.forEachVisible(wearer, entityPose(), (posed, crystal) ->
                crystal.render(posed, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, -1)
        );
        return buffer.sorted();
    }

    /** Roughly what LivingEntityRenderer pushes before the layers run. */
    private static PoseStack entityPose() {
        PoseStack stack = new PoseStack();
        stack.translate(10.5f, 64.0f, -3.25f);
        stack.mulPose(new org.joml.Quaternionf().rotationY(0.8f));
        stack.scale(-1.0f, -1.0f, 1.0f);
        stack.translate(0.0f, -1.501f, 0.0f);
        return stack;
    }

    private static final class CapturingBuffer implements VertexConsumer {
        private final List<float[]> vertices = new ArrayList<>();
        private float[] current;

        List<float[]> sorted() {
            List<float[]> result = new ArrayList<>(vertices);
            Comparator<float[]> order = Comparator.comparingDouble(v -> v[0]);
            for (int i = 1; i < 8; i++) {
                final int c = i;
                order = order.thenComparingDouble(v -> v[c]);
            }
            result.sort(order);
            return result;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            current = new float[]{x, y, z, 0, 0, 0, 0, 0};
            vertices.add(current);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            current[3] = u;
            current[4] = v;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            current[5] = x;
            current[6] = y;
            current[7] = z;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }
    }
}
