package ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto;

import java.util.List;

/**
 * Etapa 1: calificación del dador. Debe incluir un puntaje (0..máximo de la pregunta) por
 * cada pregunta respondida. {@code decision}: APROBAR (pasa a la etapa 2) o DESCARTAR
 * (requiere comentario).
 */
public record evaluacionFormularioSolicitud(
        List<calificacion> calificaciones,
        String decision,
        String comentario
) {
    public record calificacion(Long preguntaId, Integer puntaje) {
    }
}
