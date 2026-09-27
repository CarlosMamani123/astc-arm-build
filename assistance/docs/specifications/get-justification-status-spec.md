

## GetJustificationStatus

### Descripción
Obtiene el estado actual de una justificación específica del usuario.

### Pantalla / Módulo
Detalle de justificación

### Rol de usuario
Team Member

### Inputs
- justificationId (UUID): Identificador de la justificación

### Campos de respuesta
- justificationId (UUID): Identificador de la justificación
- status (String): Estado actual de la justificación

### Reglas de negocio
- La justificación debe pertenecer al usuario autenticado
- Solo se retorna el estado actual de la justificación

### Estado vacío
- Si no existe la justificación, retornar null o error

### Manejo de errores
- Retorna error si el usuario no está autenticado
- Retorna error si la justificación no existe
- Retorna error si la justificación no pertenece al usuario
- Retorna error genérico si ocurre una falla en el backend

### Notas para frontend
- Usar para actualizar el estado en tiempo real
- Mostrar el estado de forma clara (pendiente, aprobado, rechazado)

### Request
```graphql
query {
  GetJustificationStatus(
    justificationId: "44444444-4444-4444-4444-444444444444"
  ) {
    justificationId
    status
  }
}