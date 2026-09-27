================================================================================
🗄️ AUTH SERVICE - DATABASE SPECIFICATION
================================================================================

🧠 1. OVERVIEW
--------------------------------------------------------------------------------
La base de datos del auth-service es la Single Source of Truth (SSOT) del sistema ASTC
para la gestión de identidad, autenticación y roles.

Este esquema está diseñado para soportar:
- Autenticación segura de usuarios
- Gestión de roles globales
- Emisión de JWT (claims basados en BD)
- Escalabilidad horizontal del sistema de identidad

--------------------------------------------------------------------------------

🧱 2. DATABASE ENGINE
--------------------------------------------------------------------------------
- PostgreSQL 16+
- Encoding: UTF-8
- Timezone: UTC
- ORM: Prisma / Hibernate (según implementación)
- Migraciones: Flyway / Prisma Migrate

--------------------------------------------------------------------------------

📦 3. SCHEMA DESIGN
--------------------------------------------------------------------------------
El esquema está compuesto por las siguientes entidades:

- roles
- users

--------------------------------------------------------------------------------

🟦 4. TABLE: roles
--------------------------------------------------------------------------------

🎯 Purpose:
Define los roles globales del sistema ASTC.

--------------------------------------------------------------------------------

📐 STRUCTURE
--------------------------------------------------------------------------------

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL
);

--------------------------------------------------------------------------------

📊 FIELD DESCRIPTION
--------------------------------------------------------------------------------

- id   → Identificador único del rol (UUID)
- code → Código del rol (ADMIN, PROJECT_MANAGER, TEAM_MEMBER)
- name → Nombre descriptivo del rol

--------------------------------------------------------------------------------

🔐 BUSINESS RULES
--------------------------------------------------------------------------------

- El campo code debe ser único en toda la tabla
- No se permiten roles duplicados
- Los roles son globales (compartidos por todo el sistema)
- No se recomienda eliminación física en producción

--------------------------------------------------------------------------------

🟩 5. TABLE: users
--------------------------------------------------------------------------------

🎯 Purpose:
Almacena los usuarios del sistema utilizados para autenticación.

--------------------------------------------------------------------------------

📐 STRUCTURE
--------------------------------------------------------------------------------

CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,

    first_name VARCHAR(100),
    last_name VARCHAR(100),
    full_name VARCHAR(150),
    phone VARCHAR(30),
    avatar_url TEXT,

    role_id UUID NOT NULL REFERENCES roles(id),

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

--------------------------------------------------------------------------------

📊 FIELD DESCRIPTION
--------------------------------------------------------------------------------

- id            → Identificador único del usuario (UUID)
- username      → Nombre de usuario único
- email         → Email único de login
- password_hash → Contraseña encriptada (bcrypt)
- first_name    → Nombre del usuario
- last_name     → Apellido del usuario
- full_name     → Nombre completo (opcional)
- phone         → Número de teléfono
- avatar_url    → URL de imagen de perfil
- role_id       → Relación con roles (FK)
- created_at    → Fecha de creación del registro
- updated_at    → Fecha de actualización
- deleted_at    → Soft delete (eliminación lógica)

--------------------------------------------------------------------------------

🔗 RELATIONSHIPS
--------------------------------------------------------------------------------

users.role_id → roles.id

--------------------------------------------------------------------------------

🔐 BUSINESS RULES
--------------------------------------------------------------------------------

- El email debe ser único
- El username debe ser único
- Todo usuario debe tener un rol asignado
- password_hash nunca debe exponerse al frontend
- deleted_at se usa para soft delete
- Solo el auth-service puede gestionar credenciales

--------------------------------------------------------------------------------

🌱 6. INITIAL DATA (SEED)
--------------------------------------------------------------------------------

INSERT INTO roles (id, code, name)
VALUES
('018f6b7a-0001-7000-8000-000000000001', 'ADMIN', 'Administrator'),
('018f6b7a-0001-7000-8000-000000000002', 'PROJECT_MANAGER', 'Project Manager'),
('018f6b7a-0001-7000-8000-000000000003', 'TEAM_MEMBER', 'Team Member');

--------------------------------------------------------------------------------

🧠 7. SECURITY MODEL
--------------------------------------------------------------------------------

- Passwords almacenadas con bcrypt
- JWT generado a partir de datos de esta BD
- Role incluido en claims del token
- password_hash nunca se expone fuera del servicio

--------------------------------------------------------------------------------

⚙️ 8. PERFORMANCE RECOMMENDATIONS
--------------------------------------------------------------------------------

- Índices recomendados:
  - users.email
  - users.username
  - users.role_id

- Uso recomendado de soft delete (deleted_at)
- Evitar queries directas sin índice en producción

--------------------------------------------------------------------------------

🚀 9. SUMMARY
--------------------------------------------------------------------------------

Esta base de datos representa el núcleo de identidad del sistema ASTC.

Responsabilidades:
- Autenticación de usuarios
- Gestión de roles globales
- Fuente de datos para JWT
- Control de identidad centralizado

================================================================================