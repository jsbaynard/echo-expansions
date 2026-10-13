package monster.giz.client.render;

import monster.giz.network.EchoLinkBeamPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class EchoLinkBeams {

    private static final float FADE_TICKS = 4.0F;

    private static final int SWEEP_THRESHOLD = 64;

    private static final Map<BlockPos, Beam> ACTIVE = new HashMap<>();

    private EchoLinkBeams() {
    }

    public record Beam(int sourceId, Vec3 origin, long startTick, int durationTicks) {
    }

    public static void add(EchoLinkBeamPayload payload, long gameTime) {
        if (ACTIVE.size() > SWEEP_THRESHOLD) {
            ACTIVE.entrySet().removeIf(entry -> expired(entry.getValue(), gameTime));
        }

        ACTIVE.put(
                payload.receiver().immutable(),
                new Beam(payload.sourceId(), payload.origin(), gameTime, payload.durationTicks())
        );
    }

    public static @Nullable Beam get(BlockPos receiver, long gameTime) {
        Beam beam = ACTIVE.get(receiver);
        if (beam == null) {
            return null;
        }

        if (expired(beam, gameTime)) {
            ACTIVE.remove(receiver);
            return null;
        }

        return beam;
    }

    public static Vec3 originOf(Beam beam, Level level, float partialTick) {
        if (beam.sourceId() == EchoLinkBeamPayload.NO_SOURCE) {
            return beam.origin();
        }

        Entity source = level.getEntity(beam.sourceId());
        if (source == null) {
            return beam.origin();
        }

        return EchoLinkOrigins.handPosition(source, partialTick);
    }

    public static float beamOpacity(Beam beam, long gameTime, float partialTick) {
        float remaining = beam.durationTicks() - (gameTime - beam.startTick() + partialTick);
        if (remaining >= FADE_TICKS) {
            return 1.0F;
        }

        return Math.max(0.0F, remaining / FADE_TICKS);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static boolean expired(Beam beam, long gameTime) {
        long age = gameTime - beam.startTick();
        return age < 0 || age >= beam.durationTicks();
    }
}
