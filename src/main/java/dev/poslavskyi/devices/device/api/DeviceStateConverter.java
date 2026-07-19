package dev.poslavskyi.devices.device.api;

import dev.poslavskyi.devices.device.domain.DeviceState;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DeviceStateConverter implements Converter<String, DeviceState> {
    @Override
    public DeviceState convert(String source) {
        return DeviceState.fromWire(source);
    }
}