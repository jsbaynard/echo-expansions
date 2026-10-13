package monster.giz.init;

import monster.giz.EchoExpansions;
import monster.giz.world.item.component.EchoReceiverLinkComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Unit;

public class EEDataComponentTypes {

    public static final DataComponentType<EchoReceiverLinkComponent> ECHO_RECEIVER_LINK = register(
            "echo_receiver_link",
            DataComponentType.<EchoReceiverLinkComponent>builder()
                    .persistent(EchoReceiverLinkComponent.CODEC)
                    .networkSynchronized(EchoReceiverLinkComponent.STREAM_CODEC)
                    .build()
    );


    // Set while the linked receiver is live and in reach
    public static final DataComponentType<Unit> ECHO_RECEIVER_LIVE = register(
            "receiver_live",
            DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(Unit.STREAM_CODEC)
                    .build()
    );

    public static final DataComponentType<Unit> ECHO_RESONATOR_RIBBON = register(
            "ribbon",
            DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(Unit.STREAM_CODEC)
                    .build()
    );

    private static <T> DataComponentType<T> register(String name, DataComponentType<T> type) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, EchoExpansions.idOf(name), type);
    }

    public static void registerAll() {}
}
