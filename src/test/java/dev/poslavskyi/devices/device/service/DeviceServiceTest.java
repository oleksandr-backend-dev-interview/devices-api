package dev.poslavskyi.devices.device.service;

import dev.poslavskyi.devices.device.domain.Device;
import dev.poslavskyi.devices.device.domain.DeviceNotFoundException;
import dev.poslavskyi.devices.device.domain.DeviceOperationNotAllowedException;
import dev.poslavskyi.devices.device.domain.DeviceState;
import dev.poslavskyi.devices.device.persistence.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {
    private static final String DATE_TIME_TEST = "2026-07-18T10:00:00Z";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse(DATE_TIME_TEST), ZoneOffset.UTC);

    @Mock
    DeviceRepository repository;

    DeviceService service;

    @BeforeEach
    void setUp() {
        service = new DeviceService(repository, FIXED_CLOCK);
    }

    @Test
    void createPersistsNewAvailableDevice() {
        when(repository.save(any(Device.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

        Device created = service.create("iPhone 15", "Apple");

        assertEquals("iPhone 15", created.getName());
        assertEquals("Apple", created.getBrand());
        assertEquals(DeviceState.AVAILABLE, created.getState());
        assertEquals(Instant.parse(DATE_TIME_TEST), created.getCreationTime());
        verify(repository).save(created);
    }

    @Test
    void getReturnsDeviceWhenItExists() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        assertEquals(device, service.get(device.getId()));
    }

    @Test
    void getThrowsNotFoundForUnknownId() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(DeviceNotFoundException.class)
                .isThrownBy(() -> service.get(unknownId))
                .withMessageContaining(unknownId.toString());
    }

    @Test
    void replaceUpdatesAllMutableFields() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);

        when(repository.findById(device.getId()))
                .thenReturn(Optional.of(device));

        Device result = service.replace(device.getId(), "Galaxy S25", "Samsung", DeviceState.INACTIVE);

        assertEquals(device, result);
        assertEquals("Galaxy S25", result.getName());
        assertEquals("Samsung", result.getBrand());
        assertEquals(DeviceState.INACTIVE, result.getState());
        assertEquals(Instant.parse(DATE_TIME_TEST), result.getCreationTime());

        verify(repository).findById(device.getId());
        verify(repository, never()).save(any(Device.class));
    }

    @Test
    void replaceThrowsNotFoundForUnknownDevice() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(DeviceNotFoundException.class)
                .isThrownBy(() -> service.replace(id, "New name", "New brand", DeviceState.AVAILABLE))
                .withMessageContaining(id.toString());
    }

    @Test
    void replaceRejectsNameChangeWhenDeviceIsInUse() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        device.changeState(DeviceState.IN_USE);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        assertThatExceptionOfType(DeviceOperationNotAllowedException.class)
                .isThrownBy(() -> service.replace(device.getId(), "iPhone 15 Pro", "Apple", DeviceState.AVAILABLE));

        assertEquals("iPhone 15", device.getName());
        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    void replaceAllowsStateOnlyChangeForInUseDevice() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        device.changeState(DeviceState.IN_USE);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        Device replaced = service.replace(device.getId(), "iPhone 15", "Apple", DeviceState.INACTIVE);

        assertEquals(DeviceState.INACTIVE, replaced.getState());
    }

    @Test
    void patchChangesOnlyProvidedName() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        Device patched = service.patch(device.getId(), "iPhone 15 Pro", null, null);

        assertEquals("iPhone 15 Pro", patched.getName());
        assertEquals("Apple", patched.getBrand());
        assertEquals(DeviceState.AVAILABLE, patched.getState());
    }

    @Test
    void patchChangesOnlyProvidedBrand() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        Device patched = service.patch(device.getId(), null, "Samsung", null);

        assertEquals("iPhone 15", patched.getName());
        assertEquals("Samsung", patched.getBrand());
        assertEquals(DeviceState.AVAILABLE, patched.getState());
    }

    @Test
    void patchChangesOnlyProvidedState() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        Device patched = service.patch(device.getId(), null, null, DeviceState.INACTIVE);

        assertEquals("iPhone 15", patched.getName());
        assertEquals("Apple", patched.getBrand());
        assertEquals(DeviceState.INACTIVE, patched.getState());
    }

    @Test
    void patchWithAllNullValuesLeavesDeviceUnchanged() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        Device patched = service.patch(device.getId(), null, null, null);

        assertEquals("iPhone 15", patched.getName());
        assertEquals("Apple", patched.getBrand());
        assertEquals(DeviceState.AVAILABLE, patched.getState());
    }

    @Test
    void patchAllowsStateChangeWhenDeviceIsInUse() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        device.changeState(DeviceState.IN_USE);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        Device patched = service.patch(device.getId(), null, null, DeviceState.AVAILABLE);

        assertEquals(DeviceState.AVAILABLE, patched.getState());
    }

    @Test
    void patchCannotBypassInUseRuleByChangingNameAndStateTogether() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        device.changeState(DeviceState.IN_USE);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        assertThatExceptionOfType(DeviceOperationNotAllowedException.class)
                .isThrownBy(() -> service.patch(device.getId(), "iPhone 15 Pro", null, DeviceState.AVAILABLE));

        assertEquals("iPhone 15", device.getName());
        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    void patchThrowsNotFoundForUnknownDevice() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(DeviceNotFoundException.class)
                .isThrownBy(() -> service.patch(id, "New name", null, null));
    }

    @Test
    void deleteRemovesAvailableDevice() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        service.delete(device.getId());

        verify(repository).delete(device);
    }

    @Test
    void deleteRemovesInactiveDevice() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        device.changeState(DeviceState.INACTIVE);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        service.delete(device.getId());

        verify(repository).delete(device);
    }

    @Test
    void deleteRejectsInUseDevice() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        device.changeState(DeviceState.IN_USE);

        when(repository.findById(device.getId())).thenReturn(Optional.of(device));

        assertThatExceptionOfType(DeviceOperationNotAllowedException.class)
                .isThrownBy(() -> service.delete(device.getId()))
                .withMessageContaining("in use");

        verify(repository, never()).delete(any(Device.class));
        verify(repository, never()).deleteById(any(UUID.class));
    }

    @Test
    void deleteThrowsNotFoundForUnknownDevice() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(DeviceNotFoundException.class)
                .isThrownBy(() -> service.delete(id));

        verify(repository, never()).delete(any(Device.class));
    }
}