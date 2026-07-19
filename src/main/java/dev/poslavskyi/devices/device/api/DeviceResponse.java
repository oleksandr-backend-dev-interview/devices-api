package dev.poslavskyi.devices.device.api;

import dev.poslavskyi.devices.device.domain.Device;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(UUID id, String name, String brand, String state, Instant creationTime) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(device.getId(), device.getName(), device.getBrand(),
                device.getState().toWire(), device.getCreationTime());
    }
}
