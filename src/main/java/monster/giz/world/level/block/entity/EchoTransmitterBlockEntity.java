package monster.giz.world.level.block.entity;

import monster.giz.init.EEBlockEntities;
import monster.giz.init.EEBlocks;
import monster.giz.init.EEDataComponentTypes;
import monster.giz.world.item.component.EchoReceiverLinkComponent;
import monster.giz.world.level.block.EchoTransmitterBlock;
import monster.giz.world.level.saveddata.EchoReceiverIndex;
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
        if (!state.getValue(EchoTransmitterBlock.POWERED) || be.target == null) {
            return;
        }

        GlobalPos resolved = EchoReceiverIndex.of(level).lookup(be.target);
        if (resolved == null) {
            be.setTarget(null);
            return;
        }

        if (!resolved.dimension().equals(level.dimension())) {
            return;
        }

        BlockPos targetPos = resolved.pos();
        if (!EchoReceiverBlockEntity.isWithinRange(level, Vec3.atCenterOf(pos), targetPos)) {
            return;
        }

        if (!level.getBlockState(targetPos).is(EEBlocks.ECHO_RECEIVER)) {
            be.setTarget(null);
            return;
        }

        EchoReceiverBlockEntity receiver = EchoReceiverBlockEntity.get(level, targetPos);
        if (receiver != null) {
            receiver.pokeSustained(EchoTransmitterBlock.inputSignal(level, pos, state));
        }
    }

    public void setTarget(@Nullable UUID target) {
        this.target = target;
        setChanged();
        if (this.getLevel() instanceof ServerLevel serverLevel) {
            BlockState state = this.getBlockState();
            boolean linked = target != null;
            if (state.getValue(EchoTransmitterBlock.LINKED) != linked) {
                serverLevel.setBlock(this.getBlockPos(), state.setValue(EchoTransmitterBlock.LINKED, linked), Block.UPDATE_ALL);
            }
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
