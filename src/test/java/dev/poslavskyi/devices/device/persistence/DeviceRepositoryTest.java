package dev.poslavskyi.devices.device.persistence;

import dev.poslavskyi.devices.device.domain.Device;
import dev.poslavskyi.devices.device.domain.DeviceState;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DeviceRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final String DATE_TIME_TEST = "2026-07-18T10:00:00Z";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse(DATE_TIME_TEST), ZoneOffset.UTC);

    @Autowired
    DeviceRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    void savedDeviceSurvivesFullRoundTrip() {
        Device device = getDevice();
        UUID id = device.getId();
        repository.saveAndFlush(device);
        entityManager.clear();

        Device reloaded = repository.findById(id).orElseThrow();
        assertEquals(id, reloaded.getId());
        assertEquals(device.getName(), reloaded.getName());
        assertEquals(device.getBrand(), reloaded.getBrand());
        assertEquals(device.getState(), reloaded.getState());
        assertEquals(Instant.parse(DATE_TIME_TEST), reloaded.getCreationTime());
    }

    @Test
    void checkConstraintRejectsUnknownState() {
        assertThatExceptionOfType(Exception.class).isThrownBy(() ->
                entityManager.createNativeQuery("""
                        INSERT INTO device (id, name, brand, state, creation_time, version)
                        VALUES (:id, 'someName', 'someBrand', 'someUnknownState', now(), 0)
                        """)
                        .setParameter("id", UUID.randomUUID())
                        .executeUpdate());
    }

    private static Device getDevice() {
        Device device = new Device("iPhone 15", "Apple", FIXED_CLOCK);
        return device;
    }

    @Test
    void findsDevicesByBrandIgnoringCase() {
        repository.save(new Device("iPhone 15", "Apple", FIXED_CLOCK));
        repository.save(new Device("iPhone 16", "APPLE", FIXED_CLOCK));
        repository.save(new Device("Galaxy S25", "Samsung", FIXED_CLOCK));
        repository.flush();

        List<Device> devices = repository.findAllByBrandIgnoreCase("apple");

        assertThat(devices)
                .extracting(Device::getName) // Specifies the type
                .containsExactlyInAnyOrder(
                        "iPhone 15",
                        "iPhone 16"
                );
    }

    @Test
    void findsDevicesByState() {
        Device available = new Device("iPhone 15", "Apple", FIXED_CLOCK);

        Device inactive = new Device("Galaxy S25", "Samsung", FIXED_CLOCK);
        inactive.changeState(DeviceState.INACTIVE);

        repository.saveAll(List.of(available, inactive));
        repository.flush();

        List<Device> devices = repository.findAllByState(DeviceState.INACTIVE);

        assertThat(devices)
                .extracting(Device::getName)
                .containsExactly("Galaxy S25");
    }

    @Test
    void findsDevicesByBrandAndState() {
        Device availableApple = new Device("iPhone 15", "Apple", FIXED_CLOCK);

        Device inactiveApple = new Device("iPhone 13", "APPLE", FIXED_CLOCK);
        inactiveApple.changeState(DeviceState.INACTIVE);

        Device inactiveSamsung = new Device("Galaxy S25", "Samsung", FIXED_CLOCK);
        inactiveSamsung.changeState(DeviceState.INACTIVE);

        repository.saveAll(List.of(availableApple, inactiveApple, inactiveSamsung));
        repository.flush();

        List<Device> devices = repository.findAllByBrandIgnoreCaseAndState("apple", DeviceState.INACTIVE);

        assertThat(devices)
                .extracting(Device::getName)
                .containsExactly("iPhone 13");
    }

    @Test
    void entityUpdateIsPersistedThroughDirtyChecking() {
        Device saved = repository.saveAndFlush(getDevice());
        UUID id = saved.getId();

        saved.rename("iPhone 15 Pro");
        repository.flush();
        entityManager.clear();

        Device reloaded = repository.findById(id).orElseThrow();

        assertEquals("iPhone 15 Pro", reloaded.getName());
        assertEquals(Instant.parse(DATE_TIME_TEST), reloaded.getCreationTime());
    }

    @Test
    void versionIsInitializedAndIncrementedAfterUpdate() {
        Device saved = repository.saveAndFlush(getDevice());
        UUID id = saved.getId();

        long initialVersion = readVersion(id);

        saved.rename("iPhone 15 Pro");
        repository.flush();

        long updatedVersion = readVersion(id);

        assertEquals(0, initialVersion);
        assertEquals(initialVersion + 1, updatedVersion);
    }

    private long readVersion(UUID id) {
        Number version = (Number) entityManager.createNativeQuery("""
            SELECT version
            FROM device
            WHERE id = :id
            """)
                .setParameter("id", id)
                .getSingleResult();

        return version.longValue();
    }
}