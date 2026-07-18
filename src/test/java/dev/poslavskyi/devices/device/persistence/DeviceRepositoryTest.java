package dev.poslavskyi.devices.device.persistence;

import dev.poslavskyi.devices.device.domain.Device;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DeviceRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    public static final String DATE_TIME_TEST = "2026-07-18T10:00:00Z";
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
}