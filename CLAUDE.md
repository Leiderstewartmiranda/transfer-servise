# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run locally (H2 in-memory DB, no setup needed)
mvn spring-boot:run
# or
make run

# Run all tests
mvn test
# or
make test

# Run a single test class
mvn test -Dtest=TransferServiceTest

# Run a single test method
mvn test -Dtest=TransferServiceTest#executeTransfer_fullFlow

# Tests + JaCoCo coverage report (target/site/jacoco/index.html)
mvn verify
# or
make test-coverage

# Build JAR (skip tests)
mvn package -DskipTests

# SonarQube analysis (requires .env with SONAR_TOKEN)
cp .env.example .env   # fill in SONAR_TOKEN after starting SonarQube
make sonar-up          # wait ~60s for SonarQube at http://localhost:9000
make sonar

# Full Docker stack (App + Postgres + SonarQube)
cp .env.example .env   # fill in DB_PASSWORD and JWT_SECRET
make docker-up
make docker-logs
make docker-down
```

## Architecture

The project follows **Hexagonal Architecture** (Ports & Adapters). The key rule is that **dependency arrows always point inward**: infrastructure → application → domain. The domain layer has zero framework dependencies.

```
domain/          Pure business logic — no Spring, no JPA
  model/         Account, Transfer (with Builder), Money (value object)
  port/in/       TransferUseCase (interface), TransferCommand (record)
  port/out/      AccountRepository, TransferRepository (interfaces)
  service/       TransferDomainService — pure transfer execution logic
  exception/     Domain exceptions (AccountNotFoundException, etc.)

application/
  service/       TransferService — Spring @Service, orchestrates the use case,
                 owns the @Transactional boundary

infrastructure/
  adapter/in/rest/     REST controllers (TransferController, AccountController,
                       AuthController) — depend only on domain ports/models
  adapter/out/         JPA adapters implementing domain port interfaces
  entity/              JPA entities (separate from domain models)
  mapper/              Manual mappers between domain models and JPA entities
  repository/          Spring Data JPA interfaces
  security/            JWT: JwtTokenProvider, JwtAuthenticationFilter,
                       JwtProperties, UserDetailsServiceImpl
  config/              SecurityConfig, DataInitializer, GlobalExceptionHandler,
                       OpenApiConfig
```

### Request flow

`REST Controller → TransferUseCase (port) → TransferService (application) → TransferDomainService (domain) → AccountRepository / TransferRepository (ports) → JPA adapters → DB`

### Key design decisions

- **Domain models are not JPA entities.** `Account` and `Transfer` are plain Java classes with no annotations. JPA entities live in `infrastructure/entity/` and are mapped via `AccountMapper` / `TransferMapper`.
- **Transfer state machine:** `PENDING → COMPLETED` (via `transfer.complete()`) or `PENDING → FAILED` (via `transfer.fail()`). The transfer is persisted in `PENDING` before the domain service executes, then updated. On exception it is saved as `FAILED` and the exception is re-thrown (letting Spring roll back the account saves).
- **Authorization in controllers:** `ROLE_ADMIN` can operate on any account. `ROLE_USER` can only transfer from/view their own linked account. `UserDetailsServiceImpl` maps username → `accountId` for this check.
- **JWT tokens carry a `"type"` claim** (`"access"` or `"refresh"`). The filter rejects refresh tokens used on protected endpoints via `isAccessToken()`.
- **Data initialization:** `DataInitializer` seeds the `admin` user on startup (linked to `a1b2c3d4-0000-0000-0000-000000000004`). Test accounts are seeded via `data.sql` (H2) / `init-postgres.sql` (Postgres).

### Authentication endpoints (no JWT required)

| Endpoint | Description |
|---|---|
| `POST /api/v1/auth/login` | Returns `accessToken`, `refreshToken`, `accountId` |
| `POST /api/v1/auth/register` | Creates user + account with 0 COP balance |
| `POST /api/v1/auth/refresh` | Returns new `accessToken` from a valid `refreshToken` |

### Test accounts (H2 dev mode)

| UUID suffix | Owner | Balance |
|---|---|---|
| `...000001` | Juan Pérez | $10,000 MXN |
| `...000002` | María García | $5,000 MXN |
| `...000003` | Carlos López | $2,500 MXN |
| `...000004` | Ana Martínez / admin | $0 MXN |

Full UUID format: `a1b2c3d4-0000-0000-0000-00000000000X`

### Tests

- **Unit tests** (`TransferDomainServiceTest`, `TransferServiceTest`, `JwtTokenProviderTest`) use Mockito, no Spring context.
- **Integration tests** (`TransferControllerIntegrationTest`, `AuthControllerIntegrationTest`) use `@SpringBootTest` + `MockMvc` + H2. They log in as `admin` in `@BeforeEach` and reuse the token.
- JaCoCo enforces ≥ 70% line coverage on `com.transfer.domain.*` and `com.transfer.application.*` packages during `mvn verify`.

### Frontend

Static SPA in `frontend/` (plain HTML/CSS/JS, no build step). Connects directly to `http://localhost:8080/api/v1`. Tokens stored in `localStorage` under keys `ts_access`, `ts_refresh`, `ts_user`. Includes automatic token refresh on 401 responses. Serve it by opening `frontend/index.html` in a browser while the backend is running.

### Profiles

| Profile | DB | Activated by |
|---|---|---|
| default | H2 in-memory | `mvn spring-boot:run` |
| prod | PostgreSQL | `SPRING_PROFILES_ACTIVE=prod` + `.env` |
| test | H2 (separate config) | `application-test.properties` |
