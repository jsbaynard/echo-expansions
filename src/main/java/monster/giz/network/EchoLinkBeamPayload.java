package monster.giz.network;

import io.netty.buffer.ByteBuf;
import monster.giz.EchoExpansions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record EchoLinkBeamPayload(BlockPos receiver, int sourceId, Vec3 origin, int durationTicks) implements CustomPacketPayload {

    public static final int NO_SOURCE = -1;

    // Share of the holder's height the beam leaves from, roughly chest height
    public static final float CHEST_HEIGHT = 0.72F;

    public static final Type<EchoLinkBeamPayload> TYPE = new Type<>(EchoExpansions.idOf("echo_link_beam"));

    public static final StreamCodec<ByteBuf, EchoLinkBeamPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, EchoLinkBeamPayload::receiver,
            ByteBufCodecs.VAR_INT, EchoLinkBeamPayload::sourceId,
            Vec3.STREAM_CODEC, EchoLinkBeamPayload::origin,
            ByteBufCodecs.VAR_INT, EchoLinkBeamPayload::durationTicks,
            EchoLinkBeamPayload::new
    );

    public static Vec3 chestOf(Entity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * CHEST_HEIGHT, 0.0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
