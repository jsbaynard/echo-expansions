package com.jozsmo.world.level.block;

import com.jozsmo.init.EEBlocks;
import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.world.item.component.EchoReceiverLinkComponent;
import com.jozsmo.world.level.block.entity.EchoReceiverBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;


public class EchoReceiverBlock extends BaseEntityBlock {

    public static final BooleanProperty ACTIVATED = BooleanProperty.create("activated");

    public EchoReceiverBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVATED, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EchoReceiverBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> EchoReceiverBlockEntity.serverTick((ServerLevel) lvl, pos, st, (EchoReceiverBlockEntity) be);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return outputOf(level, pos);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return outputOf(level, pos);
    }

    private static int outputOf(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EchoReceiverBlockEntity receiver ? receiver.getPower() : 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston && state.getValue(ACTIVATED)) {
            updateNeighbours(level, pos, this);
        }
    }

    // Direct power carries past the neighbours, so whatever they power needs the update too
    public static void updateNeighbours(Level level, BlockPos pos, Block block) {
        level.updateNeighborsAt(pos, block);
        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), block);
        }
    }

    public static void announce(Level level, BlockPos pos, boolean activated) {
        level.gameEvent(null, activated ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(EEBlocks.ECHO_TRANSMITTER.asItem())) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        EchoReceiverBlockEntity receiver = EchoReceiverBlockEntity.get(serverLevel, pos);
        if (receiver == null) {
            return InteractionResult.PASS;
        }

        stack.set(EEDataComponentTypes.ECHO_RECEIVER_LINK, EchoReceiverLinkComponent.of(receiver.identity(serverLevel)));
        return InteractionResult.SUCCESS;
    }


    // Claims an identity the moment it is put down, so a freshly crafted receiver never
    // inherits the links of whichever one used to stand here
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            EchoReceiverBlockEntity receiver = EchoReceiverBlockEntity.get(serverLevel, pos);
            if (receiver != null) {
                receiver.identity(serverLevel);
            }
        }
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(ACTIVATED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVATED);
    }

    public static boolean isActivated(BlockState state) {
        return state.getValue(ACTIVATED);
    }
}
