package ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto;

import ucentral.edu.sadm.seleccion.dominio.tipoRespuestaSeleccion;

import java.time.Instant;
import java.util.List;

/** Etapa 1 (vista del dador): respuestas del adoptante con el puntaje máximo de cada pregunta. */
public record evaluacionFormularioEntidad(
        Long postulacionId,
        Long mascotaId,
        String mascotaNombre,
        Long adoptanteId,
        String adoptanteNombre,
        String estado,
        String estadoEtiqueta,
        boolean evaluable,
        Instant fechaEnvio,
        Instant fechaEvaluacion,
        int puntajeMaximoEtapa,
        int puntajeObtenido,
        String comentario,
        List<pregunta> preguntas
) {
    public record pregunta(
            Long id,
            String texto,
            tipoRespuestaSeleccion tipoRespuesta,
            List<String> opciones,
            int puntajeMaximo,
            String respuesta,
            List<String> respuestaOpciones,
            Integer puntajeAsignado
    ) {
    }
}
