package monster.giz.world.item;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

// What a resonator looks like right now
public enum EchoResonatorGlow implements StringRepresentable {

    // The press reached a receiver
    ACTIVE("active"),

    // Pressed but nothing answered: out of range, unlinked, or still cooling
    ATTEMPTING("attempting");

    public static final StringRepresentable.EnumCodec<EchoResonatorGlow> CODEC =
            StringRepresentable.fromEnum(EchoResonatorGlow::values);

    public static final StreamCodec<ByteBuf, EchoResonatorGlow> STREAM_CODEC =
            ByteBufCodecs.idMapper(EchoResonatorGlow::byId, EchoResonatorGlow::ordinal);

    private static final EchoResonatorGlow[] BY_ID = values();

    private final String serializedName;

    EchoResonatorGlow(String serializedName) {
        this.serializedName = serializedName;
    }

    private static EchoResonatorGlow byId(int id) {
        return id >= 0 && id < BY_ID.length ? BY_ID[id] : ACTIVE;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
