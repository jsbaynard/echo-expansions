package com.jozsmo.client;

import com.jozsmo.client.render.EchoLinkBeams;
import com.jozsmo.client.render.EchoReceiverRenderer;
import com.jozsmo.client.render.EchoResonatorGlows;
import com.jozsmo.init.EEBlockEntities;
import com.jozsmo.network.EchoLinkBeamPayload;
import com.jozsmo.network.EchoResonatorGlowPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.multiplayer.ClientLevel;

public class EchoExpansionsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRendererRegistry.register(EEBlockEntities.ECHO_RECEIVER_BLOCK_ENTITY, context -> new EchoReceiverRenderer());

		ClientPlayNetworking.registerGlobalReceiver(EchoLinkBeamPayload.TYPE, (payload, context) -> {
			ClientLevel level = context.client().level;
			if (level != null) {
				EchoLinkBeams.add(payload, level.getGameTime());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(EchoResonatorGlowPayload.TYPE, (payload, context) -> {
			ClientLevel level = context.client().level;
			if (level != null) {
				EchoResonatorGlows.add(payload, level);
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			EchoLinkBeams.clear();
			EchoResonatorGlows.clear();
		});
	}
}
