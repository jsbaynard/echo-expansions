package com.jozsmo.world.level.block.entity;

import com.jozsmo.init.EEBlockEntities;
import com.jozsmo.init.EEDataComponentTypes;
import com.jozsmo.init.EEGamerules;
import com.jozsmo.init.EESounds;
import com.jozsmo.network.EchoLinkBeamPayload;
import com.jozsmo.world.level.block.EchoReceiverBlock;
import com.jozsmo.world.level.saveddata.EchoReceiverIndex;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EchoReceiverBlockEntity extends BlockEntity {

    // How long the receiver glows, and the beam shows, on a fresh link
    public static final int LINK_FLASH_TICKS = 10;
    private static final int SUSTAIN_DECAY_TICKS = 4;

    // A resonator always drives the receiver flat out; a transmitter passes its own level on
    public static final int FULL_POWER = 15;

    // Players within this distance of either end are told to draw the beam
    private static final double BEAM_VIEW_RADIUS = 96.0;

    // How long a sent beam stays valid on the client
    private static final int BEAM_HOLD_TICKS = 20;
    // How often the beam is re-sent
    private static final int BEAM_REFRESH_INTERVAL = 10;
    // Beam life left after release, i.e. the fade out
    private static final int BEAM_RELEASE_TICKS = 5;

    private static final String PULSE_DURATION_KEY = "pulse_duration";
    private static final String COOLDOWN_KEY = "cooldown";
    private static final String SUSTAIN_TICKS_KEY = "sustain_ticks";
    private static final String POWER_KEY = "power";
    private static final String RECEIVER_ID_KEY = "receiver_id";

    @Nullable
    private UUID receiverId;
    // Per load, not persisted: the index is reconciled once after coming back
    private boolean identityChecked;

    private int cooldown;
    private int pulseDuration;
    private int sustainTicks;
    // What the block emits now, and the strongest poke since the last tick
    private int power;
    private int pendingPower;

    public EchoReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(EEBlockEntities.ECHO_RECEIVER_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, EchoReceiverBlockEntity be) {
        be.identity(level);
        if (be.cooldown > 0) be.cooldown--;
        if (be.pulseDuration > 0) be.pulseDuration--;
        if (be.sustainTicks > 0) be.sustainTicks--;
        be.refreshActivation(level, pos, state);
    }

    // Acknowledges a fresh link: a short glow and beam
    public void flashLink(@Nullable ServerPlayer user, Vec3 origin) {
        if (!(this.getLevel() instanceof ServerLevel level)) {
            return;
        }

        cooldown = cooldownTicks(level);
        pulseDuration = LINK_FLASH_TICKS;
        refreshActivation(level, this.getBlockPos(), this.getBlockState());
        setChanged();

        playResonance(level, origin);
        broadcastBeam(
                level,
                user == null ? origin : EchoLinkBeamPayload.chestOf(user),
                user == null ? EchoLinkBeamPayload.NO_SOURCE : user.getId(),
                LINK_FLASH_TICKS
        );
    }

    // Powers the receiver for as long as the resonator is held; the cooldown waits for endSustain
    public boolean beginSustain(ServerPlayer user) {
        if (cooldown > 0 || !(this.getLevel() instanceof ServerLevel level)) {
            return false;
        }

        pokeSustained(FULL_POWER);
        playResonance(level, user.position());
        broadcastBeam(level, EchoLinkBeamPayload.chestOf(user), user.getId(), BEAM_HOLD_TICKS);
        return true;
    }

    public void continueSustain(ServerPlayer user, int heldTicks) {
        if (!(this.getLevel() instanceof ServerLevel level)) {
            return;
        }

        pokeSustained(FULL_POWER);
        if (heldTicks > 0 && heldTicks % BEAM_REFRESH_INTERVAL == 0) {
            broadcastBeam(level, EchoLinkBeamPayload.chestOf(user), user.getId(), BEAM_HOLD_TICKS);
        }
    }

    // Stops driving without claiming the cooldown, for a hold that is still on but out of
    // reach. The receiver decays on its own, so walking back in picks it straight back up.
    public void pauseSustain(ServerPlayer user) {
        if (!(this.getLevel() instanceof ServerLevel level)) {
            return;
        }

        broadcastBeam(level, EchoLinkBeamPayload.chestOf(user), user.getId(), BEAM_RELEASE_TICKS);
    }

    // Ends the hold; the receiver decays on its own, and this is where the cooldown lands
    public void endSustain(ServerPlayer user) {
        if (!(this.getLevel() instanceof ServerLevel level)) {
            return;
        }

        cooldown = cooldownTicks(level);
        setChanged();
        broadcastBeam(level, EchoLinkBeamPayload.chestOf(user), user.getId(), BEAM_RELEASE_TICKS);
    }

    private void playResonance(ServerLevel level, Vec3 userPos) {
        level.playSound(
                null,
                userPos.x,
                userPos.y,
                userPos.z,
                EESounds.ECHO_RESONANCE,
                SoundSource.PLAYERS,
                1.0F,
                1.5F
        );
        level.playSound(
                null,
                this.getBlockPos().getX() + 0.5,
                this.getBlockPos().getY() + 0.5,
                this.getBlockPos().getZ() + 0.5,
                EESounds.ECHO_RESONANCE,
                SoundSource.BLOCKS,
                1.0F,
                0.7F
        );
    }

    // Sends the beam to everyone who could see either end
    private void broadcastBeam(ServerLevel level, Vec3 origin, int sourceId, int durationTicks) {
        EchoLinkBeamPayload payload = new EchoLinkBeamPayload(this.getBlockPos(), sourceId, origin, durationTicks);

        Set<ServerPlayer> viewers = new HashSet<>(PlayerLookup.around(level, origin, BEAM_VIEW_RADIUS));
        viewers.addAll(PlayerLookup.around(level, Vec3.atCenterOf(this.getBlockPos()), BEAM_VIEW_RADIUS));

        for (ServerPlayer viewer : viewers) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    public int getPower() {
        return power;
    }

    public void pokeSustained(int strength) {
        if (!(this.getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean wasActive = sustainTicks > 0;
        this.sustainTicks = SUSTAIN_DECAY_TICKS;
        this.pendingPower = Math.max(this.pendingPower, strength);
        if (!wasActive) {
            refreshActivation(level, this.getBlockPos(), this.getBlockState());
            setChanged();
        }
    }

    // Shares the resonator's cooldown gamerule
    private static int cooldownTicks(ServerLevel level) {
        return level.getGameRules().get(EEGamerules.ECHO_RESONATOR_COOLDOWN);
    }

    // The handle links carry instead of coordinates. Claimed when this receiver is first
    // seen at a position, then reconciled once per load.
    public UUID identity(ServerLevel level) {
        if (receiverId != null && identityChecked) {
            return receiverId;
        }

        GlobalPos here = GlobalPos.of(level.dimension(), this.getBlockPos());
        EchoReceiverIndex index = EchoReceiverIndex.of(level);

        if (receiverId == null) {
            assign(UUID.randomUUID(), index, here);
        } else {
            GlobalPos known = index.lookup(receiverId);
            if (known == null || known.equals(here)) {
                index.claim(receiverId, here);
            } else if (holdsId(level, known, receiverId)) {
                // The receiver this id belongs to is still out there, so this one is a copy
                assign(UUID.randomUUID(), index, here);
            } else {
                // Nothing left where the id was last seen; this is that receiver, moved
                index.claim(receiverId, here);
            }
        }

        identityChecked = true;
        return receiverId;
    }

    // Whether the receiver that owns this id is still where the index last saw it. An unloaded
    // one counts, so a copy made far from the original does not quietly steal its links.
    private static boolean holdsId(ServerLevel level, GlobalPos known, UUID id) {
        ServerLevel target = level.getServer().getLevel(known.dimension());
        if (target == null) {
            return false;
        }
        if (!target.isLoaded(known.pos())) {
            return true;
        }
        return answersTo(target, known.pos(), id);
    }

    // Whether the receiver standing here is the one that answers to this id. A different
    // receiver put down on the same spot is a different receiver, address notwithstanding.
    public static boolean answersTo(ServerLevel level, BlockPos pos, UUID id) {
        return level.getBlockEntity(pos) instanceof EchoReceiverBlockEntity receiver
                && id.equals(receiver.receiverId);
    }

    private void assign(UUID id, EchoReceiverIndex index, GlobalPos here) {
        receiverId = id;
        index.claim(id, here);
        setChanged();
    }

    // Real removal only; chunk unload goes through setRemoved and leaves the index alone.
    // While links may follow the block the entry is kept, so a pocketed receiver stays linked.
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (receiverId != null && this.getLevel() instanceof ServerLevel level
                && !level.getGameRules().get(EEGamerules.ECHO_RECEIVER_RELOCATION)) {
            EchoReceiverIndex.of(level).release(receiverId);
        }
    }

    // The id rides the dropped block only while the rule lets links follow it
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (receiverId != null && this.getLevel() instanceof ServerLevel level
                && level.getGameRules().get(EEGamerules.ECHO_RECEIVER_RELOCATION)) {
            builder.set(EEDataComponentTypes.ECHO_RECEIVER_IDENTITY, receiverId);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        UUID carried = input.get(EEDataComponentTypes.ECHO_RECEIVER_IDENTITY);
        if (carried != null) {
            receiverId = carried;
            identityChecked = false;
        }
    }

    private void refreshActivation(ServerLevel level, BlockPos pos, BlockState state) {
        boolean shouldBeActive = sustainTicks > 0 || pulseDuration > 0;
        int before = power;

        if (pulseDuration > 0) {
            power = FULL_POWER;
        } else if (sustainTicks > 0) {
            // Nothing poked this tick, so hold the level through the decay
            if (pendingPower > 0) {
                power = pendingPower;
            }
        } else {
            power = 0;
        }
        pendingPower = 0;

        boolean flipped = state.getValue(EchoReceiverBlock.ACTIVATED) != shouldBeActive;
        if (flipped) {
            level.setBlock(pos, state.setValue(EchoReceiverBlock.ACTIVATED, shouldBeActive), Block.UPDATE_CLIENTS);
            EchoReceiverBlock.announce(level, pos, shouldBeActive);
        }

        if (flipped || power != before) {
            EchoReceiverBlock.updateNeighbours(level, pos, state.getBlock());
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.cooldown = input.getIntOr(COOLDOWN_KEY, 0);
        this.pulseDuration = input.getIntOr(PULSE_DURATION_KEY, 0);
        this.sustainTicks = input.getIntOr(SUSTAIN_TICKS_KEY, 0);
        this.power = input.getIntOr(POWER_KEY, 0);
        this.receiverId = input.read(RECEIVER_ID_KEY, UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (cooldown > 0) output.putInt(COOLDOWN_KEY, cooldown);
        if (pulseDuration > 0) output.putInt(PULSE_DURATION_KEY, pulseDuration);
        if (sustainTicks > 0) output.putInt(SUSTAIN_TICKS_KEY, sustainTicks);
        if (power > 0) output.putInt(POWER_KEY, power);
        if (receiverId != null) output.store(RECEIVER_ID_KEY, UUIDUtil.CODEC, receiverId);
    }

    public static boolean isWithinRange(ServerLevel level, Vec3 userPos, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        return userPos.distanceTo(Vec3.atCenterOf(pos)) <= level.getGameRules().get(EEGamerules.ECHO_ENTANGLEMENT_RANGE);
    }

    @Nullable
    public static EchoReceiverBlockEntity get(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof EchoReceiverBlockEntity echoReceiver ? echoReceiver : null;
    }
}
