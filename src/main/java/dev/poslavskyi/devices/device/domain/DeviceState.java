package dev.poslavskyi.devices.device.domain;

import java.util.Arrays;

public enum DeviceState {
    AVAILABLE,
    IN_USE,
    INACTIVE;

    public String toWire() {
        return name().toLowerCase().replace('_', '-');
    }

    public static DeviceState fromWire(String value) {
        for (DeviceState s : values()) {
            if (s.toWire().equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException(String.format(
                "Unknown state '%s'. Allowed: %s", value, Arrays.stream(DeviceState.values()).map(DeviceState::toWire).toList()));
    }
}
