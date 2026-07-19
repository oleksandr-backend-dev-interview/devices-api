# Devices API

REST API for managing device resources: create, update, fetch, filter, and delete devices.

## Tech stack

- Java 21, Spring Boot 4.1.0, Maven
- PostgreSQL 17, Flyway migrations, Spring Data JPA
- springdoc-openapi
- JUnit 5, Mockito, Testcontainers
- Docker / Docker Compose

## Running everything in Docker

````bash
docker compose up --build
````
* Health check: http://localhost:8080/actuator/health
* App: http://localhost:8080
* Swagger UI: http://localhost:8080/swagger-ui.html

## API
| Method | Path | Description |
|---|---|---|
| POST | /api/v1/devices | Create device (state optional, defaults to `available`) |
| GET | /api/v1/devices/{id} | Fetch one |
| GET | /api/v1/devices?brand=&state= | Fetch all, optional filters (combinable) |
| PUT | /api/v1/devices/{id} | Full update |
| PATCH | /api/v1/devices/{id} | Partial update (omitted/null fields unchanged) |
| DELETE | /api/v1/devices/{id} | Delete |

### Error responses
* 400 invalid input 
* 404 unknown device 
* 409 business rule violation (rename/rebrand/delete of in-use device) or
concurrent modification (optimistic locking).

### Example requests
````bash
curl -X POST http://localhost:8080/api/v1/devices \
  -H "Content-Type: application/json" \
  -d '{"name":"iPhone 15","brand":"Apple"}'
````

In Windows PowerShell:
```declarative
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/devices" `
  -ContentType "application/json" `
  -Body '{"name":"iPhone 15","brand":"Apple"}'
```
```declarative
Invoke-RestMethod `
  -Method Get `
  -Uri "http://localhost:8080/api/v1/devices?brand=Apple&state=in-use"
```

## Running tests

```bash
./mvnw clean verify
```

## Important Mentions
### Local database credentials

The PostgreSQL credentials in compose.yaml are intentionally simple and are intended only for local development and evaluation.

They must not be reused for production deployments. A production environment should supply credentials through an appropriate secrets-management mechanism or deployment-specific configuration rather than storing them in the repository.

## Assumptions

- New devices default to `available`; create may optionally specify a state.
- Any state transition is permitted (no transition matrix specified).
- Brand filtering is case-insensitive exact match.
- "Update" means value change: writing identical name/brand to an in-use device is a no-op, not a violation.
- PATCH uses null-as-absent semantics (no device field is nullable, so explicit-null has no meaning to express).
- List endpoints are unpaginated at current scope.

## Known limitations & future improvements
- Pagination and sorting on list endpoints (Pageable is a small change; kept out to match the literal acceptance criteria).
- AuthN/AuthZ (Spring Security + JWT/OAuth2) — out of scope per task.
- Observability: metrics (Micrometer/Prometheus), structured logging, tracing.
- CI pipeline running `mvn verify` with Testcontainers.
- UUIDv7 identifiers to avoid B-tree fragmentation of random v4.
- ETag/If-Match to expose optimistic locking to clients instead of bare 409.
- Device lifecycle events (outbox pattern → message broker) if other systems need to react to state changes.

## Design decisions

See [DECISIONS.md](DECISIONS.md).

## Final verification

````bash
git clone git@github.com:oleksandr-backend-dev-interview/devices-api.git devices-api
cd devices-api
./mvnw clean verify
docker compose up --build -d
````

