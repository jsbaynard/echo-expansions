package com.jozsmo.init;

import com.jozsmo.EchoExpansions;
import com.jozsmo.world.item.crafting.EchoReceiverLinkClearRecipe;
import com.jozsmo.world.item.crafting.EchoResonatorDyeRecipe;
import com.jozsmo.world.item.crafting.EchoResonatorLinkCopyRecipe;
import com.jozsmo.world.item.crafting.EchoResonatorRibbonRemoveRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class EERecipeSerializers {

    public static final RecipeSerializer<EchoResonatorDyeRecipe> ECHO_RESONATOR_DYE = register(
            "echo_resonator_dye",
            EchoResonatorDyeRecipe.SERIALIZER
    );

    public static final RecipeSerializer<EchoResonatorLinkCopyRecipe> ECHO_RESONATOR_LINK_COPY = register(
            "echo_resonator_link_copy",
            EchoResonatorLinkCopyRecipe.SERIALIZER
    );

    public static final RecipeSerializer<EchoResonatorRibbonRemoveRecipe> ECHO_RESONATOR_RIBBON_REMOVE = register(
            "echo_resonator_ribbon_remove",
            EchoResonatorRibbonRemoveRecipe.SERIALIZER
    );

    public static final RecipeSerializer<EchoReceiverLinkClearRecipe> ECHO_RECEIVER_LINK_CLEAR = register(
            "echo_receiver_link_clear",
            EchoReceiverLinkClearRecipe.SERIALIZER
    );

    private static <T extends Recipe<?>> RecipeSerializer<T> register(String name, RecipeSerializer<T> serializer) {
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, EchoExpansions.idOf(name), serializer);
    }

    public static void registerAll() {}
}
