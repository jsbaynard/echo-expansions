package com.jozsmo.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.phys.Vec3;

public class EchoLinkRenderState extends BlockEntityRenderState {

    public boolean active;
    public Vec3 toOrigin = Vec3.ZERO;
    public float strength;
}
