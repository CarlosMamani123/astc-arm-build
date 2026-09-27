================================================================================
🔐 AUTH SERVICE - ESPECIFICACIÓN ENTERPRISE (IDENTITY PROVIDER)
================================================================================

🧠 1. RESUMEN DEL SERVICIO
--------------------------------------------------------------------------------
El auth-service es el proveedor de identidad (Identity Provider - IdP) del sistema ASTC.

Es el núcleo de seguridad del ecosistema y se encarga de autenticar usuarios,
generar tokens JWT y administrar la identidad global del sistema.

Este servicio es crítico (Tier 0):
> Si este servicio falla, todo el sistema queda inoperativo.

--------------------------------------------------------------------------------

🎯 2. RESPONSABILIDADES DEL SERVICIO
--------------------------------------------------------------------------------

Este servicio es responsable de:

✔ Autenticación de usuarios (login)
✔ Validación de credenciales con bcrypt
✔ Generación de tokens JWT (RS256)
✔ Gestión de identidad del usuario
✔ Inclusión de claims en el token
✔ Resolución de roles del usuario
✔ Seguridad central del sistema

--------------------------------------------------------------------------------

🚨 3. CRITICIDAD DEL SERVICIO
--------------------------------------------------------------------------------

Nivel:
🔴 TIER 0 - SERVICIO CRÍTICO

Dependencias:
- Todos los microservicios del sistema ASTC dependen de este servicio

Impacto:
- Sin auth-service no hay login
- Sin auth-service no hay autorización
- Sin auth-service no hay acceso al sistema

--------------------------------------------------------------------------------

🌐 4. CONTRATO DE API (GRAPHQL)
--------------------------------------------------------------------------------

Endpoint principal:
/graphql

--------------------------------------------------------------------------------

📊 QUERIES
--------------------------------------------------------------------------------

- ping → Health check del servicio (sin autenticación requerida)

--------------------------------------------------------------------------------

⚡ MUTATIONS
--------------------------------------------------------------------------------

- login(email: String!, password: String!): AuthResponse

--------------------------------------------------------------------------------

📦 5. FLUJO DE AUTENTICACIÓN
--------------------------------------------------------------------------------

El flujo de autenticación es el siguiente:

1. Frontend envía credenciales (email + password)
2. Auth Service busca usuario en PostgreSQL
3. Valida contraseña con bcrypt
4. Genera token JWT (RS256)
5. Responde con token + datos del usuario

El token JWT contiene:
- sub → ID del usuario
- email → correo del usuario
- role → rol del usuario (ADMIN | PROJECT_MANAGER | TEAM_MEMBER)
- groups → array con el rol
- iat → fecha de emisión
- exp → expiración (24 horas)
- issuer → "better-auth-verify"

--------------------------------------------------------------------------------

🗄️ 6. BASE DE DATOS (POSTGRESQL + LIQUIBASE)
--------------------------------------------------------------------------------

Propiedades:
- Base de datos independiente del servicio (schema: astc_auth)
- Migraciones gestionadas con Liquibase (contenedor: liquibase-auth)

TABLAS PRINCIPALES:

1. users
   - id (UUID)
   - username (único)
   - email (único)
   - password_hash (bcrypt)
   - first_name
   - last_name
   - full_name
   - phone
   - avatar_url
   - role_id (FK → roles.id)
   - created_at / updated_at / deleted_at

2. roles
   - id (UUID)
   - code (ADMIN | PROJECT_MANAGER | TEAM_MEMBER)
   - name (string)

--------------------------------------------------------------------------------

🔧 7. INTEGRACIONES
--------------------------------------------------------------------------------

Este servicio se integra con:

- PostgreSQL → persistencia de usuarios
- RabbitMQ → eventos (outbox pattern)
- Frontend Auth → endpoint de login
- Gateway → propagación de JWT
- Todos los microservicios → validación de token

================================================================================
FIN DEL ESPEC
================================================================================
