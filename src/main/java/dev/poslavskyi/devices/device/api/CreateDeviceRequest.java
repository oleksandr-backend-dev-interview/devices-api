package dev.poslavskyi.devices.device.api;

import dev.poslavskyi.devices.device.domain.DeviceState;
import jakarta.validation.constraints.NotBlank;

public record CreateDeviceRequest(
        @NotBlank String name,
        @NotBlank String brand,
        DeviceState state) {
}
