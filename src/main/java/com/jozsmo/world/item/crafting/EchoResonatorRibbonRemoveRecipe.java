package com.jozsmo.world.item.crafting;

import com.mojang.serialization.MapCodec;
import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.init.EEItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

// Cuts the ribbon back off a ribboned resonator with shears, keeping its link
public class EchoResonatorRibbonRemoveRecipe extends CustomRecipe {

    public static final MapCodec<EchoResonatorRibbonRemoveRecipe> MAP_CODEC =
            MapCodec.unit(EchoResonatorRibbonRemoveRecipe::new);

    public static final StreamCodec<RegistryFriendlyByteBuf, EchoResonatorRibbonRemoveRecipe> STREAM_CODEC =
            StreamCodec.unit(new EchoResonatorRibbonRemoveRecipe());

    public static final RecipeSerializer<EchoResonatorRibbonRemoveRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack resonator = ItemStack.EMPTY;
        boolean shears = false;

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
            } else if (stack.is(Items.SHEARS)) {
                if (shears) {
                    return ItemStack.EMPTY;
                }
                shears = true;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (resonator.isEmpty() || !shears) {
            return ItemStack.EMPTY;
        }

        ItemStack result = resonator.copyWithCount(1);
        result.remove(EEDataComponentTypes.ECHO_RESONATOR_RIBBON);
        // The colour was the ribbon's, and only the ribbon layer is tinted by it
        result.remove(DataComponents.DYED_COLOR);
        return result;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < remaining.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(Items.SHEARS)) {
                remaining.set(slot, worn(stack));
            }
        }

        return remaining;
    }

    // The shears come back a point of durability worse off, and break on the last one
    private static ItemStack worn(ItemStack shears) {
        ItemStack kept = shears.copyWithCount(1);
        if (!kept.isDamageableItem()) {
            return kept;
        }

        int damage = kept.getDamageValue() + 1;
        if (damage >= kept.getMaxDamage()) {
            return ItemStack.EMPTY;
        }

        kept.setDamageValue(damage);
        return kept;
    }

    @Override
    public RecipeSerializer<EchoResonatorRibbonRemoveRecipe> getSerializer() {
        return SERIALIZER;
    }
}
