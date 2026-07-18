package dev.poslavskyi.devices.device.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceTest {
    private static final Instant FIXED_TIME = Instant.parse("2026-07-18T10:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

    @Test
    void newDeviceHasGivenNameBrandAndAvailableState() {
        Device device = getDevice();

        assertEquals("iPhone 15", device.getName());
        assertEquals("Apple", device.getBrand());
        assertEquals(DeviceState.AVAILABLE, device.getState());
    }

    private static Device getDevice() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        return device;
    }

    @Test
    void newDeviceGetCreationTimeFromClock() {
        Device device = getDevice();

        assertEquals(FIXED_TIME, device.getCreationTime());
    }

    @Test
    void newDeviceGetsGeneratedId() {
        Device device = getDevice();

        assertNotNull(device.getId());
    }

    @Test
    void rejectBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Device("   ", "Apple", FIXED_CLOCK));
    }

    @Test
    void rejectBlankBrand() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Device("iPhone 15", "  ", FIXED_CLOCK));
    }

    @Test
    void trimNameBrandWithSpaces() {
        Device device = new Device(" iPhone 15  ", " Apple", FIXED_CLOCK);
        assertEquals("iPhone 15", device.getName());
        assertEquals("Apple", device.getBrand());
    }

    @Test
    void stateCanBeChanged() {
        Device device = getDevice();

        device.changeState(DeviceState.IN_USE);

        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    void rejectNullState() {
        Device device = getDevice();

        assertThatNullPointerException()
                .isThrownBy(() -> device.changeState(null));
    }

    @Test
    void renameWorksWhenDeviceIsAvailable() {
        Device device = getDevice();

        device.rename("iPhone 15 Pro");

        assertEquals("iPhone 15 Pro", device.getName());
    }

    @Test
    void renameIsRejectedWhenDeviceIsInUse() {
        Device device = getDevice();
        device.changeState(DeviceState.IN_USE);

        assertThatExceptionOfType(DeviceOperationNotAllowedException.class)
                .isThrownBy(() -> device.rename("iPhone 15 Pro"))
                .withMessageContaining("in use");
    }

    @Test
    void renameIsRejectedWhenNewNameIsNull() {
        Device device = getDevice();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> device.rename(null));
    }

    @Test
    void renameIsRejectedWhenNewNameIsBlank() {
        Device device = getDevice();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> device.rename("   "));
    }

    @Test
    void changeBrandWorksWhenDeviceIsAvailable() {
        Device device = getDevice();

        device.changeBrand("Samsung");

        assertThat(device.getBrand()).isEqualTo("Samsung");
    }

    @Test
    void changeBrandIsRejectedWhenDeviceIsInUse() {
        Device device = getDevice();
        device.changeState(DeviceState.IN_USE);

        assertThatExceptionOfType(DeviceOperationNotAllowedException.class)
                .isThrownBy(() -> device.changeBrand("Samsung"))
                .withMessageContaining("in use");
    }

    @Test
    void changeBrandIsRejectedWhenNewNameIsNull() {
        Device device = getDevice();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> device.changeBrand(null));
    }

    @Test
    void changeBrandIsRejectedWhenNewNameIsBlank() {
        Device device = getDevice();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> device.changeBrand("   "));
    }

    @Test
    void deleteDeviceWorksWhenDeviceIsAvailable() {
        Device device = getDevice();
        assertDoesNotThrow(device::ensureCanBeDeleted);
    }

    @Test
    void deleteDeviceIsRejectedWhenDeviceIsInUse() {
        Device device = getDevice();
        device.changeState(DeviceState.IN_USE);
        assertThatExceptionOfType(DeviceOperationNotAllowedException.class)
                .isThrownBy(device::ensureCanBeDeleted)
                .withMessageContaining("in use");
    }
}