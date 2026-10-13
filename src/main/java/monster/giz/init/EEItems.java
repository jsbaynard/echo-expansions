package monster.giz.init;

import monster.giz.EchoExpansions;
import monster.giz.world.item.EchoResonatorItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.UseEffects;

import java.util.function.Function;

public class EEItems {

    private static final UseEffects RESONATOR_USE_EFFECTS = new UseEffects(true, true, 1.0F);

    public static final Item ECHO_RESONATOR = register(
            "echo_resonator",
            EchoResonatorItem::new,
            new Item.Properties()
                    .stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(DataComponents.USE_EFFECTS, RESONATOR_USE_EFFECTS)
    );

    private static Item register(String name, Function<Item.Properties, Item> itemFactory, Item.Properties properties) {
        ResourceKey<Item> itemKey = keyOfItem(name);
        Item item = itemFactory.apply(properties.setId(itemKey));
        return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(EchoExpansions.MOD_ID, name));
    }

    private static void putItemInCreativeInventory(Item item) {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.insertAfter(Items.SPYGLASS, item.getDefaultInstance());
        });
    }

    public static void registerAll() {
        putItemsInTab();
    }

    public static void putItemsInTab() {
        putItemInCreativeInventory(ECHO_RESONATOR);
    }
}
