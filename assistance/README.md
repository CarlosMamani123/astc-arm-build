# SMMS Assistance Service

Microservicio de Asistencia para el rol **Team Member** del sistema SMMS.

## Stack Tecnológico
- Java 21 + Quarkus 3.x
- Hibernate 6 + Panache
- SmallRye GraphQL
- PostgreSQL
- Redis
- RabbitMQ
- MinIO (S3-compatible)
- OpenTelemetry
- JWT (RS256)

## Arquitectura
Hexagonal estricta:
```
assistance/
├── application/
│   ├── in/          # Puertos de entrada (interfaces)
│   └── usecase/     # Implementaciones de casos de uso
├── domain/
│   ├── entity/      # Entidades JPA
│   └── model/       # Modelos de dominio, enums, value objects
└── infrastructure/
    ├── controller/graphql/     # Resolvers GraphQL + DTOs
    ├── messaging/              # Transactional Outbox
    ├── repository/             # Repositorios Panache
    ├── security/               # AuthContext (JWT)
    └── storage/                # Adapter S3/MinIO
```

## Tablas propias de este servicio
| Tabla | Descripción |
|-------|-------------|
| `attendances` | Registros de asistencia |
| `absences` | Faltas registradas |
| `justifications` | Justificaciones de faltas |
| `outbox_events` | Transactional Outbox para eventos |

### Tablas externas (NO creadas por este servicio)
- `users` — gestionada por Authentication Service
- `projects` — gestionada por Project Management Service
- `project_users` — gestionada por Project Management Service
- `alerts` — gestionada por Backoffice Service

## Operaciones GraphQL

### Queries
| Nombre | Descripción |
|--------|-------------|
| `getDashboard` | Resumen del Team Member autenticado |
| `listAttendance` | Lista asistencias con filtros y paginación |
| `listAbsences` | Lista faltas del usuario |
| `listJustifications` | Lista justificaciones con filtros |
| `getJustificationStatus` | Estado de una justificación |

### Mutations
| Nombre | Descripción |
|--------|-------------|
| `registerAttendance` | Registra asistencia |
| `justifyAbsence` | Marca una falta como justificada |
| `saveJustification` | Guarda justificación con documento |

## Ejecución local

### Con Docker Compose
```bash
cp .env.example .env
docker compose up -d
```

Servicios disponibles:
- **Assistance API**: http://localhost:8082/graphql
- **GraphQL UI**: http://localhost:8082/q/graphql-ui
- **MinIO Console**: http://localhost:9001
- **RabbitMQ Management**: http://localhost:15673
- **Health**: http://localhost:8082/q/health

### Sin Docker (desarrollo)
```bash
# Levantar dependencias
docker compose up -d postgres redis rabbitmq minio

# Ejecutar Quarkus en dev mode
./mvnw quarkus:dev
```

## Configuración JWT

1. Generar par de claves RSA:
```bash
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
```

2. Colocar `publicKey.pem` en `src/main/resources/`

3. Generar token de prueba (claims requeridos):
```json
{
  "sub": "USER_UUID",
  "email": "user@example.com",
  "role": "TEAM_MEMBER",
  "groups": ["TEAM_MEMBER"],
  "iss": "https://smms.auth.example.com",
  "exp": 9999999999
}
```

## Integración con smms-gateway-apollo

Este servicio expone su schema GraphQL en `http://localhost:8082/graphql`.

Para integrarlo con Apollo Gateway:
```javascript
// smms-gateway-apollo/gateway.config.js
const { ApolloGateway, RemoteGraphQLDataSource } = require("@apollo/gateway");

const gateway = new ApolloGateway({
  serviceList: [
    { name: "assistance", url: "http://smms-assistance-service:8082/graphql" },
    // otros servicios...
  ],
});
```

O con Apollo Federation / Schema Stitching:
- Este servicio puede ser registrado como subgraph
- El gateway `smms-gateway-apollo` centraliza todos los schemas
- El gateway `smms-gateway-external` actúa como reverse proxy HTTP

## Pruebas con Bruno

1. Importar la carpeta `bruno/` en Bruno
2. Configurar environment `local`
3. Reemplazar `jwt_token` con un token válido
4. Ejecutar queries y mutations

## Migraciones

Las migraciones se ejecutan automáticamente con Liquibase al iniciar el servicio (contenedor: liquibase-assistance).
Solo incluyen tablas del dominio Assistance.

Ubicación: `services/astc-db-assistance/migration/changelog/`

## Eventos de dominio (Transactional Outbox)
- `attendance_registered` — al registrar asistencia
- `absence_justified` — al justificar una falta
- `justification_saved` — al guardar una justificación

Los eventos se escriben en `outbox_events` dentro de la misma transacción.
Un poller externo los publica a RabbitMQ.
