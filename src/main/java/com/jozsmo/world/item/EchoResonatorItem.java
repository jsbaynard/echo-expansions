package com.jozsmo.world.item;

import com.jozsmo.init.EEBlocks;
import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.init.EEGamerules;
import com.jozsmo.network.EchoResonatorGlowPayload;
import com.jozsmo.world.item.component.EchoReceiverLinkComponent;
import com.jozsmo.world.level.block.EchoReceiverBlock;
import com.jozsmo.world.level.block.entity.EchoReceiverBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class EchoResonatorItem extends Item {

    private static final int MAX_HOLD_TICKS = 72000;

    // How long a sent glow stays valid on the client
    private static final int GLOW_HOLD_TICKS = 20;
    // How often the glow is re-sent
    private static final int GLOW_REFRESH_INTERVAL = 10;

    // How long the resonator stays lit after the button is let go
    private static final int RELEASE_GLOW_TICKS = 5;

    // The press each player has going, by identity. Weak keys so a logout pins nothing.
    private final Map<Player, Hold> holds = new WeakHashMap<>();

    public EchoResonatorItem(Properties properties) {
        super(properties);
    }

    // How long a use costs, per gamerule
    private static int cooldownTicks(ServerLevel level) {
        return level.getGameRules().get(EEGamerules.ECHO_RESONATOR_COOLDOWN);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> adder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        if (stack.has(EEDataComponentTypes.ECHO_RECEIVER_LINK)) {
            adder.accept(EchoReceiverLinkComponent.ENTANGLED_TOOLTIP);
        }
    }

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        updateLinkComponent(stack, level);
        updateReach(stack, level, entity);
    }

    private void updateLinkComponent(ItemStack stack, ServerLevel level) {
        EchoReceiverLinkComponent linkComponent = stack.get(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        if (linkComponent == null) {
            return;
        }

        EchoReceiverLinkComponent updated = linkComponent.forLevel(level);
        if (linkComponent.equals(updated)) {
            return;
        }

        // Drops the component instead of storing an empty link
        if (updated.receiver().isEmpty()) {
            stack.remove(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        } else {
            stack.set(EEDataComponentTypes.ECHO_RECEIVER_LINK, updated);
        }
    }

    // Marks the stack with how close the linked receiver is: within reach, and live with it
    private void updateReach(ItemStack stack, ServerLevel level, Entity entity) {
        BlockState receiver = reachableReceiver(stack, level, entity);
        mark(stack, EEDataComponentTypes.ECHO_RECEIVER_IN_RANGE, receiver != null);
        mark(stack, EEDataComponentTypes.ECHO_RECEIVER_LIVE, receiver != null && EchoReceiverBlock.isActivated(receiver));
    }

    private static void mark(ItemStack stack, DataComponentType<Unit> type, boolean wanted) {
        if (wanted == stack.has(type)) {
            return;
        }

        if (wanted) {
            stack.set(type, Unit.INSTANCE);
        } else {
            stack.remove(type);
        }
    }

    // The linked receiver if it is standing within reach right now, else null
    private @Nullable BlockState reachableReceiver(ItemStack stack, ServerLevel level, Entity entity) {
        EchoReceiverLinkComponent linkComponent = stack.get(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        return linkComponent == null ? null : linkComponent.reachableFrom(level, entity.position());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Only SUCCESS breaks the hand loop in startUseItem, so a click reaches the off hand
        // either way. With one in each hand, the main hand is the button that was pressed.
        if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().is(this)) {
            return InteractionResult.PASS;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        if (stack.get(EEDataComponentTypes.ECHO_RECEIVER_LINK) == null) {
            return InteractionResult.PASS;
        }

        player.startUsingItem(hand);
        playSound(level, player);

        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            beginUse(serverLevel, serverPlayer, stack, hand);
        }

        // FAIL just to stop the swing and item bob
        return InteractionResult.FAIL;
    }

    // Powers the receiver on the click itself, or waits for it to come within reach
    private void beginUse(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Hold hold = new Hold(stack);
        holds.put(player, hold);

        retarget(level, player, hold, 0);
        sendGlow(player, hand, hold.glow(), GLOW_HOLD_TICKS);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_HOLD_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return;
        }

        InteractionHand hand = player.getUsedItemHand();
        Hold hold = holds.get(player);

        // Anything but the stack that started the hold ends it, as does a link that is gone
        // for good. One that is merely out of reach does not.
        if (hold == null || hold.stack != stack || !stack.has(EEDataComponentTypes.ECHO_RECEIVER_LINK)) {
            holds.remove(player);
            dropHold(serverLevel, player, hold, stack);
            return;
        }

        int heldTicks = MAX_HOLD_TICKS - remainingUseTicks;
        retarget(serverLevel, player, hold, heldTicks);

        if (heldTicks > 0 && heldTicks % GLOW_REFRESH_INTERVAL == 0) {
            sendGlow(player, hand, hold.glow(), GLOW_HOLD_TICKS);
        }
    }

    // Takes the receiver up as it comes within reach, and lets go again as it leaves
    private void retarget(ServerLevel level, ServerPlayer player, Hold hold, int heldTicks) {
        EchoReceiverBlockEntity receiver = locate(level, hold.stack);
        boolean reachable = receiver != null
                && EchoReceiverBlockEntity.isWithinRange(level, player.position(), receiver.getBlockPos());

        if (!reachable) {
            if (hold.engaged) {
                hold.engaged = false;
                if (receiver != null) {
                    receiver.pauseSustain(player);
                }
            }
            return;
        }

        if (hold.engaged) {
            receiver.continueSustain(player, heldTicks);
        } else {
            hold.engaged = receiver.beginSustain(player);
        }
    }

    // Ends a hold that stopped without being released: scrolled away, swapped hands, or the
    // stack changed. The caller owns the map entry.
    private void dropHold(ServerLevel level, ServerPlayer player, @Nullable Hold hold, ItemStack stack) {
        if (hold != null && hold.engaged) {
            EchoReceiverBlockEntity receiver = locate(level, hold.stack);
            if (receiver != null) {
                receiver.endSustain(player);
            }
        }

        player.getCooldowns().addCooldown(stack, cooldownTicks(level));
        player.stopUsingItem();
        sendGlow(player, InteractionHand.MAIN_HAND, EchoResonatorGlow.ACTIVE, 0);
    }

    // Ends any hold whose owner is no longer pressing
    public void dropStaleHolds() {
        if (holds.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Player, Hold>> iterator = holds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, Hold> entry = iterator.next();

            if (!(entry.getKey() instanceof ServerPlayer player)
                    || player.hasDisconnected()
                    || !(player.level() instanceof ServerLevel level)) {
                // Nobody to cool down, and nobody to tell
                iterator.remove();
                continue;
            }

            Hold hold = entry.getValue();
            if (player.isUsingItem() && player.getUseItem() == hold.stack) {
                continue;
            }

            iterator.remove();
            dropHold(level, player, hold, hold.stack);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return true;
        }

        Hold hold = holds.remove(player);
        if (hold != null && hold.engaged) {
            EchoReceiverBlockEntity receiver = locate(serverLevel, stack);
            if (receiver != null) {
                receiver.endSustain(player);
            }
        }

        // Letting go costs the cooldown whether or not the press ever reached anything
        player.getCooldowns().addCooldown(stack, cooldownTicks(serverLevel));
        sendGlow(player, player.getUsedItemHand(), EchoResonatorGlow.ACTIVE, RELEASE_GLOW_TICKS);
        return true;
    }

    // Tells the holder and everyone tracking them how this resonator looks. Zero duration clears it.
    private static void sendGlow(ServerPlayer player, InteractionHand hand, EchoResonatorGlow state, int durationTicks) {
        EchoResonatorGlowPayload payload = new EchoResonatorGlowPayload(
                player.getId(),
                hand == InteractionHand.OFF_HAND,
                state,
                durationTicks
        );

        ServerPlayNetworking.send(player, payload);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            if (viewer != player) {
                ServerPlayNetworking.send(viewer, payload);
            }
        }
    }

    // The receiver this resonator points at wherever it stands, or null if the link leads
    // nowhere. Range is decided by the caller.
    private @Nullable EchoReceiverBlockEntity locate(ServerLevel level, ItemStack stack) {
        EchoReceiverLinkComponent linkComponent = stack.get(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        if (linkComponent == null) {
            return null;
        }

        Optional<GlobalPos> target = linkComponent.forLevel(level).resolve(level);
        if (target.isEmpty()) {
            stack.remove(EEDataComponentTypes.ECHO_RECEIVER_LINK);
            return null;
        }

        GlobalPos targetGlobal = target.get();
        if (!targetGlobal.dimension().equals(level.dimension())) {
            return null;
        }

        return linkComponent.entangledAt(level, targetGlobal.pos());
    }

    // One player's press: the stack it started on, and whether it is reaching the receiver
    private static final class Hold {

        private final ItemStack stack;
        private boolean engaged;

        private Hold(ItemStack stack) {
            this.stack = stack;
        }

        private EchoResonatorGlow glow() {
            return engaged ? EchoResonatorGlow.ACTIVE : EchoResonatorGlow.ATTEMPTING;
        }
    }

    private void playSound(Level level, Player player) {
        level.playSound(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.STONE_BUTTON_CLICK_ON,
                SoundSource.PLAYERS,
                0.35F,
                1.5F
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();

        if (!level.getBlockState(blockPos).is(EEBlocks.ECHO_RECEIVER)) {
            return super.useOn(context);
        }

        if (level instanceof ServerLevel serverLevel) {
            linkToReceiver(serverLevel, context, blockPos);
        }

        return InteractionResult.SUCCESS;
    }

    private void linkToReceiver(ServerLevel level, UseOnContext context, BlockPos blockPos) {
        ItemStack stack = context.getItemInHand();

        ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;

        EchoReceiverBlockEntity be = EchoReceiverBlockEntity.get(level, blockPos);
        if (be == null) {
            return;
        }

        stack.set(EEDataComponentTypes.ECHO_RECEIVER_LINK, EchoReceiverLinkComponent.of(be.identity(level)));
        be.flashLink(player, context.getClickLocation());

        if (player != null) {
            // Lights for exactly as long as the receiver it just bound to
            sendGlow(player, context.getHand(), EchoResonatorGlow.ACTIVE, EchoReceiverBlockEntity.LINK_FLASH_TICKS);
            player.getCooldowns().addCooldown(stack, cooldownTicks(level));
        }
    }
}
