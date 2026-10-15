package com.jozsmo.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.jozsmo.init.EEBlocks;
import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.init.EEItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

// Drops a link that has nowhere left to point, for when the relocation rule keeps one alive
public class EchoReceiverLinkClearRecipe extends CustomRecipe {

    public static final MapCodec<EchoReceiverLinkClearRecipe> MAP_CODEC =
            MapCodec.unit(EchoReceiverLinkClearRecipe::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoReceiverLinkClearRecipe> STREAM_CODEC =
            StreamCodec.unit(new EchoReceiverLinkClearRecipe());

    public static final RecipeSerializer<EchoReceiverLinkClearRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack linked = ItemStack.EMPTY;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            // One carrier of a link and nothing else, so this never shadows the other recipes
            if (!linked.isEmpty() || !carriesLink(stack)) {
                return ItemStack.EMPTY;
            }
            linked = stack;
        }

        if (linked.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = linked.copyWithCount(1);
        result.remove(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        // The reach markers are kept up to date per tick; clear them so the sprite is right now
        result.remove(EEDataComponentTypes.ECHO_RECEIVER_IN_RANGE);
        result.remove(EEDataComponentTypes.ECHO_RECEIVER_LIVE);
        return result;
    }

    private static boolean carriesLink(ItemStack stack) {
        boolean holder = stack.is(EEItems.ECHO_RESONATOR) || stack.is(EEBlocks.ECHO_TRANSMITTER.asItem());
        return holder && stack.has(EEDataComponentTypes.ECHO_RECEIVER_LINK);
    }

    @Override
    public RecipeSerializer<EchoReceiverLinkClearRecipe> getSerializer() {
        return SERIALIZER;
    }
}
