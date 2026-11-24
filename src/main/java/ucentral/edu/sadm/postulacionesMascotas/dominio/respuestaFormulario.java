package ucentral.edu.sadm.postulacionesMascotas.dominio;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ucentral.edu.sadm.seleccion.dominio.preguntaSeleccion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 1: respuesta del adoptante a una pregunta configurada por el dador.
 * TEXTO, SI_NO y SELECCION_UNICA usan {@link #valor}; SELECCION_MULTIPLE usa {@link #opciones}.
 */
@Entity
@Table(
        name = "respuesta_formulario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_respuesta_formulario_postulacion_pregunta",
                columnNames = {"postulacion_id", "pregunta_id"}
        )
)
public class respuestaFormulario extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "postulacion_id", nullable = false)
    public postulacion postulacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    public preguntaSeleccion pregunta;

    @Column(length = 2000)
    public String valor;

    @ElementCollection
    @CollectionTable(name = "respuesta_formulario_opcion", joinColumns = @JoinColumn(name = "respuesta_id"))
    @OrderColumn(name = "orden")
    @Column(name = "opcion", nullable = false, length = 200)
    public List<String> opciones = new ArrayList<>();

    /** Puntaje que asignó el dador al evaluar (null mientras no se evalúe). */
    @Column(name = "puntaje_asignado")
    public Integer puntajeAsignado;

    @Column(name = "created_at", nullable = false, updatable = false)
    public Instant created_at;

    @Column(name = "updated_at", nullable = false)
    public Instant updated_at;

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        created_at = ahora;
        updated_at = ahora;
    }

    @PreUpdate
    void alActualizar() {
        updated_at = Instant.now();
    }
}
