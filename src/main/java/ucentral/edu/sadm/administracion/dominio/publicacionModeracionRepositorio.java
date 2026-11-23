package ucentral.edu.sadm.administracion.dominio;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

/** HU26: acceso a la tabla publicacion_moderacion. */
@ApplicationScoped
public class publicacionModeracionRepositorio implements PanacheRepository<publicacionModeracion> {
}
