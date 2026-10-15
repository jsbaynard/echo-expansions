package com.jozsmo.world.item;

import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.world.item.component.EchoReceiverLinkComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

// Carries the link like any other transmitter, and tracks whether the receiver is in reach
// of whoever is holding it so the item model can say so
public class EchoTransmitterItem extends BlockItem {

    public EchoTransmitterItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        EchoReceiverLinkComponent link = stack.get(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        if (link != null) {
            link.appendTooltip(adder);
        }
    }

    // The reach marker changes as the holder walks about; that is not a new item, so it must
    // not re-equip in the hand
    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        EchoReceiverLinkComponent link = stack.get(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        boolean inRange = link != null && link.reachableFrom(level, entity.position()) != null;
        if (inRange == stack.has(EEDataComponentTypes.ECHO_RECEIVER_IN_RANGE)) {
            return;
        }

        if (inRange) {
            stack.set(EEDataComponentTypes.ECHO_RECEIVER_IN_RANGE, Unit.INSTANCE);
        } else {
            stack.remove(EEDataComponentTypes.ECHO_RECEIVER_IN_RANGE);
        }
    }
}
