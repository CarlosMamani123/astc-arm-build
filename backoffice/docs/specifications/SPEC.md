================================================================================
🧠 BACKOFFICE SERVICE - ESPECIFICACIÓN DE DOMINIO (ENTERPRISE)
================================================================================

📌 RESUMEN DEL SERVICIO
--------------------------------------------------------------------------------
El backoffice-service es un microservicio central de administración del sistema
ASTC, encargado de la gestión global de usuarios, roles y operaciones
administrativas del sistema.

Este servicio actúa como el núcleo administrativo (ADMIN CORE) de la plataforma.

--------------------------------------------------------------------------------

🎯 RESPONSABILIDADES DEL SERVICIO
--------------------------------------------------------------------------------

Este servicio es responsable de:

✔ Gestión completa de usuarios del sistema
✔ Creación, edición y eliminación de usuarios
✔ Gestión de roles del sistema
✔ Asignación de roles a usuarios
✔ Consultas administrativas globales
✔ Soporte para el panel de administración (Backoffice UI)

--------------------------------------------------------------------------------

🏗️ ARQUITECTURA INTERNA
--------------------------------------------------------------------------------

El servicio sigue arquitectura limpia (Clean Architecture):

application/
├── usecases/          (casos de uso: CreateUser, EditUser, DeleteUser)

domain/
├── model/             (User)
├── entity/            (Role)

infrastructure/
├── controller/graphql (GraphQL resolvers)
├── repository         (acceso a datos)
├── dto                (mapeo request/response)
├── security          (validación JWT y roles)

--------------------------------------------------------------------------------

Principio clave:
> El dominio es independiente de frameworks y bases de datos.

================================================================================

🌐 API GRAPHQL
================================================================================

Endpoint principal:
/graphql

--------------------------------------------------------------------------------

📊 QUERIES
--------------------------------------------------------------------------------

- ListUsers  → Lista todos los usuarios del sistema
- GetUser    → Obtiene detalle de un usuario específico

--------------------------------------------------------------------------------

⚡ MUTATIONS
--------------------------------------------------------------------------------

- CreateUser → Crear nuevo usuario
- EditUser   → Editar usuario existente
- DeleteUser → Eliminar usuario del sistema

================================================================================

🔐 SEGURIDAD
================================================================================

Autenticación:
- JWT emitido por auth-service

Issuer:
- auth-service

Rol requerido:
- ADMIN

Reglas de seguridad:

✔ Solo usuarios con rol ADMIN pueden acceder
✔ Todos los endpoints están protegidos con @RolesAllowed("ADMIN")
✔ El usuario autenticado se obtiene desde SecurityIdentity
✔ No se permite acceso sin token válido

--------------------------------------------------------------------------------

📌 PRINCIPIO DE SEGURIDAD:
> Este servicio es completamente administrativo y restringido.

================================================================================

🗄️ BASE DE DATOS (POSTGRESQL + LIQUIBASE)
================================================================================

Propiedades:
- Base de datos independiente del servicio (schema: astc_backoffice)
- Migraciones gestionadas con Liquibase (contenedor: liquibase-backoffice)
- Sin dependencias de otros microservicios

--------------------------------------------------------------------------------

📌 TABLAS PRINCIPALES
--------------------------------------------------------------------------------

1. users
--------------------------------------------------------------------------------
Tabla principal de usuarios del sistema.

Campos:
- id (UUID)
- username (único)
- email (único)
- password (hash)
- first_name
- last_name
- phone
- role_id (FK lógica)

--------------------------------------------------------------------------------

2. roles
--------------------------------------------------------------------------------
Catálogo de roles del sistema.

Campos:
- id (UUID)
- code (ADMIN, USER, PROJECT_MANAGER, etc.)
- description / name

--------------------------------------------------------------------------------

🔗 RELACIONES
--------------------------------------------------------------------------------

- users.role_id → roles.id

Nota:
- Relación lógica gestionada por aplicación
- Puede existir validación a nivel de repositorio

================================================================================

📦 STORAGE (MINIO)
================================================================================

Bucket:
- backoffice-storage

Uso:
✔ Archivos administrativos
✔ Documentos de usuarios (opcional)
✔ Recursos del panel de administración

================================================================================

⚙️ CONFIGURACIÓN TÉCNICA
================================================================================

- Quarkus GraphQL API
- PostgreSQL como base de datos principal
- Flyway para migraciones
- MinIO compatible S3
- JWT validation (sin proveedor de autenticación interno)

================================================================================

🧠 FLUJO DEL SISTEMA
================================================================================

1. ADMIN accede al panel Backoffice UI
2. Frontend realiza llamadas GraphQL
3. Backoffice-service valida JWT
4. Se ejecuta el caso de uso correspondiente
5. Se realiza operación en PostgreSQL
6. Se devuelve respuesta al frontend

================================================================================

⚠️ REGLAS IMPORTANTES
================================================================================

✔ Este servicio es el único responsable de la gestión de usuarios
✔ No depende de otros microservicios para lógica de usuarios
✔ Acceso restringido exclusivamente a ADMIN
✔ Logging obligatorio en operaciones críticas
✔ Validación estricta de datos de entrada

================================================================================

📊 RESUMEN DEL SERVICIO
================================================================================

El backoffice-service es:

✔ Núcleo administrativo del sistema ASTC
✔ API GraphQL de gestión de usuarios
✔ Servicio independiente y aislado
✔ Controlador central de roles y accesos
✔ Parte crítica del sistema de seguridad

================================================================================
FIN DEL ESPEC
================================================================================