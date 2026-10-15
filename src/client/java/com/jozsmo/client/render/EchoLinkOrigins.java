package com.jozsmo.client.render;

import com.jozsmo.init.EEItems;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Where the beam leaves from, aligned with the hand actually holding the resonator
public final class EchoLinkOrigins {

    private static final double ARM_REACH = 0.32;
    private static final double HAND_FORWARD = 0.42;
    private static final double SHOULDER_DROP = 0.615;


    private static final double VIEW_FORWARD = 0.40;
    private static final double VIEW_SIDE = 0.34;
    private static final double VIEW_DROP = 0.32;

    private EchoLinkOrigins() {
    }

    public static Vec3 handPosition(Entity holder, float partialTick) {
        if (!(holder instanceof LivingEntity living)) {
            return holder.getEyePosition(partialTick);
        }

        HumanoidArm arm = armHolding(living);
        // Vanilla's convention: the right arm sits on local -X before body yaw is applied
        double side = arm == HumanoidArm.RIGHT ? -ARM_REACH : ARM_REACH;

        if (isSeenThroughItsOwnEyes(living)) {
            return firstPersonHand(living, side, partialTick);
        }

        float bodyYaw = Mth.lerp(partialTick, living.yBodyRotO, living.yBodyRot) * Mth.DEG_TO_RAD;
        float pitch = living.getXRot(partialTick) * Mth.DEG_TO_RAD;

        // Swing about the shoulder as they look up and down, then turn with the body
        Vec3 reach = new Vec3(side, 0.0, HAND_FORWARD).xRot(-pitch).yRot(-bodyYaw);

        return living.getPosition(partialTick)
                .add(0.0, living.getEyeHeight() - SHOULDER_DROP, 0.0)
                .add(reach);
    }

    private static Vec3 firstPersonHand(LivingEntity holder, double side, float partialTick) {
        Vec3 forward = holder.getViewVector(partialTick);
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        right = right.normalize();
        Vec3 up = right.cross(forward);

        return holder.getEyePosition(partialTick)
                .add(forward.scale(VIEW_FORWARD))
                .add(right.scale(-Math.signum(side) * VIEW_SIDE))
                .add(up.scale(-VIEW_DROP));
    }


    private static HumanoidArm armHolding(LivingEntity holder) {
        if (holder.isUsingItem() && holder.getUseItem().is(EEItems.ECHO_RESONATOR)) {
            return armFor(holder, holder.getUsedItemHand());
        }
        if (holder.getItemInHand(InteractionHand.MAIN_HAND).is(EEItems.ECHO_RESONATOR)) {
            return holder.getMainArm();
        }
        if (holder.getItemInHand(InteractionHand.OFF_HAND).is(EEItems.ECHO_RESONATOR)) {
            return holder.getMainArm().getOpposite();
        }
        return holder.getMainArm();
    }

    private static HumanoidArm armFor(LivingEntity holder, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? holder.getMainArm() : holder.getMainArm().getOpposite();
    }

    private static boolean isSeenThroughItsOwnEyes(LivingEntity holder) {
        Minecraft minecraft = Minecraft.getInstance();
        return holder == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
    }
}
