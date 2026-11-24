package ucentral.edu.sadm.postulacionesMascotas.dominio;

import java.util.List;

public enum estadoPostulacion {
    /** Etapa 1: el adoptante debe diligenciar el formulario de preguntas. */
    ETAPA_1_EN_CURSO("Etapa 1 en curso"),
    /** Etapa 1: el adoptante envió el formulario y el dador debe calificarlo. */
    ETAPA_1_EN_EVALUACION("Etapa 1 en evaluación"),
    /** El dador aprobó la etapa 1; la postulación pasa a la etapa 2 (documentos). */
    ETAPA_2_EN_CURSO("Etapa 2 en curso"),
    /** El dador descartó la postulación al evaluar el formulario. */
    DESCARTADA_ETAPA_1("Descartada en la etapa 1"),
    CANCELADA_POR_DADOR("Cancelada por el dador");

    /** Estados en los que la postulación sigue viva dentro del proceso. */
    public static final List<estadoPostulacion> ACTIVOS =
            List.of(ETAPA_1_EN_CURSO, ETAPA_1_EN_EVALUACION, ETAPA_2_EN_CURSO);

    private final String etiqueta;

    estadoPostulacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public boolean esActivo() {
        return ACTIVOS.contains(this);
    }
}
