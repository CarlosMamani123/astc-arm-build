
## RegisterAttendance

### Descripción
Registra una asistencia del usuario.

### Pantalla / Módulo
Registro de asistencia

### Rol de usuario
Team Member

### Inputs
- projectId (UUID)
- latitude (Float)
- longitude (Float)
- photoUrl (String)

### Campos de respuesta
- id
- userId
- date
- checkIn
- status

### Reglas de negocio
- Se registra con el usuario autenticado
- Hora generada por el sistema

### Estado vacío
- No aplica

### Manejo de errores
- Error si datos inválidos

### Notas para frontend
- Mostrar confirmación

### Request
```graphql
mutation {
  RegisterAttendance(
    input: {
      projectId: "2222"
      latitude: -12
      longitude: -77
      photoUrl: "img.jpg"
    }
  ) {
    id
    status
  }
}