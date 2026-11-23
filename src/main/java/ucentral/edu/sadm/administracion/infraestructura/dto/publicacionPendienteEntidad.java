package ucentral.edu.sadm.administracion.infraestructura.dto;

import java.util.Date;
import java.util.List;

/**
 * HU26: elemento de GET /api/admin/mascotas/pendientes.
 * Mismos campos que mascotaDetalleEntidad (el front lo lee con MascotaDetalle.fromJson) más las
 * coordenadas y un resumen del dador. Nunca incluye passwordHash ni identificacion.
 */
public record publicacionPendienteEntidad(
        Long id,
        String nombre,
        String especie,
        String raza,
        Integer edad,
        String unidadEdad,
        String tamano,
        String sexo,
        List<String> temperamento,
        String historialSalud,
        String ubicacion,
        String estado,
        Date fechaPublicacion,
        Long idDador,
        List<Long> fotosIds,
        Double latitud,
        Double longitud,
        Dador dador
) {
    public record Dador(Long id, String nombre, String apellido, String correo, String rol, String estado) {
    }
}
