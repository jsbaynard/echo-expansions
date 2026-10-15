package com.jozsmo.init;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;
import com.jozsmo.EchoExpansions;
import net.minecraft.core.Registry;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.*;

import java.util.function.ToIntFunction;

import static net.minecraft.core.registries.BuiltInRegistries.GAME_RULE;

public class EEGamerules {

    public static final GameRule<Integer> ECHO_ENTANGLEMENT_RANGE =
            registerInteger("echo_entanglement_range", GameRuleCategory.MISC, 64, 1, 256);

    public static final GameRule<Integer> ECHO_RESONATOR_COOLDOWN =
            registerInteger("echo_resonator_cooldown", GameRuleCategory.MISC, 30, 1, 1200);

    // Off by default, so breaking a receiver ends every link to it. Turning this on puts the
    // receiver's identity on the dropped block, so it can be picked up, carried and put back
    // down as the same receiver, and the links follow it. A copy never takes over the links.
    public static final GameRule<Boolean> ECHO_RECEIVER_RELOCATION =
            registerBoolean("echo_receiver_relocation", GameRuleCategory.MISC, false);

    private static GameRule<Boolean> registerBoolean(final String id, final GameRuleCategory category, final boolean defaultValue) {
        return register(id, category, GameRuleType.BOOL, BoolArgumentType.bool(), Codec.BOOL, defaultValue, FeatureFlagSet.of(), GameRuleTypeVisitor::visitBoolean, (b) -> b ? 1 : 0);
    }

    private static GameRule<Integer> registerInteger(final String id, final GameRuleCategory category, final int defaultValue, final int min, final int max) {
        return register(id, category, GameRuleType.INT, IntegerArgumentType.integer(min, max), Codec.intRange(min, max), defaultValue, FeatureFlagSet.of(), GameRuleTypeVisitor::visitInteger, (i) -> i);
    }

    private static <T> GameRule<T> register(
            final String id,
            final GameRuleCategory category,
            final GameRuleType typeHint,
            final ArgumentType<T> argumentType,
            final Codec<T> codec,
            final T defaultValue,
            final FeatureFlagSet requiredFeatures,
            final GameRules.VisitorCaller<T> visitorCaller,
            final ToIntFunction<T> commandResultFunction
    ) {
        return Registry.register(
                GAME_RULE,
                EchoExpansions.idOf(id),
                new GameRule<>(category, typeHint, argumentType, visitorCaller, codec, commandResultFunction, defaultValue, requiredFeatures)
        );
    }

    public static void registerAll() {}
}
