package ucentral.edu.sadm.administracion.infraestructura.dto;

/**
 * Cuerpo de DELETE /api/admin/mascotas/{id}: {"motivo": "..."}.
 * El motivo (obligatorio, máx. 300) se valida en adminMascotaServicio para devolver un único
 * mensaje claro, igual que inhabilitarSolicitud de HU28.
 */
public record rechazoPublicacionSolicitud(String motivo) {
}
