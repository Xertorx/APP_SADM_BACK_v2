package ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto;

import ucentral.edu.sadm.seleccion.dominio.tipoRespuestaSeleccion;

import java.time.Instant;
import java.util.List;

/**
 * Etapa 1 (vista del adoptante): preguntas configuradas por el dador con las respuestas
 * guardadas. No expone el puntaje de cada pregunta para no condicionar las respuestas.
 */
public record formularioPostulacionEntidad(
        Long postulacionId,
        Long mascotaId,
        String mascotaNombre,
        String estado,
        String estadoEtiqueta,
        boolean editable,
        boolean enviado,
        Instant fechaEnvio,
        int totalPreguntas,
        int respondidas,
        String comentarioEvaluacion,
        List<pregunta> preguntas
) {
    public record pregunta(
            Long id,
            String texto,
            tipoRespuestaSeleccion tipoRespuesta,
            List<String> opciones,
            String respuesta,
            List<String> respuestaOpciones
    ) {
    }
}
