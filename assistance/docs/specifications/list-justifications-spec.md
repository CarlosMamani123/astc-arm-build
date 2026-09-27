```md
## ListJustifications

### Descripción
Obtiene la lista de justificaciones del usuario.

### Pantalla / Módulo
Módulo de justificaciones

### Rol de usuario
Team Member

### Inputs
- page (Int)
- size (Int)
- status (String, opcional)
- fromDate (String, opcional)
- toDate (String, opcional)

### Campos de respuesta
- items
- page
- size
- total

### Reglas de negocio
- Solo se muestran justificaciones del usuario

### Estado vacío
- Lista vacía

### Manejo de errores
- Error si no autenticado

### Notas para frontend
- Mostrar estado (pendiente/aprobado)

### Request
```graphql
query {
  ListJustifications(
    page: 0
    size: 10
    status: null
    fromDate: null
    toDate: null
  ) {
    items {
      id
      description
      status
    }
    page
    size
    total
  }
}