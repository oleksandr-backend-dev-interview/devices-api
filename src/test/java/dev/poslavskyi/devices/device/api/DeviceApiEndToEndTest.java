package dev.poslavskyi.devices.device.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
@Testcontainers
class DeviceApiEndToEndTest {

    private static final String DEVICES_PATH = "/api/v1/devices";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    RestTestClient restClient;

    @Test
    void completeDeviceLifecycleWorksThroughHttpAndPostgres() {

        // Create an in-use device.

        var createResult = restClient.post()
                .uri(DEVICES_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "name": "iPhone 15",
                          "brand": "Apple",
                          "state": "in-use"
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(DeviceResponse.class)
                .returnResult();

        DeviceResponse created = Objects.requireNonNull(
                createResult.getResponseBody()
        );

        UUID id = created.id();

        assertThat(created.name()).isEqualTo("iPhone 15");
        assertThat(created.brand()).isEqualTo("Apple");
        assertThat(created.state()).isEqualTo("in-use");
        assertThat(created.creationTime()).isNotNull();

        assertThat(createResult.getResponseHeaders()
                .getFirst(HttpHeaders.LOCATION))
                .endsWith(DEVICES_PATH + "/" + id);

        // Fetch the device by ID.

        restClient.get()
                .uri(DEVICES_PATH + "/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id")
                .isEqualTo(id.toString())
                .jsonPath("$.state")
                .isEqualTo("in-use");

        // Fetch all devices.

        restClient.get()
                .uri(DEVICES_PATH)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(id.toString());

        // Fetch devices by brand. The lowercase query also proves case-insensitive matching.

        restClient.get()
                .uri(DEVICES_PATH + "?brand=apple")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(id.toString())
                .jsonPath("$[0].brand")
                .isEqualTo("Apple");

        // Fetch devices by state.

        restClient.get()
                .uri(DEVICES_PATH + "?state=in-use")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id")
                .isEqualTo(id.toString())
                .jsonPath("$[0].state")
                .isEqualTo("in-use");

        // Fully replace mutable fields. Name and brand remain unchanged, so an in-use device may be released by changing only its state.

        restClient.put()
                .uri(DEVICES_PATH + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "name": "iPhone 15",
                          "brand": "Apple",
                          "state": "available"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name")
                .isEqualTo("iPhone 15")
                .jsonPath("$.brand")
                .isEqualTo("Apple")
                .jsonPath("$.state")
                .isEqualTo("available");

        // Partially update name and state. The device is currently available, so changing its name and then moving it to in-use is allowed.

        restClient.patch()
                .uri(DEVICES_PATH + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "name": "iPhone 15 Pro",
                          "state": "in-use"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name")
                .isEqualTo("iPhone 15 Pro")
                .jsonPath("$.brand")
                .isEqualTo("Apple")
                .jsonPath("$.state")
                .isEqualTo("in-use");

        // Deleting an in-use device must fail.

        restClient.delete()
                .uri(DEVICES_PATH + "/{id}", id)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Operation not allowed")
                .jsonPath("$.status")
                .isEqualTo(409)
                .jsonPath("$.detail")
                .value(detail ->
                        assertThat(detail.toString())
                                .contains("in use")
                );

        // Release the device through PATCH.

        restClient.patch()
                .uri(DEVICES_PATH + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "state": "available"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.state")
                .isEqualTo("available");

        // Delete the released device.

        restClient.delete()
                .uri(DEVICES_PATH + "/{id}", id)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody()
                .isEmpty();

        // The deleted device must no longer exist.

        restClient.get()
                .uri(DEVICES_PATH + "/{id}", id)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Device not found")
                .jsonPath("$.status")
                .isEqualTo(404);
    }
}