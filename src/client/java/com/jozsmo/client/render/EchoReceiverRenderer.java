package com.jozsmo.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.jozsmo.world.level.block.entity.EchoReceiverBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class EchoReceiverRenderer implements BlockEntityRenderer<EchoReceiverBlockEntity, EchoLinkRenderState> {

    private static final RenderType BEAM_RENDER_TYPE = RenderTypes.lightning();

    // Bright aqua blue
    private static final float R = 0.16F;
    private static final float G = 0.95F;
    private static final float B = 1.00F;

    private static final float HALF_WIDTH = 0.018F;

    private static final float BASE_ALPHA = 0.45F;

    private static final float FALLOFF = 0.40F;


    private static final float ORIGIN_CLEARANCE = 0.08F;
    private static final float RECEIVER_CLEARANCE = 0.45F;

    @Override
    public EchoLinkRenderState createRenderState() {
        return new EchoLinkRenderState();
    }

    @Override
    public void extractRenderState(
            EchoReceiverBlockEntity blockEntity,
            EchoLinkRenderState state,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
        state.active = false;

        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        long gameTime = level.getGameTime();
        EchoLinkBeams.Beam beam = EchoLinkBeams.get(blockEntity.getBlockPos(), gameTime);
        if (beam == null) {
            return;
        }

        state.active = true;
        state.toOrigin = EchoLinkBeams.originOf(beam, level, partialTick)
                .subtract(Vec3.atCenterOf(blockEntity.getBlockPos()));
        state.strength = EchoLinkBeams.beamOpacity(beam, gameTime, partialTick);
    }

    @Override
    public void submit(EchoLinkRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.active || state.strength <= 0.0F) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        collector.submitCustomGeometry(poseStack, BEAM_RENDER_TYPE, (pose, consumer) -> emitLine(pose, consumer, state));
        poseStack.popPose();
    }

    private static void emitLine(PoseStack.Pose pose, VertexConsumer consumer, EchoLinkRenderState state) {
        double length = state.toOrigin.length();
        double end = length - RECEIVER_CLEARANCE;
        if (end - ORIGIN_CLEARANCE <= 0.0) {
            return;
        }

        Vec3 direction = state.toOrigin.scale(-1.0 / length);
        Vec3 reference = Math.abs(direction.y) > 0.99 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction);

        float alphaFrom = alphaAt(state, ORIGIN_CLEARANCE / length);
        float alphaTo = alphaAt(state, end / length);
        if (alphaFrom <= 0.01F && alphaTo <= 0.01F) {
            return;
        }

        Vec3 from = state.toOrigin.add(direction.scale(ORIGIN_CLEARANCE));
        Vec3 to = state.toOrigin.add(direction.scale(end));

        emitPlane(pose, consumer, from, to, right, alphaFrom, alphaTo);
        emitPlane(pose, consumer, from, to, up, alphaFrom, alphaTo);
    }

    private static float alphaAt(EchoLinkRenderState state, double along) {
        return BASE_ALPHA * state.strength * (1.0F - FALLOFF * (float) along);
    }

    private static void emitPlane(PoseStack.Pose pose, VertexConsumer consumer, Vec3 from, Vec3 to, Vec3 perpendicular, float alphaFrom, float alphaTo) {
        Vec3 offset = perpendicular.scale(HALF_WIDTH);
        Vec3 a = from.add(offset);
        Vec3 b = from.subtract(offset);
        Vec3 c = to.subtract(offset);
        Vec3 d = to.add(offset);

        emitQuad(pose, consumer, a, b, c, d, alphaFrom, alphaTo);
        emitQuad(pose, consumer, d, c, b, a, alphaTo, alphaFrom);
    }

    private static void emitQuad(PoseStack.Pose pose, VertexConsumer consumer, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float alphaAB, float alphaCD) {
        vertex(pose, consumer, a, alphaAB);
        vertex(pose, consumer, b, alphaAB);
        vertex(pose, consumer, c, alphaCD);
        vertex(pose, consumer, d, alphaCD);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, Vec3 position, float alpha) {
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(R, G, B, alpha);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
