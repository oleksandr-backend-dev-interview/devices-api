package dev.poslavskyi.devices.device.api;

import dev.poslavskyi.devices.device.domain.DeviceState;

public record PatchDeviceRequest(String name, String brand, DeviceState state) {
}
