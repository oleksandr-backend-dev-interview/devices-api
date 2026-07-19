package dev.poslavskyi.devices.device.api;

import dev.poslavskyi.devices.device.domain.DeviceState;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

@JacksonComponent
public class DeviceStateJacksonComponent {

    public static class Serializer
            extends ValueSerializer<DeviceState> {

        @Override
        public void serialize(
                DeviceState value,
                JsonGenerator generator,
                SerializationContext context
        ) {
            generator.writeString(value.toWire());
        }
    }

    public static class Deserializer
            extends ValueDeserializer<DeviceState> {

        @Override
        public DeviceState deserialize(
                JsonParser parser,
                DeserializationContext context
        ) {
            return DeviceState.fromWire(parser.getString());
        }
    }
}