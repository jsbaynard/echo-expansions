package com.jozsmo.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.init.EEItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;


public class EchoResonatorDyeRecipe extends CustomRecipe {

    public static final MapCodec<EchoResonatorDyeRecipe> MAP_CODEC = MapCodec.unit(EchoResonatorDyeRecipe::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoResonatorDyeRecipe> STREAM_CODEC =
            StreamCodec.unit(new EchoResonatorDyeRecipe());

    public static final RecipeSerializer<EchoResonatorDyeRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack resonator = ItemStack.EMPTY;
        List<DyeColor> dyes = new ArrayList<>();

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(EEItems.ECHO_RESONATOR)) {
                if (!resonator.isEmpty() || !stack.has(EEDataComponentTypes.ECHO_RESONATOR_RIBBON)) {
                    return ItemStack.EMPTY;
                }
                resonator = stack;
                continue;
            }

            DyeColor dye = stack.get(DataComponents.DYE);
            if (dye == null) {
                return ItemStack.EMPTY;
            }
            dyes.add(dye);
        }

        if (resonator.isEmpty() || dyes.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return DyedItemColor.applyDyes(resonator, dyes);
    }

    @Override
    public RecipeSerializer<EchoResonatorDyeRecipe> getSerializer() {
        return SERIALIZER;
    }
}
