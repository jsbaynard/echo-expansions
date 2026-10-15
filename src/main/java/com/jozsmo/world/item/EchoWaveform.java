package com.jozsmo.world.item;

import com.jozsmo.EchoExpansions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

import java.util.UUID;

// A receiver id drawn as a line of text: a short signal trace in the waveform font, coloured
// from the same id. Two items on the same receiver read the same at a glance, without ever
// putting the id itself on screen.
public final class EchoWaveform {

    private static final FontDescription FONT =
            new FontDescription.Resource(EchoExpansions.idOf("waveform"));

    private static final int SEGMENTS = 27;
    private static final int LEVELS = 7;
    private static final int TOP = LEVELS - 1;

    // Transition glyphs, indexed from * LEVELS + to
    private static final char FIRST_GLYPH = '\uE000';
    // Advances -1, cancelling the pixel BitmapProvider adds so the segments join up
    private static final char TIGHTEN = '\uE0FF';

    // Control points for the envelope and the centre line. Few and interpolated, so both
    // drift slowly while the carrier alternates every segment.
    private static final int CONTROLS = 5;
    private static final float AMPLITUDE_MIN = 0.8F;
    private static final float AMPLITUDE_MAX = 3.0F;
    private static final float CENTRE_MIN = 2.0F;
    private static final float CENTRE_MAX = 4.0F;
    // How far a single swing may fall short of the envelope, so peaks are uneven
    private static final float SWING_MIN = 0.45F;
    // Chance a swing holds its side for a second segment, which varies the period. Strict
    // alternation would just draw a triangle wave.
    private static final float HOLD_CHANCE = 0.3F;

    // The band every signature is picked from: echo teal through to amethyst
    private static final float HUE_FROM = 172.0F;
    private static final float HUE_TO = 276.0F;
    private static final float SATURATION_FROM = 0.62F;
    private static final float SATURATION_TO = 0.48F;
    private static final float VALUE_FROM = 0.91F;
    private static final float VALUE_TO = 0.93F;

    private EchoWaveform() {
    }

    public static Component of(UUID receiver) {
        int[] levels = levels(receiver);

        StringBuilder trace = new StringBuilder(SEGMENTS * 2);
        for (int i = 0; i < SEGMENTS; i++) {
            if (i > 0) {
                trace.append(TIGHTEN);
            }
            trace.append((char) (FIRST_GLYPH + levels[i] * LEVELS + levels[i + 1]));
        }

        return Component.literal(trace.toString())
                .withStyle(Style.EMPTY.withFont(FONT).withColor(colour(receiver)));
    }

    // A carrier alternating every segment, swung about a drifting centre by a drifting
    // envelope. Reads as a mixed signal rather than one steady oscillation.
    private static int[] levels(UUID receiver) {
        Noise noise = new Noise(mix(receiver.getMostSignificantBits()) ^ receiver.getLeastSignificantBits());

        float[] envelope = noise.controls(AMPLITUDE_MIN, AMPLITUDE_MAX);
        float[] centre = noise.controls(CENTRE_MIN, CENTRE_MAX);

        int[] levels = new int[SEGMENTS + 1];
        int side = 1;
        int held = 0;

        for (int i = 0; i <= SEGMENTS; i++) {
            float at = i / (float) SEGMENTS;
            float middle = sample(centre, at);

            // Never swing past the rails, so loud stretches stay shaped instead of clipping flat
            float headroom = Math.min(middle, TOP - middle);
            float reach = Math.min(sample(envelope, at), headroom);
            float swing = SWING_MIN + (1.0F - SWING_MIN) * noise.unit();

            if (held == 0) {
                side = -side;
                held = noise.unit() < HOLD_CHANCE ? 2 : 1;
            }
            held--;

            levels[i] = Math.clamp(Math.round(middle + side * reach * swing), 0, TOP);
        }

        return levels;
    }

    private static float sample(float[] controls, float at) {
        float scaled = at * (controls.length - 1);
        int index = Math.min((int) scaled, controls.length - 2);
        return lerp(controls[index], controls[index + 1], scaled - index);
    }

    // Hue walks one band only, so every signature still looks like it belongs to the mod
    private static int colour(UUID receiver) {
        long hash = mix(receiver.getMostSignificantBits() * 31L + receiver.getLeastSignificantBits());
        float at = (hash >>> 40) / (float) (1 << 24);

        return hsv(
                lerp(HUE_FROM, HUE_TO, at) / 360.0F,
                lerp(SATURATION_FROM, SATURATION_TO, at),
                lerp(VALUE_FROM, VALUE_TO, at)
        );
    }

    private static int hsv(float hue, float saturation, float value) {
        float position = hue * 6.0F;
        int sector = (int) position % 6;
        float offset = position - (int) position;

        float high = value;
        float low = value * (1.0F - saturation);
        float rising = value * (1.0F - saturation * (1.0F - offset));
        float falling = value * (1.0F - saturation * offset);

        return switch (sector) {
            case 0 -> rgb(high, rising, low);
            case 1 -> rgb(falling, high, low);
            case 2 -> rgb(low, high, rising);
            case 3 -> rgb(low, falling, high);
            case 4 -> rgb(rising, low, high);
            default -> rgb(high, low, falling);
        };
    }

    private static int rgb(float red, float green, float blue) {
        return (channel(red) << 16) | (channel(green) << 8) | channel(blue);
    }

    private static int channel(float value) {
        return Math.clamp((int) (value * 255.0F + 0.5F), 0, 255);
    }

    private static float lerp(float from, float to, float at) {
        return from + (to - from) * at;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    // splitmix64, so the same id always draws the same trace
    private static final class Noise {

        private long state;

        private Noise(long seed) {
            this.state = seed;
        }

        private float unit() {
            state += 0x9E3779B97F4A7C15L;
            return (mix(state) >>> 40) / (float) (1 << 24);
        }

        private float[] controls(float from, float to) {
            float[] controls = new float[CONTROLS];
            for (int i = 0; i < CONTROLS; i++) {
                controls[i] = from + (to - from) * unit();
            }
            return controls;
        }
    }
}
