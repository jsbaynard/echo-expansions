package monster.giz;

import monster.giz.init.EEBlockEntities;
import monster.giz.init.EEBlocks;
import monster.giz.init.EEDataComponentTypes;
import monster.giz.init.EEGamerules;
import monster.giz.init.EEItems;
import monster.giz.init.EERecipeSerializers;
import monster.giz.init.EESounds;
import monster.giz.network.EchoLinkBeamPayload;
import monster.giz.network.EchoResonatorGlowPayload;
import monster.giz.world.item.EchoResonatorItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

public class EchoExpansions implements ModInitializer {

	public static final String MOD_ID = "echo-expansions";

	public static Identifier idOf(String resource) {
		return Identifier.tryBuild(MOD_ID, resource);
	}

	@Override
	public void onInitialize() {
		EEGamerules.registerAll();
		EESounds.registerAll();
		EEDataComponentTypes.registerAll();
		EEBlocks.registerAll();
		EEBlockEntities.registerAll();
		EEItems.registerAll();
		EERecipeSerializers.registerAll();

		PayloadTypeRegistry.clientboundPlay().register(EchoLinkBeamPayload.TYPE, EchoLinkBeamPayload.STREAM_CODEC);
		PayloadTypeRegistry.clientboundPlay().register(EchoResonatorGlowPayload.TYPE, EchoResonatorGlowPayload.STREAM_CODEC);

		// Sweeps holds whose owner is no longer pressing
		if (EEItems.ECHO_RESONATOR instanceof EchoResonatorItem resonator) {
			ServerTickEvents.END_SERVER_TICK.register(server -> resonator.dropStaleHolds());
		}
	}
}
