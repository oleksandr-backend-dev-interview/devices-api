package dev.poslavskyi.devices.device.persistence;

import dev.poslavskyi.devices.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    @Query("SELECT d FROM Device d WHERE LOWER(d.brand) = LOWER(:brand)")
    List<Device> findByBrandIgnoreCase(@Param("brand") String brand);
}
