package ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto;

import java.util.List;

/**
 * Etapa 1: respuestas del adoptante. {@code valor} se usa para TEXTO, SI_NO ("SI"/"NO") y
 * SELECCION_UNICA; {@code opciones} para SELECCION_MULTIPLE. Una respuesta vacía borra la
 * guardada (solo permitido en el borrador).
 */
public record formularioRespuestasSolicitud(List<respuesta> respuestas) {

    public record respuesta(Long preguntaId, String valor, List<String> opciones) {
    }
}
