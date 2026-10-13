package monster.giz.init;

import monster.giz.EchoExpansions;
import monster.giz.world.level.block.EchoReceiverBlock;
import monster.giz.world.level.block.EchoTransmitterBlock;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class EEBlocks {

    public static final Block ECHO_RECEIVER = register(
            "echo_receiver",
            EchoReceiverBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK).lightLevel(state -> state.getValue(EchoReceiverBlock.ACTIVATED) ? 8 : 0),
            true
    );

    public static final Block ECHO_TRANSMITTER = register(
            "echo_transmitter",
            EchoTransmitterBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.REPEATER),
            true
    );

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, BlockBehaviour.Properties settings, boolean shouldRegisterItem) {
        ResourceKey<Block> blockKey = keyOfBlock(name);
        Block block = blockFactory.apply(settings.setId(blockKey));
        if (shouldRegisterItem) {
            ResourceKey<Item> itemKey = keyOfItem(name);

            BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
            Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        }

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(EchoExpansions.MOD_ID, name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(EchoExpansions.MOD_ID, name));
    }

    private static void putItemInCreativeInventory(Block block) {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(content -> {
            content.insertAfter(Items.SCULK_SENSOR, block.asItem().getDefaultInstance());
        });
    }

    public static void registerAll() {
        putItemsInTab();
    }

    public static void putItemsInTab() {
        putItemInCreativeInventory(ECHO_RECEIVER);
        putItemInCreativeInventory(ECHO_TRANSMITTER);
    }
}
