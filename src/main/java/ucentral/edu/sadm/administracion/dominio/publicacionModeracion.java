package ucentral.edu.sadm.administracion.dominio;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import ucentral.edu.sadm.mascotas.dominio.mascota;
import ucentral.edu.sadm.usuarios.dominio.usuario;

import java.time.Instant;

/**
 * HU26: historial de moderación de publicaciones (aprobar / editar / rechazar).
 * Tabla propia: no se agregan columnas a mascota; solo se la referencia con FK.
 */
@Entity
@Table(name = "publicacion_moderacion")
@NoArgsConstructor
public class publicacionModeracion extends PanacheEntity {

    public static final String APROBAR = "APROBAR";
    public static final String EDITAR = "EDITAR";
    public static final String RECHAZAR = "RECHAZAR";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mascota_id", nullable = false)
    public mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    public usuario admin;

    @Column(nullable = false, length = 15)
    public String accion;

    /** Solo se llena al RECHAZAR. */
    @Column(length = 300)
    public String motivo;

    @Column(nullable = false)
    public Instant fecha;

    @PrePersist
    void alCrear() {
        if (fecha == null) {
            fecha = Instant.now();
        }
    }
}
