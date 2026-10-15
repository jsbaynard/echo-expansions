package com.jozsmo.init;

import com.jozsmo.EchoExpansions;
import com.jozsmo.world.item.component.EchoReceiverLinkComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Unit;

import java.util.UUID;

public class EEDataComponentTypes {

    public static final DataComponentType<EchoReceiverLinkComponent> ECHO_RECEIVER_LINK = register(
            "echo_receiver_link",
            DataComponentType.<EchoReceiverLinkComponent>builder()
                    .persistent(EchoReceiverLinkComponent.CODEC)
                    .networkSynchronized(EchoReceiverLinkComponent.STREAM_CODEC)
                    .build()
    );


    // Set while the linked receiver is within reach, powered or not
    public static final DataComponentType<Unit> ECHO_RECEIVER_IN_RANGE = register(
            "receiver_in_range",
            DataComponentType.<Unit>builder()
                    .persistent(Unit.CODEC)
                    .networkSynchronized(Unit.STREAM_CODEC)
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

    // Rides a picked up receiver so it can be put back down as the same one
    public static final DataComponentType<UUID> ECHO_RECEIVER_IDENTITY = register(
            "receiver_identity",
            DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
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
