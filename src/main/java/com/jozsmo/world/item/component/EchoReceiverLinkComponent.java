package com.jozsmo.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import com.jozsmo.init.EEBlocks;
import com.jozsmo.init.EEGamerules;
import com.jozsmo.world.level.block.entity.EchoReceiverBlockEntity;
import com.jozsmo.world.level.saveddata.EchoReceiverIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

// Holds who the receiver is, not where it is. The position lives in EchoReceiverIndex.
public record EchoReceiverLinkComponent(Optional<UUID> receiver) {

    public static final Codec<EchoReceiverLinkComponent> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    UUIDUtil.CODEC.optionalFieldOf("receiver").forGetter(EchoReceiverLinkComponent::receiver)
            ).apply(instance, EchoReceiverLinkComponent::new)
    );

    public static final StreamCodec<ByteBuf, EchoReceiverLinkComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), EchoReceiverLinkComponent::receiver,
            EchoReceiverLinkComponent::new
    );

    public static final Component ENTANGLED_TOOLTIP = Component
            .translatable("tooltip.echo-expansions.entangled")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

    public static EchoReceiverLinkComponent of(UUID receiver) {
        return new EchoReceiverLinkComponent(Optional.of(receiver));
    }

    public Optional<GlobalPos> resolve(ServerLevel level) {
        return receiver.map(id -> EchoReceiverIndex.of(level).lookup(id));
    }

    public @Nullable BlockState reachableFrom(ServerLevel level, Vec3 from) {
        return reachable(level, from, receiver.orElse(null));
    }

    // The receiver an id points at, if it is standing within range of `from`. One place for
    // every caller that needs to know whether a signal would land: items, blocks and models.
    public static @Nullable BlockState reachable(ServerLevel level, Vec3 from, @Nullable UUID receiver) {
        if (receiver == null) {
            return null;
        }

        GlobalPos target = EchoReceiverIndex.of(level).lookup(receiver);
        if (target == null || !target.dimension().equals(level.dimension())) {
            return null;
        }

        BlockPos pos = target.pos();
        if (!EchoReceiverBlockEntity.isWithinRange(level, from, pos)) {
            return null;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.is(EEBlocks.ECHO_RECEIVER) || !EchoReceiverBlockEntity.answersTo(level, pos, receiver)) {
            return null;
        }

        return state;
    }

    // The receiver this link names, but only if it is the one actually standing there
    public @Nullable EchoReceiverBlockEntity entangledAt(ServerLevel level, BlockPos pos) {
        UUID id = this.receiver.orElse(null);
        if (id == null || !EchoReceiverBlockEntity.answersTo(level, pos, id)) {
            return null;
        }

        return EchoReceiverBlockEntity.get(level, pos);
    }

    // Whether a link outlives a receiver that is not there. While a receiver can be picked up
    // and put back down as itself, an absent one is on its way somewhere, not gone.
    public static boolean survivesAbsence(ServerLevel level) {
        return level.getGameRules().get(EEGamerules.ECHO_RECEIVER_RELOCATION);
    }

    // Drops the link once the index has forgotten the receiver, or once the block it resolves to
    // is loaded and is no longer one. An unloaded receiver is left alone.
    public EchoReceiverLinkComponent forLevel(ServerLevel level) {
        if (this.receiver.isEmpty()) {
            return this;
        }

        Optional<GlobalPos> resolved = resolve(level);
        if (resolved.isEmpty()) {
            return new EchoReceiverLinkComponent(Optional.empty());
        }

        if (survivesAbsence(level)) {
            return this;
        }

        GlobalPos target = resolved.get();
        if (!target.dimension().equals(level.dimension())) {
            return this;
        }

        BlockPos blockPos = target.pos();
        if (level.isLoaded(blockPos) && (!level.isInWorldBounds(blockPos) || !level.getBlockState(blockPos).is(EEBlocks.ECHO_RECEIVER))) {
            return new EchoReceiverLinkComponent(Optional.empty());
        }

        return this;
    }
}
