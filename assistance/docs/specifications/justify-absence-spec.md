## JustifyAbsence

### Descripción
Valida si una inasistencia específica puede ser justificada por el usuario autenticado.

### Pantalla / Módulo
Módulo de inasistencias / Inicio de justificación

### Rol de usuario
Team Member

### Inputs
- absenceId (UUID): Identificador de la inasistencia

### Campos de respuesta
- absenceId (UUID): Identificador de la inasistencia evaluada
- valid (Boolean): Indica si la inasistencia puede ser justificada

### Reglas de negocio
- La inasistencia debe pertenecer al usuario autenticado
- Solo se puede justificar una inasistencia válida según las reglas del sistema
- La operación solo valida el flujo, no registra aún la justificación

### Estado vacío
- No aplica, ya que es una validación puntual

### Manejo de errores
- Retorna error si el usuario no está autenticado
- Retorna error si la inasistencia no existe
- Retorna error si la inasistencia no pertenece al usuario
- Retorna error genérico si ocurre una falla en el backend

### Notas para frontend
- Usar antes de abrir el formulario final de justificación
- Si `valid` es false, mostrar mensaje al usuario y no continuar
- Puede servir como paso previo antes de `SaveJustification`

### Request
```graphql
mutation {
  JustifyAbsence(
    input: {
      absenceId: "33333333-3333-3333-3333-333333333333"
    }
  ) {
    absenceId
    valid
  }
}