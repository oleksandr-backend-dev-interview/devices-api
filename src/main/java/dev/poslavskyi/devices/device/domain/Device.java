package dev.poslavskyi.devices.device.domain;

import jakarta.persistence.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "device")
public class Device {
    private static final String NAME = "name";
    private static final String BRAND = "brand";

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceState state;

    @Column(name = "creation_time", nullable = false, updatable = false)
    private Instant creationTime;

    @Version
    private Long version;

    protected Device() {

    }

    public Device(String name, String brand, Clock clock) {
        this.id = UUID.randomUUID();
        this.name = requireNonBlank(name, NAME).trim();
        this.brand = requireNonBlank(brand, BRAND).trim();
        this.state = DeviceState.AVAILABLE;
        this.creationTime = clock.instant();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public DeviceState getState() {
        return state;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public void rename(String newName) {
        ensureNotInUse("Renaming the device in use is not allowed");
        this.name = requireNonBlank(newName, NAME).trim();
    }

    public void changeBrand(String newBrand) {
        ensureNotInUse("Changing the brand of the device in use is not allowed");
        this.brand = requireNonBlank(newBrand, BRAND).trim();
    }

    public void ensureCanBeDeleted() {
        ensureNotInUse("Deleting the device in use is not allowed");
    }

    private void ensureNotInUse(String message) {
        if (this.state.equals(DeviceState.IN_USE)) {
            throw new DeviceOperationNotAllowedException(message);
        }
    }

    public void changeState(DeviceState newState) {
        Objects.requireNonNull(newState, "state");
        this.state = newState;
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
