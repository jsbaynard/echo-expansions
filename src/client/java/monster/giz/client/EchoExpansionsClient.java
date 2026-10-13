package monster.giz.client;

import monster.giz.client.render.EchoLinkBeams;
import monster.giz.client.render.EchoReceiverRenderer;
import monster.giz.client.render.EchoResonatorGlows;
import monster.giz.init.EEBlockEntities;
import monster.giz.network.EchoLinkBeamPayload;
import monster.giz.network.EchoResonatorGlowPayload;
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
