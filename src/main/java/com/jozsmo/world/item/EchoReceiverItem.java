package com.jozsmo.world.item;

import com.jozsmo.init.EEDataComponentTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.UUID;
import java.util.function.Consumer;

// A receiver only carries its identity once it has been picked up with relocation on. Showing
// the same trace its links show is what makes the two match up in an inventory.
public class EchoReceiverItem extends BlockItem {

    public EchoReceiverItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);

        UUID identity = stack.get(EEDataComponentTypes.ECHO_RECEIVER_IDENTITY);
        if (identity != null) {
            adder.accept(EchoWaveform.of(identity));
        }
    }
}
