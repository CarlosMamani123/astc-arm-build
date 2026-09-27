## ListAttendance

### Descripción
Obtiene la lista de asistencias del usuario con soporte de paginación y filtros opcionales.

### Pantalla / Módulo
Historial de asistencias

### Rol de usuario
Team Member

### Inputs
- page (Int): Número de página
- size (Int): Cantidad de registros por página
- projectId (UUID, opcional): Filtrar por proyecto
- fromDate (String, opcional): Fecha de inicio
- toDate (String, opcional): Fecha de fin
- status (String, opcional): Estado de la asistencia

### Campos de respuesta
- items (Lista de asistencias)
- page (Int)
- size (Int)
- total (Long)

### Reglas de negocio
- Solo se listan asistencias del usuario autenticado
- Se pueden aplicar filtros
- Los resultados están paginados

### Estado vacío
- Lista vacía si no hay datos
- Mostrar mensaje “No hay asistencias”

### Manejo de errores
- Error si el usuario no está autenticado
- Error si parámetros inválidos
- Error genérico del backend

### Notas para frontend
- Mostrar en tabla
- Implementar filtros
- Implementar paginación

### Request
```graphql
query {
  ListAttendance(
    page: 0
    size: 10
    projectId: null
    fromDate: "2026-01-01"
    toDate: "2026-12-31"
    status: null
  ) {
    items {
      id
      userId
      projectId
      date
      checkIn
      checkOut
      status
    }
    page
    size
    total
  }
}