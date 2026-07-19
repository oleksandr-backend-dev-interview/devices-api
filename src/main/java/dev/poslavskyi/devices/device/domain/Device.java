package dev.poslavskyi.devices.device.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    public void applyUpdate(
            String name,
            String brand,
            DeviceState state
    ) {
        String normalizedName = requireNonBlank(name, NAME).trim();
        String normalizedBrand = requireNonBlank(brand, BRAND).trim();
        DeviceState requiredState = Objects.requireNonNull(state, "state must not be null");

        boolean nameChanges = !normalizedName.equals(this.name);
        boolean brandChanges = !normalizedBrand.equals(this.brand);

        if (nameChanges || brandChanges) {
            ensureNotInUse(
                    "Changing the name or brand of a device in use is not allowed"
            );
        }

        // Reuse existing domain operations after validating the entire update.
        rename(normalizedName);
        changeBrand(normalizedBrand);
        changeState(requiredState);
    }

    public void rename(String newName) {
        String normalizedNewName = requireNonBlank(newName, NAME).trim();
        if (normalizedNewName.equals(this.name)) {
            return;
        }
        ensureNotInUse("Renaming the device in use is not allowed");
        this.name = normalizedNewName;
    }

    public void changeBrand(String newBrand) {
        String normalizedNewBrand = requireNonBlank(newBrand, BRAND).trim();
        if (normalizedNewBrand.equals(this.brand)) {
            return;
        }
        ensureNotInUse("Changing the brand of the device in use is not allowed");
        this.brand = normalizedNewBrand;
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
