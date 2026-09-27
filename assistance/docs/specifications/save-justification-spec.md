

## SaveJustification

### Descripción
Guarda una justificación asociada a una inasistencia del usuario autenticado y registra la solicitud para revisión.

### Pantalla / Módulo
Módulo de justificaciones / Registro de justificación

### Rol de usuario
Team Member

### Inputs
- absenceId (UUID): Identificador de la inasistencia
- description (String): Descripción o motivo de la justificación
- documentUrl (String, opcional): URL o referencia del documento adjunto

### Campos de respuesta
- id (UUID): Identificador de la justificación registrada
- status (String): Estado inicial de la justificación
- submittedAt (DateTime): Fecha y hora de envío

### Reglas de negocio
- La justificación debe estar asociada a una inasistencia válida del usuario
- La descripción representa el sustento principal de la justificación
- El estado inicial debe registrarse como pendiente
- Debe generarse una notificación mediante `SendNotificationByQueue`

### Estado vacío
- No aplica, ya que es una operación de registro

### Manejo de errores
- Retorna error si el usuario no está autenticado
- Retorna error si la inasistencia no existe
- Retorna error si la inasistencia no pertenece al usuario
- Retorna error si faltan campos obligatorios
- Retorna error genérico si ocurre una falla en el backend

### Notas para frontend
- Usar desde el formulario de justificación
- Mostrar confirmación después del registro exitoso
- Mostrar el estado retornado al usuario
- Permitir adjuntar o asociar documento antes de enviar

### Request
```graphql
mutation {
  SaveJustification(
    input: {
      absenceId: "33333333-3333-3333-3333-333333333333"
      description: "Falta por enfermedad"
      documentUrl: "certificado.pdf"
    }
  ) {
    id
    status
    submittedAt
  }
}