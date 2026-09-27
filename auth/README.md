# Auth Service (Identity Provider)

Microservicio de autenticación y generación de tokens JWT para el sistema ASTC.

## Stack Tecnológico

- Java 21 + Quarkus 3.x
- Hibernate 6 + Panache
- SmallRye GraphQL
- JWT (SmallRye JWT - RS256)
- PostgreSQL
- Arquitectura Hexagonal

## Descripción

El auth-service es el **Identity Provider (IdP)** del sistema ASTC. Es el núcleo de seguridad y se encarga de:

- Autenticar usuarios (login)
- Validar credenciales con bcrypt
- Generar tokens JWT (RS256)
- Gestionar identidad del usuario
- Resolver roles del usuario

**Criticidad:** Tier 0 - Si este servicio falla, todo el sistema queda inoperativo.

## Arquitectura

```
auth/
├── application/
│   ├── in/              # Puertos de entrada (interfaces)
│   └── usecase/         # Implementaciones de casos de uso
├── domain/
│   ├── entity/          # Entidades JPA
│   └── model/           # Modelos de dominio
└── infrastructure/
    ├── controller/graphql/  # Resolvers GraphQL
    ├── repository/          # Repositorios Panache
    └── security/            # JWT + AuthContext
```

## API GraphQL

### Endpoint

```
/graphql
```

### Queries

| Nombre | Descripción |
|--------|-------------|
| `ping` | Health check del servicio (sin auth) |

### Mutations

| Nombre | Descripción |
|--------|-------------|
| `login(email, password)` | Autenticar usuario y retornar JWT |

### Ejemplo de Login

```graphql
mutation {
  login(email: "user@astc.com", password: "password123") {
    token
    user {
      id
      email
      role
    }
  }
}
```

## Variables de Entorno

| Variable | Descripción |
|----------|-------------|
| `DB_JDBC_URL` | URL JDBC de PostgreSQL |
| `DB_USERNAME` | Usuario de BD |
| `DB_PASSWORD` | Contraseña de BD |
| `QUARKUS_HTTP_PORT` | Puerto del servicio (default: 8082) |

## Ejecución Local

```bash
# Con Docker Compose
docker compose up -d liquibase-auth auth-service

# Sin Docker
./mvnw quarkus:dev
```

GraphQL UI: http://localhost:8082/q/graphql-ui

## Configurar JWT

### 1. Generar par de claves RSA

```bash
openssl genrsa -out rsaPrivateKey.pem 2048
openssl rsa -pubout -in rsaPrivateKey.pem -out src/main/resources/META-INF/resources/publicKey.pem
openssl pkcs8 -topk8 -nocrypt -inform pem -in rsaPrivateKey.pem -outform pem -out privateKey.pem
```

### 2. Generar token JWT de prueba

```json
{
  "iss": "https://astc.auth.example.com",
  "sub": "USER_UUID",
  "iat": 1700000000,
  "exp": 1800000000,
  "groups": ["TEAM_MEMBER"],
  "role": "TEAM_MEMBER"
}
```

## Roles del Sistema

| Código | Nombre |
|--------|--------|
| ADMIN | Administrator |
| PROJECT_MANAGER | Project Manager |
| TEAM_MEMBER | Team Member |

## Docker

```bash
# Puerto mapeado: 8081:8082
docker compose up -d auth-service
```

## Integraciones

- **assistance-service**: Valida JWT para operaciones de asistencia
- **project-assistance-service**: Valida JWT para gestión de proyectos
- **backoffice-service**: Valida JWT para administración
- **frontend-auth**: Consumo del endpoint de login
