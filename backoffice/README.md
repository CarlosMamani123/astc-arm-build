# Backoffice Service

Microservicio de gestión de usuarios para Backoffice, construido con:

- **Java 21** + **Quarkus 3.25**
- **Hibernate 6** + Panache (PostgreSQL)
- **SmallRye GraphQL**
- **JWT** (SmallRye JWT)
- **Arquitectura Hexagonal**
- Native Image ready

## Estructura del Proyecto

```
src/main/java/com/backoffice/
├── shared/                          # Código compartido
│   ├── domain/                      # BaseAuditableEntity, PageResult
│   ├── infrastructure/config/       # SecurityConfig
│   └── util/                        # UuidGenerator (UUID v7)
└── backoffice/                      # Módulo Backoffice
    ├── application/
    │   ├── in/                      # Puertos de entrada (interfaces)
    │   └── usecase/                 # Implementaciones de casos de uso
    ├── domain/
    │   ├── entity/                  # Entidades JPA
    │   └── model/                   # Modelos de dominio puros
    └── infrastructure/
        ├── controller/graphql/      # Resolvers GraphQL + DTOs
        ├── messaging/               # Outbox processor
        ├── repository/              # Repositorios Panache
        └── sender/                  # Storage adapter (S3/MinIO)
```

## Requisitos

- Java 21+
- Maven 3.9+
- PostgreSQL 15+ (o Docker)
- (Opcional) Redis, RabbitMQ, MinIO

## Ejecutar en modo desarrollo

```bash
# Levantar PostgreSQL con Docker
docker run -d --name backoffice-db \
  -e POSTGRES_USER=backoffice \
  -e POSTGRES_PASSWORD=backoffice \
  -e POSTGRES_DB=backoffice \
  -p 5432:5432 postgres:18

# Ejecutar Quarkus
./mvnw quarkus:dev
```

GraphQL UI disponible en: http://localhost:8080/q/graphql-ui

## Configurar JWT

### 1. Generar par de claves RSA

```bash
openssl genrsa -out rsaPrivateKey.pem 2048
openssl rsa -pubout -in rsaPrivateKey.pem -out src/main/resources/META-INF/resources/publicKey.pem
openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
```

### 2. Generar token JWT de prueba

Usar la utilidad incluida o cualquier generador JWT con este payload:

```json
{
  "iss": "https://backoffice.example.com",
  "sub": "018f6b7a-0003-7000-8000-000000000001",
  "iat": 1700000000,
  "exp": 1800000000,
  "groups": ["ADMIN"],
  "roles": ["ADMIN"]
}
```

Firmar con la clave privada RSA256.

### 3. Usar en requests

```
Authorization: Bearer <token>
```

## Operaciones GraphQL

### Queries

```graphql
query {
  listUsers(filter: { keyword: "admin", page: 0, size: 10 }) {
    items {
      id username fullName email status roleName projectName
    }
    pageInfo {
      page size totalCount totalPages hasNextPage
    }
  }
}

query {
  getUserDetail(id: "018f6b7a-0003-7000-8000-000000000001") {
    id username firstName lastName fullName email phone
    roleCode roleName projectName status
    createdAt updatedAt deletedAt createdBy updatedBy
  }
}
```

### Mutations

```graphql
mutation {
  createUser(input: {
    username: "jdoe"
    firstName: "John"
    lastName: "Doe"
    email: "john@example.com"
    password: "SecurePass123!"
    roleId: "018f6b7a-0001-7000-8000-000000000003"
    projectId: "018f6b7a-0002-7000-8000-000000000001"
  }) {
    id username fullName email status
  }
}

mutation {
  editUser(input: {
    id: "018f6b7a-0003-7000-8000-000000000001"
    phone: "+0987654321"
    status: "INACTIVE"
  }) {
    id username status phone
  }
}

mutation {
  deleteUser(input: { id: "018f6b7a-0003-7000-8000-000000000001" })
}
```

## Compilar Native Image

```bash
./mvnw package -Pnative
```

## Migraciones

Las migraciones se ejecutan automáticamente con Liquibase al iniciar el servicio (contenedor: liquibase-backoffice).

Ubicación: `services/astc-db-backoffice/migration/changelog/`

## Roles del sistema

| Código | Nombre |
|--------|--------|
| ADMIN | Administrator |
| PROJECT_MANAGER | Project Manager |
| TEAM_MEMBER | Team Member |

## Docker Compose (completo)

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_USER: backoffice
      POSTGRES_PASSWORD: backoffice
      POSTGRES_DB: backoffice
    ports: ["5432:5432"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  rabbitmq:
    image: rabbitmq:3-management-alpine
    ports: ["5672:5672", "15672:15672"]

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports: ["9000:9000", "9001:9001"]
```
