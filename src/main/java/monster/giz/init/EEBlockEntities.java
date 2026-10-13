package monster.giz.init;

import monster.giz.EchoExpansions;
import monster.giz.world.level.block.entity.EchoReceiverBlockEntity;
import monster.giz.world.level.block.entity.EchoTransmitterBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class EEBlockEntities {

    public static final BlockEntityType<EchoReceiverBlockEntity> ECHO_RECEIVER_BLOCK_ENTITY =
            register("echo_receiver", EchoReceiverBlockEntity::new, EEBlocks.ECHO_RECEIVER);

    public static final BlockEntityType<EchoTransmitterBlockEntity> ECHO_TRANSMITTER_BLOCK_ENTITY =
            register("echo_transmitter", EchoTransmitterBlockEntity::new, EEBlocks.ECHO_TRANSMITTER);

    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
            Block... blocks
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(EchoExpansions.MOD_ID, name);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build());
    }

    public static void registerAll() {}
}
