package dev.poslavskyi.devices.device.api;

import dev.poslavskyi.devices.device.domain.Device;
import dev.poslavskyi.devices.device.domain.DeviceState;
import dev.poslavskyi.devices.device.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> create(@Valid @RequestBody CreateDeviceRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        Device device = service.create(request.name(), request.brand(), request.state());
        URI location = uriBuilder.path("/api/v1/devices/{id}").build(device.getId());
        return ResponseEntity.created(location).body(DeviceResponse.from(device));
    }

    @GetMapping("/{id}")
    public DeviceResponse get(@PathVariable UUID id) {
        return DeviceResponse.from(service.get(id));
    }

    @GetMapping
    public List<DeviceResponse> list(@RequestParam(required = false) String brand,
                                     @RequestParam(required = false) DeviceState state) {
        return service.list(brand, state).stream().map(DeviceResponse::from).toList();
    }

    @PutMapping("/{id}")
    public DeviceResponse replace(@PathVariable UUID id, @Valid @RequestBody ReplaceDeviceRequest request) {
        return DeviceResponse.from(service.replace(id, request.name(), request.brand(), request.state()));
    }

    @PatchMapping("/{id}")
    public DeviceResponse patch(@PathVariable UUID id, @RequestBody PatchDeviceRequest request) {
        return DeviceResponse.from(service.patch(id, request.name(), request.brand(), request.state()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}