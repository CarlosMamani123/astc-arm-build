================================================================================
🧠 ASSISTANCE SERVICE - ESPECIFICACIÓN DE DOMINIO (ENTERPRISE)
================================================================================

📌 RESUMEN DEL SERVICIO
--------------------------------------------------------------------------------
El servicio de asistencia (assistance-service) es un microservicio de dominio
encargado de gestionar todo el ciclo de vida de la asistencia de usuarios,
ausencias, justificaciones y control de anomalías dentro del sistema ASTC.

Está diseñado con arquitectura empresarial basada en eventos y hexagonal.

Tecnologías:
- Java 21 + Quarkus
- API GraphQL (MicroProfile)
- PostgreSQL (base de datos propia)
- RabbitMQ (eventos)
- Redis (opcional para cache)
- MinIO (almacenamiento de archivos)
- JWT (validación externa desde auth-service)

--------------------------------------------------------------------------------

🏗️ ARQUITECTURA INTERNA (HEXAGONAL)
--------------------------------------------------------------------------------

application/
├── in/                 (puertos de entrada)
├── service/            (casos de uso)

domain/
├── entities/           (entidades del dominio)
├── value-objects/      (objetos inmutables)

infrastructure/
├── controller/graphql  (resolvers GraphQL)
├── persistence         (adaptadores de base de datos)
├── messaging           (RabbitMQ producer/consumer)
├── security            (JWT + AuthContext)

--------------------------------------------------------------------------------

🎯 RESPONSABILIDADES DEL SERVICIO
--------------------------------------------------------------------------------

Este microservicio es responsable de:

✔ Registro de asistencia (check-in / check-out)
✔ Gestión de ausencias
✔ Creación y revisión de justificaciones
✔ Consulta de dashboards de asistencia
✔ Integración con sistema de IA para detección de fraude
✔ Publicación de eventos de dominio en tiempo real

--------------------------------------------------------------------------------

🌐 API GRAPHQL
--------------------------------------------------------------------------------

📍 Endpoint principal:
/graphql

--------------------------------------------------------------------------------
MUTACIONES
--------------------------------------------------------------------------------
- RegisterAttendance  (Registrar entrada de asistencia con foto + GPS)
- CheckOut            (Registrar salida de asistencia)
- SaveJustification   (Guardar justificación como borrador PENDING)
- SubmitJustification (Enviar justificación a revisión → SUBMITTED)
- EditJustification   (Editar justificación en estado PENDING u OBSERVATION)
- DeleteJustification (Eliminar justificación en estado PENDING)
- JustifyAbsence      (Validar si una ausencia puede ser justificada)
- ApproveJustification (PM aprueba justificación → APPROVED)
- RejectJustification  (PM rechaza justificación → REJECTED)
- RequestObservation   (PM solicita correcciones → OBSERVATION)

--------------------------------------------------------------------------------
CONSULTAS
--------------------------------------------------------------------------------
- GetDashboard            (Dashboard team member: totalAttendances, totalAbsences, pendingJustifications)
- GetDashboardPMInternal  (Dashboard interno para PM)
- ListAttendance          (Historial de asistencias del usuario)
- ListAbsences            (Lista de ausencias del usuario)
- ListJustifications      (Lista de justificaciones del usuario)
- GetJustificationDetailPM (Detalle completo con history[])
- GetJustificationStatus  (Estado de una justificación)
- GetTodayAttendance      (Asistencia del día actual: checkIn, checkOut, status)
- ListTeamJustificationsPM (Justificaciones del equipo para PM)

--------------------------------------------------------------------------------

🔐 SEGURIDAD
--------------------------------------------------------------------------------

Autenticación:
- JWT emitido por el servicio auth-service (Better Auth)

Claims del token:
- sub → ID de usuario
- email → correo del usuario
- role → rol del usuario

Reglas de seguridad:
- Todo endpoint requiere JWT válido
- El userId se obtiene desde AuthContext
- Nunca se confía en datos enviados por el cliente

Roles:
- TEAM_MEMBER (usuario principal del sistema)

--------------------------------------------------------------------------------

🗄️ BASE DE DATOS (POSTGRESQL + LIQUIBASE)
--------------------------------------------------------------------------------

Propiedad:
- Base de datos exclusiva del servicio (schema: astc_assistance)
- No existen foreign keys hacia otros microservicios
- Las referencias externas (user_id, project_id) son UUID sin relación directa
- Migraciones gestionadas por Liquibase (contenedor: liquibase-assistance)

--------------------------------------------------------------------------------

TABLAS PRINCIPALES
--------------------------------------------------------------------------------

1. attendances (asistencias)
--------------------------------------------------------------------------------
- id (UUID)
- user_id (UUID externo)
- project_id (UUID externo)
- date (fecha)
- check_in (hora entrada)
- check_out (hora salida)
- status (estado)
- latitude / longitude (ubicación)
- photo_url (evidencia fotográfica)
- created_at / updated_at

--------------------------------------------------------------------------------

2. absences (ausencias)
--------------------------------------------------------------------------------
- id (UUID)
- user_id (UUID externo)
- project_id (UUID externo)
- date (fecha)
- type (tipo de ausencia)
- justified (booleano)

--------------------------------------------------------------------------------

