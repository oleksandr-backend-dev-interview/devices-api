# Devices API

REST API for managing device resources: create, update, fetch, filter, and delete devices.

## Tech stack

- Java 21, Spring Boot 4.1.0, Maven
- PostgreSQL 17, Flyway migrations, Spring Data JPA
- springdoc-openapi
- JUnit 5, Mockito, Testcontainers
- Docker / Docker Compose

## Running locally (development)

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Health check: http://localhost:8080/actuator/health

## Running everything in Docker

_TODO: added in the containerization phase (`docker compose up --build`)._

## API documentation

_TODO: Swagger UI link and endpoint summary._

## Running tests

```bash
./mvnw clean verify
```

### Important Mentions
* Hardcoded credentials in the docker-compose file are fine because this is a local dev environment. Real deployments override them via environment variables.

## Assumptions

_TODO: filled during implementation (see DECISIONS.md meanwhile)._

## Design decisions

See [DECISIONS.md](DECISIONS.md).