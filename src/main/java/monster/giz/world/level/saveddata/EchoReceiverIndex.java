package monster.giz.world.level.saveddata;

import com.mojang.serialization.Codec;
import monster.giz.EchoExpansions;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Where each receiver actually is. Links carry an id and resolve it through here.
public class EchoReceiverIndex extends SavedData {

    private static final Codec<EchoReceiverIndex> CODEC = Codec
            .unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC)
            .xmap(EchoReceiverIndex::new, index -> index.receivers);

    // Server global rather than per level; a link may point into another dimension
    private static final SavedDataType<EchoReceiverIndex> TYPE = new SavedDataType<>(
            EchoExpansions.idOf("echo_receivers"),
            EchoReceiverIndex::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_INDEX
    );

    private final Map<UUID, GlobalPos> receivers;

    private EchoReceiverIndex() {
        this(Map.of());
    }

    private EchoReceiverIndex(Map<UUID, GlobalPos> receivers) {
        this.receivers = new HashMap<>(receivers);
    }

    public static EchoReceiverIndex of(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public @Nullable GlobalPos lookup(UUID id) {
        return receivers.get(id);
    }

    public void claim(UUID id, GlobalPos pos) {
        if (!pos.equals(receivers.put(id, pos))) {
            setDirty();
        }
    }

    public void release(UUID id) {
        if (receivers.remove(id) != null) {
            setDirty();
        }
    }
}
