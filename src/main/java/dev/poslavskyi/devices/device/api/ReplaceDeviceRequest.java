package dev.poslavskyi.devices.device.api;

import dev.poslavskyi.devices.device.domain.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReplaceDeviceRequest(
        @NotBlank String name,
        @NotBlank String brand,
        @NotNull DeviceState state) {
}
