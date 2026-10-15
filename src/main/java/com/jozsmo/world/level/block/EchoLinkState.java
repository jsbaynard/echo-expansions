package com.jozsmo.world.level.block;

import net.minecraft.util.StringRepresentable;

// How a transmitter is doing at reaching the receiver it points at
public enum EchoLinkState implements StringRepresentable {

    // Nothing linked
    UNLINKED("unlinked"),

    // Linked, but the receiver cannot be reached from here right now
    FAR("far"),

    // Linked and in range, with the receiver sitting idle
    READY("ready"),

    // Linked and in range, with the receiver lit, by this transmitter or by something else
    LIVE("live");

    private final String serializedName;

    EchoLinkState(String serializedName) {
        this.serializedName = serializedName;
    }

    // Whether a signal sent right now would land
    public boolean canDeliver() {
        return this == READY || this == LIVE;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
