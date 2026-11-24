package ucentral.edu.sadm.seleccion.infraestructura.dto;

import java.util.List;

/**
 * Preguntas del formulario (etapa 1) de una mascota ya publicada.
 * {@code editable} es false cuando algún adoptante ya envió el formulario; {@code motivo} lo explica.
 */
public record preguntasFormularioEntidad(
        Long mascotaId,
        String estadoMascota,
        boolean editable,
        String motivo,
        int puntajeMaximoEtapa,
        List<configuracionSeleccionEntidad.pregunta> preguntas
) {
}