3. justifications (justificaciones)
--------------------------------------------------------------------------------
- id (UUID)
- absence_id (UUID → absences.id)
- user_id (UUID externo)
- description (texto)
- document_url (S3 key en MinIO, ej: document/private/user/{uuid}/justifications/{uuid}.pdf)
- status (PENDING | SUBMITTED | OBSERVATION | APPROVED | REJECTED)
- comment (comentario del revisor)
- submitted_at (fecha envío a revisión)
- reviewed_at (fecha revisión por PM)
- reviewed_by (UUID del PM revisor)
- created_at / updated_at

4. justification_history (historial de cambios)
--------------------------------------------------------------------------------
- id (UUID)
- justification_id (UUID → justifications.id)
- previous_status (estado anterior, null si es creación)
- new_status (nuevo estado)
- comment (comentario del cambio)
- changed_by (UUID del usuario que ejecutó el cambio)
- changed_at (timestamp del cambio)

5. alerts (alertas del sistema)
--------------------------------------------------------------------------------
- id (UUID)
- user_id (UUID externo)
- project_id (UUID externo)
- type (tipo de alerta)
    - FACE_FRAUD
    - FACE_MISMATCH
    - OUT_OF_AREA
    - LATE_ARRIVAL
- status (PENDING | APPROVED | CANCELLED)
- detail (detalle del evento)
- latitude / longitude
- created_at / updated_at

--------------------------------------------------------------------------------

📡 EVENTOS (RABBITMQ)
--------------------------------------------------------------------------------

Exchange:
- assistance.events

Eventos publicados:

- attendance.registered    (cuando se registra check-in)
- attendance.checked_out   (cuando se registra check-out)
- absence.created          (cuando se detecta una falta automáticamente)
- justification.saved      (borrador guardado, status PENDING)
- justification.submitted  (enviado a revisión, status SUBMITTED)
- justification.approved   (aprobado por PM)
- justification.rejected   (rechazado por PM)
- justification.observation (PM solicita correcciones)
- alert.created            (nueva alerta generada)

Objetivo:
- Integración con sistemas externos
- Notificaciones en tiempo real
- Analítica y monitoreo
- Integración con IA

--------------------------------------------------------------------------------

🧠 REGLAS DE NEGOCIO
--------------------------------------------------------------------------------

1. Un usuario solo puede registrar su propia asistencia (userId del JWT)
2. Solo puede existir UNA asistencia por usuario por día (unique constraint: user_id + date)
3. El check-out requiere que exista un check-in previo en el mismo día
4. Si se intenta registrar entrada de nuevo → error "Ya registraste" con opción de reemplazar
5. Solo puede existir una justificación activa por ausencia
6. El rol TEAM_MEMBER es obligatorio para operar en el sistema
7. Toda asistencia fuera de horario o ubicación genera alerta automática
8. El sistema de IA valida rostro antes de aceptar asistencia
9. Las justificaciones requieren 2 pasos: save (PENDING) + submit (SUBMITTED)
10. Solo el PM puede aprobar/rechazar/solicitar observación (rol PROJECT_MANAGER o ADMIN)
11. El historial de justificación (justification_history) es inmutable
12. Las URLs de documentos son presigned (expiran en 60 min) generadas por S3Presigner
13. El presigner usa path-style (no virtual-host) para compatibilidad con MinIO
14. Si una URL ya es externa (http/https) y no es del bucket → se retorna tal cual (sin re-firmar)

--------------------------------------------------------------------------------

📦 ALMACENAMIENTO (MINIO)
--------------------------------------------------------------------------------

Bucket:
- assistance

Configuración:
- S3_ENDPOINT: http://minio:9000 (interno Docker)
- S3_PUBLIC_ENDPOINT: http://localhost:9010 (público para presigned URLs)
- Credenciales: minioadmin / minioadmin
- Región: us-east-1

Estructura de keys:
- Foto de asistencia: private/user/{userId}/assistance/{yyyy/MM}/{uuid}.jpg
- Documento de justificación: document/private/user/{userId}/justifications/{uuid}.pdf

Presigned URLs:
- Generadas con S3Presigner.pathStyleAccessEnabled(true)
- Expiración: 60 minutos
- Formato: http://localhost:9010/assistance/{key}?X-Amz-...

--------------------------------------------------------------------------------

⚡ CACHE (REDIS - OPCIONAL)
--------------------------------------------------------------------------------

Uso:
- Dashboard de asistencia
- Consultas frecuentes
- Optimización de consultas GraphQL

--------------------------------------------------------------------------------

📊 OBSERVABILIDAD
--------------------------------------------------------------------------------

- Logs estructurados
- Trazabilidad distribuida (OpenTelemetry opcional)
- Métricas:
  - tasa de asistencia
  - cantidad de alertas generadas
  - tiempo de revisión de justificaciones
  - detección de fraude

--------------------------------------------------------------------------------

🔁 INTEGRACIONES
--------------------------------------------------------------------------------

Este servicio se integra con:

- auth-service → validación JWT
- AI worker (Python) → detección de fraude facial
- RabbitMQ → sistema de eventos
- PostgreSQL → persistencia de datos
- MinIO → almacenamiento de archivos
- backoffice-service → monitoreo administrativo

--------------------------------------------------------------------------------

🚀 RESUMEN
--------------------------------------------------------------------------------

El assistance-service es:

✔ Un microservicio de dominio
✔ API GraphQL
✔ Sistema basado en eventos
✔ Dueño exclusivo de su base de datos
✔ Integrado con IA para control de asistencia
✔ Sistema empresarial de auditoría y control

================================================================================
FIN DEL ESPEC
================================================================================