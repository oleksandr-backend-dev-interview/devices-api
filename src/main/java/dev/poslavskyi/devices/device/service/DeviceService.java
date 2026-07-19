package dev.poslavskyi.devices.device.service;

import dev.poslavskyi.devices.device.domain.Device;
import dev.poslavskyi.devices.device.domain.DeviceNotFoundException;
import dev.poslavskyi.devices.device.domain.DeviceState;
import dev.poslavskyi.devices.device.persistence.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DeviceService {
    private final DeviceRepository repository;
    private final Clock clock;

    public DeviceService(DeviceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Device create(String name, String brand, DeviceState state) {
        Device device = new Device(name, brand, clock);
        if (state != null) {
            device.changeState(state);
        }
        return repository.save(device);
    }

    @Transactional(readOnly = true)
    public Device get(UUID id) {
        return findOrThrow(id);
    }

    @Transactional(readOnly = true)
    public List<Device> list(String brandFilter, DeviceState stateFilter) {
        String normalizedBrand = normalizeBrand(brandFilter);

        List<Device> devices;

        if (Objects.nonNull(normalizedBrand) && Objects.nonNull(stateFilter)) {
            devices = repository
                    .findAllByBrandIgnoreCaseAndState(
                            normalizedBrand,
                            stateFilter
                    );
        } else if (Objects.nonNull(normalizedBrand)) {
            devices = repository
                    .findAllByBrandIgnoreCase(normalizedBrand);
        } else if (Objects.nonNull(stateFilter)) {
            devices = repository.findAllByState(stateFilter);
        } else {
            devices = repository.findAll();
        }
        return devices;
    }

    @Transactional
    public Device replace(UUID id, String name, String brand, DeviceState state) {
        Device device = findOrThrow(id);
        device.applyUpdate(name, brand, state);
        return device;
    }

    @Transactional
    public Device patch(UUID id, String name, String brand, DeviceState state) {
        Device device = findOrThrow(id);

        if (name != null) {
            device.rename(name);
        }

        if (brand != null) {
            device.changeBrand(brand);
        }

        if (state != null) {
            device.changeState(state);
        }

        return device;
    }

    @Transactional
    public void delete(UUID id) {
        Device device = findOrThrow(id);
        device.ensureCanBeDeleted();
        repository.delete(device);
    }

    private String normalizeBrand(String brand) {
        if (Objects.isNull(brand)) {
            return null;
        }
        String trimmed = brand.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Device findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));
    }
}
