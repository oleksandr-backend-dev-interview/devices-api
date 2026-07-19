package dev.poslavskyi.devices.device.domain;

import java.util.UUID;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(UUID id) {
        super(String.format("Device with id %s was not found", id));
    }
}
