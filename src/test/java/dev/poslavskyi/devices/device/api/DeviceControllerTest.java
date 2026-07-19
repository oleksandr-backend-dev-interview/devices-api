package dev.poslavskyi.devices.device.api;

import dev.poslavskyi.devices.device.api.error.GlobalExceptionHandler;
import dev.poslavskyi.devices.device.domain.Device;
import dev.poslavskyi.devices.device.domain.DeviceNotFoundException;
import dev.poslavskyi.devices.device.domain.DeviceOperationNotAllowedException;
import dev.poslavskyi.devices.device.domain.DeviceState;
import dev.poslavskyi.devices.device.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeviceController.class)
@Import({
        GlobalExceptionHandler.class,
        DeviceStateConverter.class,
        DeviceStateJacksonComponent.class
})
class DeviceControllerTest {

    private static final String DEVICES_PATH = "/api/v1/devices";
    private static final Instant FIXED_TIME =
            Instant.parse("2026-07-18T10:00:00Z");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeviceService service;

    @Test
    void createValidDeviceReturnsCreatedResponse() throws Exception {
        Device device = device(
                "iPhone 15",
                "Apple",
                DeviceState.AVAILABLE
        );

        when(service.create(
                "iPhone 15",
                "Apple",
                null
        )).thenReturn(device);

        mockMvc.perform(post(DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "iPhone 15",
                                  "brand": "Apple"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        endsWith(
                                DEVICES_PATH + "/" + device.getId()
                        )
                ))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id")
                        .value(device.getId().toString()))
                .andExpect(jsonPath("$.name")
                        .value("iPhone 15"))
                .andExpect(jsonPath("$.brand")
                        .value("Apple"))
                .andExpect(jsonPath("$.state")
                        .value("available"))
                .andExpect(jsonPath("$.creationTime")
                        .value(FIXED_TIME.toString()));

        verify(service).create(
                "iPhone 15",
                "Apple",
                null
        );
    }

    @Test
    void createAcceptsInUseWireState() throws Exception {
        Device device = device(
                "Tracker",
                "Acme",
                DeviceState.IN_USE
        );

        when(service.create(
                "Tracker",
                "Acme",
                DeviceState.IN_USE
        )).thenReturn(device);

        mockMvc.perform(post(DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tracker",
                                  "brand": "Acme",
                                  "state": "in-use"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state")
                        .value("in-use"));

        /*
         * This verification proves that the JSON value "in-use"
         * was deserialized to DeviceState.IN_USE.
         */
        verify(service).create(
                "Tracker",
                "Acme",
                DeviceState.IN_USE
        );
    }

    @Test
    void createWithBlankNameReturnsValidationProblem() throws Exception {
        mockMvc.perform(post(DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   ",
                                  "brand": "Apple"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Validation failed"))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.detail")
                        .value(containsString(
                                "name must not be blank"
                        )));

        verifyNoInteractions(service);
    }

    @Test
    void listWithoutFiltersRequestsAllDevices() throws Exception {
        Device device = device(
                "iPhone 15",
                "Apple",
                DeviceState.AVAILABLE
        );

        when(service.list(null, null))
                .thenReturn(List.of(device));

        mockMvc.perform(get(DEVICES_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id")
                        .value(device.getId().toString()))
                .andExpect(jsonPath("$[0].state")
                        .value("available"));

        verify(service).list(null, null);
    }

    @Test
    void listByBrandPassesBrandToService() throws Exception {
        Device device = device(
                "iPhone 15",
                "Apple",
                DeviceState.AVAILABLE
        );

        when(service.list("Apple", null))
                .thenReturn(List.of(device));

        mockMvc.perform(get(DEVICES_PATH)
                        .queryParam("brand", "Apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].brand")
                        .value("Apple"));

        verify(service).list("Apple", null);
    }

    @Test
    void listByStateConvertsWireValueToEnum() throws Exception {
        when(service.list(
                null,
                DeviceState.IN_USE
        )).thenReturn(List.of());

        mockMvc.perform(get(DEVICES_PATH)
                        .queryParam("state", "in-use"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        /*
         * This proves DeviceStateConverter converted "in-use"
         * from the query parameter to DeviceState.IN_USE.
         */
        verify(service).list(
                null,
                DeviceState.IN_USE
        );
    }

    @Test
    void getUnknownDeviceReturnsNotFoundProblem() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.get(id))
                .thenThrow(new DeviceNotFoundException(id));

        mockMvc.perform(get(DEVICES_PATH + "/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Device not found"))
                .andExpect(jsonPath("$.status")
                        .value(404))
                .andExpect(jsonPath("$.detail")
                        .value(containsString(id.toString())));
    }

    @Test
    void replaceForbiddenDeviceChangeReturnsConflict() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.replace(
                id,
                "iPhone 15 Pro",
                "Apple",
                DeviceState.AVAILABLE
        )).thenThrow(
                new DeviceOperationNotAllowedException(
                        "Renaming the device in use is not allowed"
                )
        );

        mockMvc.perform(put(DEVICES_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "iPhone 15 Pro",
                                  "brand": "Apple",
                                  "state": "available"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Operation not allowed"))
                .andExpect(jsonPath("$.status")
                        .value(409))
                .andExpect(jsonPath("$.detail")
                        .value(containsString("in use")));
    }

    @Test
    void patchPassesNullForOmittedProperties() throws Exception {
        UUID id = UUID.randomUUID();

        Device device = device(
                "iPhone 15",
                "Apple",
                DeviceState.AVAILABLE
        );

        when(service.patch(
                id,
                null,
                null,
                DeviceState.AVAILABLE
        )).thenReturn(device);

        mockMvc.perform(patch(DEVICES_PATH + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "state": "available"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("iPhone 15"))
                .andExpect(jsonPath("$.brand")
                        .value("Apple"))
                .andExpect(jsonPath("$.state")
                        .value("available"));

        /*
         * Omitted name and brand become null in PatchDeviceRequest.
         */
        verify(service).patch(
                id,
                null,
                null,
                DeviceState.AVAILABLE
        );
    }

    @Test
    void invalidStateInBodyReturnsBadRequestWithAllowedValues()
            throws Exception {

        mockMvc.perform(post(DEVICES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tracker",
                                  "brand": "Acme",
                                  "state": "broken-state"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Invalid request body"))
                .andExpect(jsonPath("$.status")
                        .value(400))
                .andExpect(jsonPath("$.detail")
                        .value(containsString("Allowed:")));

        verifyNoInteractions(service);
    }

    @Test
    void invalidUuidPathVariableReturnsBadRequest() throws Exception {
        mockMvc.perform(get(
                        DEVICES_PATH + "/not-a-valid-uuid"
                ))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON
                ))
                .andExpect(jsonPath("$.title")
                        .value("Invalid parameter"))
                .andExpect(jsonPath("$.status")
                        .value(400));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(
                        DEVICES_PATH + "/{id}",
                        id
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(id);
    }

    private static Device device(
            String name,
            String brand,
            DeviceState state
    ) {
        Device device = new Device(name, brand, FIXED_CLOCK);
        device.changeState(state);
        return device;
    }
}