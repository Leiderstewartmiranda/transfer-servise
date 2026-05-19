# Transfer Service
### Microservicio de Transferencia de Dinero
**Java 17 · Spring Boot 3.2 · Arquitectura Hexagonal · JWT · SonarQube · Docker**

---

## Estructura del proyecto

```
transfer-service/
├── src/main/java/com/transfer/
│   ├── domain/                        # Núcleo de negocio (sin dependencias)
│   │   ├── model/                     # Transfer, Account, Money
│   │   ├── port/in/                   # TransferUseCase (interfaces de entrada)
│   │   ├── port/out/                  # TransferRepository, AccountRepository
│   │   ├── service/                   # TransferDomainService
│   │   └── exception/                 # Excepciones de dominio
│   ├── application/
│   │   └── service/TransferService.java   # Orquestador (@Service)
│   └── infrastructure/
│       ├── adapter/in/rest/           # Controllers REST + Auth
│       ├── adapter/out/persistence/   # Adaptadores JPA
│       ├── security/                  # JWT: Provider, Filter, EntryPoint
│       ├── config/                    # SecurityConfig, OpenApiConfig
│       ├── entity/                    # Entidades JPA
│       ├── mapper/                    # Mappers dominio <-> entidad
│       └── repository/                # Spring Data JPA repos
├── Dockerfile                         # Multi-stage build
├── docker-compose.yml                 # App + Postgres + SonarQube
├── Makefile                           # Comandos rápidos
├── sonar-project.properties           # Config SonarQube
└── .env.example                       # Plantilla de variables de entorno
```

---

## Inicio rápido (local con H2)

```bash
# Requiere Java 17+ y Maven
mvn spring-boot:run
# o simplemente:
make run
```

| URL | Descripción |
|-----|-------------|
| http://localhost:3000/swagger-ui.html | Swagger UI (con auth JWT) |
| http://localhost:3000/h2-console | Consola H2 (JDBC: `jdbc:h2:mem:transferdb`) |
| http://localhost:3000/actuator/health | Health check |

---

## Flujo JWT

### 1. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

### 2. Usar el token
```bash
# Transferencia
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": "a1b2c3d4-0000-0000-0000-000000000001",
    "targetAccountId": "a1b2c3d4-0000-0000-0000-000000000002",
    "amount": 500.00,
    "currency": "MXN",
    "description": "Pago"
  }'

# Consultar cuenta
curl http://localhost:8080/api/v1/accounts/a1b2c3d4-0000-0000-0000-000000000001 \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

### 3. Renovar token
```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<REFRESH_TOKEN>"}'
```

### Usuarios de desarrollo
| Usuario | Password  | Roles                   |
|---------|-----------|-------------------------|
| admin   | admin123  | ROLE_ADMIN, ROLE_USER   |
| user    | user123   | ROLE_USER               |

---

## SonarQube

### Paso 1 — Levantar SonarQube
```bash
make sonar-up
# Esperar ~60s, luego abrir http://localhost:9000
# Login: admin / admin (pedirá cambio de contraseña)
```

### Paso 2 — Crear token en SonarQube
```
Administration → Security → Users → admin → Tokens → Generate
```

### Paso 3 — Configurar .env
```bash
cp .env.example .env
# Editar .env y pegar el token:
# SONAR_TOKEN=sqp_xxxxxxxxxxxx
```

### Paso 4 — Analizar
```bash
make sonar
# Ver resultados en: http://localhost:9000/dashboard?id=transfer-service
```

El análisis reporta: bugs, vulnerabilidades, code smells, cobertura (JaCoCo ≥ 70% en domain/application) y security hotspots.

---

## Docker — Stack completo

```bash
# Copiar y configurar variables
cp .env.example .env   # editar DB_PASSWORD y JWT_SECRET

# Levantar todo (App + Postgres + SonarQube)
make docker-up

# Ver logs
make docker-logs

# Detener
make docker-down
```

---

## Tests

```bash
make test           # solo tests
make test-coverage  # tests + reporte JaCoCo en target/site/jacoco/index.html
```

---

## Endpoints

| Método | Endpoint | Auth | Descripción |
|--------|----------|:----:|-------------|
| POST | `/api/v1/auth/login` | ❌ | Obtener JWT |
| POST | `/api/v1/auth/refresh` | ❌ | Renovar access token |
| POST | `/api/v1/transfers` | ✅ | Ejecutar transferencia |
| GET  | `/api/v1/transfers/{id}` | ✅ | Obtener transferencia por ID |
| GET  | `/api/v1/transfers/account/{id}` | ✅ | Historial por cuenta |
| GET  | `/api/v1/accounts/{id}` | ✅ | Datos y saldo de cuenta |
| GET  | `/actuator/health` | ❌ | Health check |

---

## Cuentas de prueba

| ID (sufijo) | Titular | Saldo |
|-------------|---------|-------|
| `...000001` | Juan Pérez | $10,000 MXN |
| `...000002` | María García | $5,000 MXN |
| `...000003` | Carlos López | $2,500 MXN |
| `...000004` | Ana Martínez | $0 MXN |

ID completo: `a1b2c3d4-0000-0000-0000-00000000000X`
