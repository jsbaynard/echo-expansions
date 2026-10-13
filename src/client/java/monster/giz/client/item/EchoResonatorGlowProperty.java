package monster.giz.client.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import monster.giz.EchoExpansions;
import monster.giz.client.render.EchoResonatorGlows;
import monster.giz.world.item.EchoResonatorGlow;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

// Holder driven model predication
public record EchoResonatorGlowProperty(Optional<EchoResonatorGlow> state) implements ConditionalItemModelProperty {

    public static final Identifier ID = EchoExpansions.idOf("resonator_glow");

    public static final MapCodec<EchoResonatorGlowProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    EchoResonatorGlow.CODEC.optionalFieldOf("state").forGetter(EchoResonatorGlowProperty::state)
            ).apply(instance, EchoResonatorGlowProperty::new)
    );

    @Override
    public boolean get(
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity holder,
            int seed,
            ItemDisplayContext displayContext
    ) {
        if (level == null || holder == null) {
            return false;
        }

        EchoResonatorGlows.Glow glow = EchoResonatorGlows.get(holder.getId(), level.getGameTime());
        if (glow == null) {
            return false;
        }

        if (state.isPresent() && glow.state() != state.get()) {
            return false;
        }

        return isDrawingPressed(stack, displayContext, holder, glow.hand());
    }

    private static boolean isDrawingPressed(ItemStack stack, ItemDisplayContext context, LivingEntity holder, InteractionHand hand) {
        return switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND,
                 THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> isArmPressed(context, holder, hand);
            case GUI -> holder.getItemInHand(hand) == stack;
            default -> false;
        };
    }

    private static boolean isArmPressed(ItemDisplayContext context, LivingEntity holder, InteractionHand hand) {
        HumanoidArm pressed = hand == InteractionHand.MAIN_HAND
                ? holder.getMainArm()
                : holder.getMainArm().getOpposite();

        return context.leftHand() == (pressed == HumanoidArm.LEFT);
    }

    @Override
    public MapCodec<? extends ConditionalItemModelProperty> type() {
        return MAP_CODEC;
    }
}
