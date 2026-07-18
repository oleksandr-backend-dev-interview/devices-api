package dev.poslavskyi.devices.device.domain;

public class DeviceOperationNotAllowedException extends RuntimeException {
    public DeviceOperationNotAllowedException(String message) {
        super(message);
    }
}
