package com.jozsmo.world.level.block.entity;

import com.jozsmo.init.EEBlockEntities;
import com.jozsmo.init.EEBlocks;
import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.world.item.component.EchoReceiverLinkComponent;
import com.jozsmo.world.level.block.EchoLinkState;
import com.jozsmo.world.level.block.EchoReceiverBlock;
import com.jozsmo.world.level.block.EchoTransmitterBlock;
import com.jozsmo.world.level.saveddata.EchoReceiverIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class EchoTransmitterBlockEntity extends BlockEntity {

    private static final String TARGET_KEY = "receiver";
    private static final String CUSTOM_NAME_KEY = "custom_name";

    @Nullable
    private UUID target;
    @Nullable
    private Component customName;

    public EchoTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(EEBlockEntities.ECHO_TRANSMITTER_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, EchoTransmitterBlockEntity be) {
        if (!be.refreshLink(level, pos, state).canDeliver()
                || !state.getValue(EchoTransmitterBlock.POWERED)) {
            return;
        }

        GlobalPos resolved = EchoReceiverIndex.of(level).lookup(be.target);
        EchoReceiverBlockEntity receiver = resolved == null
                ? null
                : EchoReceiverBlockEntity.get(level, resolved.pos());
        if (receiver != null) {
            receiver.pokeSustained(EchoTransmitterBlock.inputSignal(level, pos, state));
        }
    }

    // Settles the blockstate on how the link is doing. Visual only, so no neighbour updates;
    // a receiver whose chunk is asleep reads as far until it wakes.
    private EchoLinkState refreshLink(ServerLevel level, BlockPos pos, BlockState state) {
        EchoLinkState link = linkState(level, pos);
        if (state.getValue(EchoTransmitterBlock.LINK) != link) {
            level.setBlock(pos, state.setValue(EchoTransmitterBlock.LINK, link), Block.UPDATE_CLIENTS);
        }
        return link;
    }

    private EchoLinkState linkState(ServerLevel level, BlockPos pos) {
        if (target == null) {
            return EchoLinkState.UNLINKED;
        }

        GlobalPos resolved = EchoReceiverIndex.of(level).lookup(target);
        if (resolved == null) {
            forgetTarget();
            return EchoLinkState.UNLINKED;
        }

        // reachable hands back the receiver's own state, so its light costs nothing to read
        BlockState receiver = EchoReceiverLinkComponent.reachable(level, Vec3.atCenterOf(pos), target);
        if (receiver != null) {
            return EchoReceiverBlock.isActivated(receiver) ? EchoLinkState.LIVE : EchoLinkState.READY;
        }

        // Out of reach is one thing; standing where the receiver used to be is another
        if (resolved.dimension().equals(level.dimension())
                && level.isLoaded(resolved.pos())
                && !level.getBlockState(resolved.pos()).is(EEBlocks.ECHO_RECEIVER)
                && !EchoReceiverLinkComponent.survivesAbsence(level)) {
            forgetTarget();
            return EchoLinkState.UNLINKED;
        }

        return EchoLinkState.FAR;
    }

    // Drops the link without touching the blockstate, for a caller already settling it
    private void forgetTarget() {
        this.target = null;
        setChanged();
    }

    public void setTarget(@Nullable UUID target) {
        this.target = target;
        setChanged();
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            refreshLink(serverLevel, this.getBlockPos(), this.getBlockState());
        }
    }

    public void validateLink(ServerLevel level) {
        if (target == null) {
            return;
        }
        GlobalPos resolved = EchoReceiverIndex.of(level).lookup(target);
        if (resolved == null) {
            setTarget(null);
            return;
        }
        if (!resolved.dimension().equals(level.dimension())) {
            return;
        }
        if (EchoReceiverLinkComponent.survivesAbsence(level)) {
            return;
        }

        BlockPos targetPos = resolved.pos();
        if (level.isLoaded(targetPos) && (!level.isInWorldBounds(targetPos) || !level.getBlockState(targetPos).is(EEBlocks.ECHO_RECEIVER))) {
            setTarget(null);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        EchoReceiverLinkComponent link = input.get(EEDataComponentTypes.ECHO_RECEIVER_LINK);
        this.setTarget(link != null ? link.receiver().orElse(null) : null);
        this.customName = input.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (this.target != null) {
            builder.set(EEDataComponentTypes.ECHO_RECEIVER_LINK, EchoReceiverLinkComponent.of(this.target));
        }
        if (this.customName != null) {
            builder.set(DataComponents.CUSTOM_NAME, this.customName);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.target = input.read(TARGET_KEY, UUIDUtil.CODEC).orElse(null);
        this.customName = input.read(CUSTOM_NAME_KEY, ComponentSerialization.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (target != null) {
            output.store(TARGET_KEY, UUIDUtil.CODEC, target);
        }
        if (customName != null) {
            output.store(CUSTOM_NAME_KEY, ComponentSerialization.CODEC, customName);
        }
    }
}
