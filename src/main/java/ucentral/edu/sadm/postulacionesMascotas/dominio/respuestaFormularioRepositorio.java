package ucentral.edu.sadm.postulacionesMascotas.dominio;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class respuestaFormularioRepositorio implements PanacheRepository<respuestaFormulario> {

    public List<respuestaFormulario> listarPorPostulacion(Long postulacionId) {
        return list("postulacion.id", postulacionId);
    }
}
