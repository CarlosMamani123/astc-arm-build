## ListAbsences

### Descripción
Obtiene la lista de inasistencias del usuario con filtros y paginación.

### Pantalla / Módulo
Historial de inasistencias

### Rol de usuario
Team Member

### Inputs
- page (Int): Número de página
- size (Int): Cantidad de registros por página
- projectId (UUID, opcional): Filtrar por proyecto
- fromDate (String, opcional): Fecha de inicio
- toDate (String, opcional): Fecha de fin
- type (String, opcional): Tipo de inasistencia
- justified (Boolean, opcional): Estado de justificación

### Campos de respuesta
- items (Lista de inasistencias)
- page (Int)
- size (Int)
- total (Long)

### Reglas de negocio
- Solo se listan inasistencias del usuario autenticado
- Permite aplicar filtros por tipo, fecha y estado de justificación
- Los resultados están paginados

### Estado vacío
- Lista vacía si no hay datos
- Mostrar mensaje “No hay inasistencias registradas”

### Manejo de errores
- Retorna error si el usuario no está autenticado
- Retorna error si los parámetros son inválidos
- Retorna error genérico si ocurre una falla en el backend

### Notas para frontend
- Mostrar en tabla o lista
- Permitir filtrar por justificadas / no justificadas
- Mostrar estado visual (badge o color)

### Request
```graphql
query {
  ListAbsences(
    page: 0
    size: 10
    projectId: null
    fromDate: "2026-01-01"
    toDate: "2026-12-31"
    type: null
    justified: null
  ) {
    items {
      id
      userId
      projectId
      date
      type
      justified
    }
    page
    size
    total
  }
}