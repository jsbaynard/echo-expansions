package monster.giz.init;

import monster.giz.EchoExpansions;
import monster.giz.world.item.crafting.EchoResonatorDyeRecipe;
import monster.giz.world.item.crafting.EchoResonatorLinkCopyRecipe;
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

    private static <T extends Recipe<?>> RecipeSerializer<T> register(String name, RecipeSerializer<T> serializer) {
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, EchoExpansions.idOf(name), serializer);
    }

    public static void registerAll() {}
}
