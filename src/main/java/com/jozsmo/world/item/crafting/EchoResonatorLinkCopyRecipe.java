package com.jozsmo.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.init.EEItems;
import com.jozsmo.world.item.component.EchoReceiverLinkComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

// Copies a link onto an unlinked resonator in the grid
public class EchoResonatorLinkCopyRecipe extends CustomRecipe {

    public static final MapCodec<EchoResonatorLinkCopyRecipe> MAP_CODEC = MapCodec.unit(EchoResonatorLinkCopyRecipe::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoResonatorLinkCopyRecipe> STREAM_CODEC =
            StreamCodec.unit(new EchoResonatorLinkCopyRecipe());

    public static final RecipeSerializer<EchoResonatorLinkCopyRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack linked = ItemStack.EMPTY;
        ItemStack unlinked = ItemStack.EMPTY;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (!stack.is(EEItems.ECHO_RESONATOR)) {
                return ItemStack.EMPTY;
            }

            if (isLinked(stack)) {
                if (!linked.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                linked = stack;
            } else {
                if (!unlinked.isEmpty()) {
                    return ItemStack.EMPTY;
                }
                unlinked = stack;
            }
        }

        if (linked.isEmpty() || unlinked.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = unlinked.copyWithCount(1);
        result.set(EEDataComponentTypes.ECHO_RECEIVER_LINK, linked.get(EEDataComponentTypes.ECHO_RECEIVER_LINK));
        return result;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < remaining.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            ItemStackTemplate craftingRemainder = stack.getItem().getCraftingRemainder();
            if (craftingRemainder != null) {
                remaining.set(slot, craftingRemainder.create());
            } else if (isLinked(stack)) {
                remaining.set(slot, stack.copyWithCount(1));
            }
        }

        return remaining;
    }

    private static boolean isLinked(ItemStack stack) {
        EchoReceiverLinkComponent link = stack.get(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        return link != null && link.receiver().isPresent();
    }

    @Override
    public RecipeSerializer<@NotNull EchoResonatorLinkCopyRecipe> getSerializer() {
        return SERIALIZER;
    }
}
