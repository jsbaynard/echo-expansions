package monster.giz.init;

import monster.giz.EchoExpansions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class EESounds {

    public static final SoundEvent ECHO_RESONANCE = register("echo_resonance");

    private static SoundEvent register(String id) {
        Identifier identifier = EchoExpansions.idOf(id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
    }

    public static void registerAll() {}
}
