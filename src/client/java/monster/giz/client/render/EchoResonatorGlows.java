package monster.giz.client.render;

import monster.giz.network.EchoResonatorGlowPayload;
import monster.giz.world.item.EchoResonatorGlow;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;


public final class EchoResonatorGlows {

    private static final int SWEEP_THRESHOLD = 32;

    private static final Map<Integer, Glow> ACTIVE = new HashMap<>();

    private EchoResonatorGlows() {
    }

    public record Glow(
            InteractionHand hand,
            EchoResonatorGlow state,
            long startTick,
            int durationTicks
    ) {
    }

    public static void add(EchoResonatorGlowPayload payload, ClientLevel level) {
        if (payload.durationTicks() <= 0) {
            ACTIVE.remove(payload.holderId());
            return;
        }

        long gameTime = level.getGameTime();
        if (ACTIVE.size() > SWEEP_THRESHOLD) {
            ACTIVE.entrySet().removeIf(entry -> expired(entry.getValue(), gameTime));
        }

        InteractionHand hand = payload.offHand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        ACTIVE.put(
                payload.holderId(),
                new Glow(hand, payload.state(), gameTime, payload.durationTicks())
        );
    }

    public static @Nullable Glow get(int holderId, long gameTime) {
        Glow glow = ACTIVE.get(holderId);
        if (glow == null) {
            return null;
        }

        if (expired(glow, gameTime)) {
            ACTIVE.remove(holderId);
            return null;
        }

        return glow;
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static boolean expired(Glow glow, long gameTime) {
        long age = gameTime - glow.startTick();
        return age < 0 || age >= glow.durationTicks();
    }
}
