package monster.giz.network;

import io.netty.buffer.ByteBuf;
import monster.giz.EchoExpansions;
import monster.giz.world.item.EchoResonatorGlow;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


// Tells nearby clients how a resonator in a holder's hand looks. Scoped to holder and hand.
public record EchoResonatorGlowPayload(int holderId, boolean offHand, EchoResonatorGlow state, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<EchoResonatorGlowPayload> TYPE = new Type<>(EchoExpansions.idOf("echo_resonator_glow"));

    public static final StreamCodec<ByteBuf, EchoResonatorGlowPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EchoResonatorGlowPayload::holderId,
            ByteBufCodecs.BOOL, EchoResonatorGlowPayload::offHand,
            EchoResonatorGlow.STREAM_CODEC, EchoResonatorGlowPayload::state,
            ByteBufCodecs.VAR_INT, EchoResonatorGlowPayload::durationTicks,
            EchoResonatorGlowPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
