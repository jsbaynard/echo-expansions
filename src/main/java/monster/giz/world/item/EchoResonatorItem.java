package monster.giz.world.item;

import monster.giz.init.EEBlocks;
import monster.giz.init.EEDataComponentTypes;
import monster.giz.init.EEGamerules;
import monster.giz.network.EchoResonatorGlowPayload;
import monster.giz.world.item.component.EchoReceiverLinkComponent;
import monster.giz.world.level.block.EchoReceiverBlock;
import monster.giz.world.level.block.entity.EchoReceiverBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public class EchoResonatorItem extends Item {

    private static final int MAX_HOLD_TICKS = 72000;

    // How long a sent glow stays valid on the client
    private static final int GLOW_HOLD_TICKS = 20;
    // How often the glow is re-sent
    private static final int GLOW_REFRESH_INTERVAL = 10;

    // How long the resonator stays lit after the button is let go
    private static final int RELEASE_GLOW_TICKS = 5;

    // How long a press that reached nothing shows the attempting look
    private static final int PRESS_FLASH_TICKS = 5;

    // The stack each player is holding the button on, by identity.
    // Weak keys so a logout pins nothing.
    private final Map<Player, ItemStack> holds = new WeakHashMap<>();

    public EchoResonatorItem(Properties properties) {
        super(properties);
    }

    // How long a use costs, per gamerule
    private static int cooldownTicks(ServerLevel level) {
        return level.getGameRules().get(EEGamerules.ECHO_RESONATOR_COOLDOWN);
    }

    @Override
    public boolean allowComponentsUpdateAnimation(Player player, InteractionHand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        updateLinkComponent(stack, level);
        updateReceiverLive(stack, level, entity);
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

    // Marks the stack while the linked receiver is live and in reach
    private void updateReceiverLive(ItemStack stack, ServerLevel level, Entity entity) {
        boolean live = isReceiverLive(stack, level, entity);
        if (live == stack.has(EEDataComponentTypes.ECHO_RECEIVER_LIVE)) {
            return;
        }

        if (live) {
            stack.set(EEDataComponentTypes.ECHO_RECEIVER_LIVE, Unit.INSTANCE);
        } else {
            stack.remove(EEDataComponentTypes.ECHO_RECEIVER_LIVE);
        }
    }

    private boolean isReceiverLive(ItemStack stack, ServerLevel level, Entity entity) {
        EchoReceiverLinkComponent linkComponent = stack.get(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        if (linkComponent == null) {
            return false;
        }

        Optional<GlobalPos> target = linkComponent.resolve(level);
        if (target.isEmpty() || !target.get().dimension().equals(level.dimension())) {
            return false;
        }

        BlockPos pos = target.get().pos();
        if (!EchoReceiverBlockEntity.isWithinRange(level, entity.position(), pos)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        return state.is(EEBlocks.ECHO_RECEIVER) && EchoReceiverBlock.isActivated(state);
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

    // Powers the receiver on the click itself
    private void beginUse(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        EchoReceiverBlockEntity receiver = resolveReceiver(level, player, stack);
        if (receiver == null || !receiver.beginSustain(player)) {
            failUse(player, stack, hand);
            return;
        }

        holds.put(player, stack);
        sendGlow(player, hand, EchoResonatorGlow.ACTIVE, GLOW_HOLD_TICKS);
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

        // Anything but the stack that started the hold ends it
        if (holds.get(player) != stack) {
            holds.remove(player);
            dropHold(serverLevel, player, stack);
            return;
        }

        EchoReceiverBlockEntity receiver = resolveReceiver(serverLevel, player, stack);
        if (receiver == null) {
            failUse(player, stack, hand);
            return;
        }

        int heldTicks = MAX_HOLD_TICKS - remainingUseTicks;
        receiver.continueSustain(player, heldTicks);

        if (heldTicks > 0 && heldTicks % GLOW_REFRESH_INTERVAL == 0) {
            sendGlow(player, hand, EchoResonatorGlow.ACTIVE, GLOW_HOLD_TICKS);
        }
    }

    // A press that reached nothing still costs the cooldown, and still shows
    private void failUse(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        player.getCooldowns().addCooldown(stack, cooldownTicks(player.level()));
        holds.remove(player);
        player.stopUsingItem();
        sendGlow(player, hand, EchoResonatorGlow.ATTEMPTING, PRESS_FLASH_TICKS);
    }

    // Ends a hold that stopped without being released: scrolled away, swapped hands, or the
    // stack changed. The caller owns the map entry.
    private void dropHold(ServerLevel level, ServerPlayer player, ItemStack stack) {
        EchoReceiverBlockEntity receiver = resolveReceiver(level, player, stack);
        if (receiver != null) {
            receiver.endSustain(player);
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

        Iterator<Map.Entry<Player, ItemStack>> iterator = holds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, ItemStack> entry = iterator.next();

            if (!(entry.getKey() instanceof ServerPlayer player)
                    || player.hasDisconnected()
                    || !(player.level() instanceof ServerLevel level)) {
                // Nobody to cool down, and nobody to tell
                iterator.remove();
                continue;
            }

            ItemStack stack = entry.getValue();
            if (player.isUsingItem() && player.getUseItem() == stack) {
                continue;
            }

            iterator.remove();
            dropHold(level, player, stack);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return true;
        }

        holds.remove(player);

        // A failed attempt already took the cooldown and stopped the hold on its way here
        if (player.getCooldowns().isOnCooldown(stack)) {
            return true;
        }

        EchoReceiverBlockEntity receiver = resolveReceiver(serverLevel, player, stack);
        if (receiver != null) {
            receiver.endSustain(player);
            player.getCooldowns().addCooldown(stack, cooldownTicks(serverLevel));
        }

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

    // The receiver this resonator points at, or null if the link is unusable right now
    private @Nullable EchoReceiverBlockEntity resolveReceiver(ServerLevel level, Player player, ItemStack stack) {
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

        BlockPos targetPos = targetGlobal.pos();
        if (!EchoReceiverBlockEntity.isWithinRange(level, player.position(), targetPos)) {
            return null;
        }

        return EchoReceiverBlockEntity.get(level, targetPos);
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
