## GetDashboard

### Descripción
Obtiene un resumen de la información de asistencia del usuario dentro de un rango de fechas. Proporciona métricas agregadas utilizadas en la visualización del dashboard.

### Pantalla / Módulo
Dashboard (vista principal del Team Member)

### Rol de usuario
Team Member

### Inputs
- fromDate (String, opcional): Fecha de inicio para filtrar los datos
- toDate (String, opcional): Fecha de fin para filtrar los datos

### Campos de respuesta
- totalAttendances (Int): Total de asistencias registradas
- totalAbsences (Int): Total de inasistencias registradas
- pendingJustifications (Int): Cantidad de justificaciones pendientes de revisión

### Reglas de negocio
- Los datos se filtran según el usuario autenticado
- Si se envían fechas, los resultados deben estar dentro del rango
- Solo se consideran registros pertenecientes al usuario
- Las justificaciones pendientes son aquellas que no han sido aprobadas ni rechazadas

### Estado vacío
- Si no existen datos, todos los valores retornan en 0
- El dashboard debe renderizarse correctamente sin errores

### Manejo de errores
- Retorna error si el usuario no está autenticado
- Retorna error si el formato de fechas es inválido
- Retorna error genérico si ocurre una falla en el backend

### Notas para frontend
- Usar esta query para poblar las tarjetas del dashboard
- Mostrar los valores como métricas (cards o widgets)
- Implementar estado de carga mientras se obtiene la data
- Manejar correctamente valores en 0 sin romper la UI

### Request
```graphql
query {
  GetDashboard(fromDate: "2026-01-01", toDate: "2026-12-31") {
    totalAttendances
    totalAbsences
    pendingJustifications
  }
}