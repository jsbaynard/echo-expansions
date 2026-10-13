package monster.giz.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import monster.giz.init.EEBlocks;
import monster.giz.world.level.saveddata.EchoReceiverIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;

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

    public static EchoReceiverLinkComponent of(UUID receiver) {
        return new EchoReceiverLinkComponent(Optional.of(receiver));
    }

    public Optional<GlobalPos> resolve(ServerLevel level) {
        return receiver.map(id -> EchoReceiverIndex.of(level).lookup(id));
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
