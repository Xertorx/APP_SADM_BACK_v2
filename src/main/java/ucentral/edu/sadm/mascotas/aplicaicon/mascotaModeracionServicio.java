package ucentral.edu.sadm.mascotas.aplicaicon;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import ucentral.edu.sadm.common.infraestructure.BusinessException;
import ucentral.edu.sadm.mascotas.dominio.exception.MascotaNoEncontradaException;
import ucentral.edu.sadm.mascotas.dominio.mascota;
import ucentral.edu.sadm.mascotas.dominio.mascotaRepositorio;
import ucentral.edu.sadm.mascotas.infraestructura.dto.mascotaDetalleEntidad;
import ucentral.edu.sadm.usuarios.dominio.usuario;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HU26: el administrador revisa las mascotas enviadas a revisión (PENDIENTE_APROBACION).
 * Aprobar las deja DISPONIBLE (ya se puede postular); rechazar las deja RECHAZADA con motivo.
 */
@ApplicationScoped
public class mascotaModeracionServicio {

    private static final Logger LOG = Logger.getLogger(mascotaModeracionServicio.class);
    private static final String PENDIENTE = "PENDIENTE_APROBACION";
    private static final int MAX_MOTIVO = 500;

    @Inject
    mascotaRepositorio mascotaRepositorio;

    @Inject
    mascotaServicio mascotaServicio;

    @Transactional
    public List<Map<String, Object>> listarPendientes() {
        return mascotaRepositorio.list("estado = ?1 order by id asc", PENDIENTE).stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional
    public void aprobar(Long id) {
        mascota mascota = buscarPendiente(id);
        mascota.estado = "DISPONIBLE";
        mascota.motivo_rechazo = null;
        if (mascota.fecha_publicacion == null) {
            mascota.fecha_publicacion = new Date();
        }
        LOG.infof("Mascota %d aprobada por el administrador", id);
    }

    @Transactional
    public void rechazar(Long id, String motivo) {
        String limpio = motivo == null ? "" : motivo.trim();
        if (limpio.isEmpty()) {
            throw new BusinessException(400, "MOTIVO_OBLIGATORIO", "El motivo del rechazo es obligatorio",
                    "Indica por qué se rechaza la publicación");
        }
        if (limpio.length() > MAX_MOTIVO) {
            throw new BusinessException(400, "MOTIVO_INVALIDO", "El motivo no puede superar los 500 caracteres",
                    "Longitud máxima del motivo: " + MAX_MOTIVO);
        }
        mascota mascota = buscarPendiente(id);
        mascota.estado = "RECHAZADA";
        mascota.motivo_rechazo = limpio;
        LOG.infof("Mascota %d rechazada por el administrador", id);
    }

    private mascota buscarPendiente(Long id) {
        mascota mascota = mascotaRepositorio.findById(id);
        if (mascota == null || "ELIMINADA".equals(mascota.estado)) {
            throw new MascotaNoEncontradaException();
        }
        if (!PENDIENTE.equals(mascota.estado)) {
            throw new BusinessException(409, "MASCOTA_NO_PENDIENTE",
                    "La publicación ya no está pendiente de revisión",
                    "Solo se aprueban o rechazan mascotas en estado " + PENDIENTE);
        }
        return mascota;
    }

    /** Mismos campos que el detalle público, más el dador y las coordenadas. */
    private Map<String, Object> convertir(mascota mascota) {
        mascotaDetalleEntidad d = mascotaServicio.convertirDetalle(mascota);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", d.id());
        datos.put("nombre", d.nombre());
        datos.put("especie", d.especie());
        datos.put("raza", d.raza());
        datos.put("edad", d.edad());
        datos.put("unidadEdad", d.unidadEdad());
        datos.put("tamano", d.tamano());
        datos.put("sexo", d.sexo());
        datos.put("temperamento", d.temperamento());
        datos.put("historialSalud", d.historialSalud());
        datos.put("ubicacion", d.ubicacion());
        datos.put("estado", d.estado());
        datos.put("fechaPublicacion", d.fechaPublicacion());
        datos.put("idDador", d.idDador());
        datos.put("fotosIds", d.fotosIds());
        datos.put("latitud", mascota.latitud);
        datos.put("longitud", mascota.longitud);
        usuario dador = mascota.dador;
        if (dador != null) {
            Map<String, Object> datosDador = new LinkedHashMap<>();
            datosDador.put("id", dador.id);
            datosDador.put("nombre", dador.nombre);
            datosDador.put("apellido", dador.apellido);
            datosDador.put("correo", dador.correo);
            datosDador.put("rol", dador.rol == null ? null : dador.rol.name());
            datosDador.put("estado", dador.estado);
            datos.put("dador", datosDador);
        }
        return datos;
    }
}
