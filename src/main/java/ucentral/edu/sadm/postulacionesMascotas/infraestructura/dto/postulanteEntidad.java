package ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto;

import java.time.Instant;

/** Fila de la lista de postulantes de una mascota (vista del dador). Sin datos de contacto. */
public record postulanteEntidad(
        Long postulacionId,
        Long adoptanteId,
        String adoptanteNombre,
        Instant fechaPostulacion,
        String estado,
        String estadoEtiqueta,
        int etapaActual,
        int puntaje,
        boolean formularioEnviado,
        Instant fechaEnvioFormulario,
        boolean pendienteEvaluacion
) {
}
