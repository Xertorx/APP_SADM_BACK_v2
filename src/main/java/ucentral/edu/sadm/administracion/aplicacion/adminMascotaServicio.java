package ucentral.edu.sadm.administracion.aplicacion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import ucentral.edu.sadm.administracion.dominio.notificadorModeracion;
import ucentral.edu.sadm.administracion.dominio.publicacionModeracion;
import ucentral.edu.sadm.administracion.dominio.publicacionModeracionRepositorio;
import ucentral.edu.sadm.administracion.infraestructura.dto.publicacionPendienteEntidad;
import ucentral.edu.sadm.common.infraestructure.BusinessException;
import ucentral.edu.sadm.mascotas.dominio.distanciaGeografica;
import ucentral.edu.sadm.mascotas.dominio.exception.FotosInvalidasException;
import ucentral.edu.sadm.mascotas.dominio.fotoMascota;
import ucentral.edu.sadm.mascotas.dominio.fotoMascotaRepositorio;
import ucentral.edu.sadm.mascotas.dominio.mascota;
import ucentral.edu.sadm.mascotas.dominio.mascotaRepositorio;
import ucentral.edu.sadm.mascotas.infraestructura.dto.mascotaEntidad;
import ucentral.edu.sadm.usuarios.dominio.usuario;
import ucentral.edu.sadm.usuarios.dominio.usuarioRepositorio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * HU26: moderación de publicaciones de mascotas (solo ADMINISTRADOR).
 * Solo actúa sobre mascotas en PENDIENTE_APROBACION; cada acción queda en publicacion_moderacion
 * y se avisa al dador por notificadorModeracion. Se usan sin modificar las entidades/repositorios
 * de mascotas (HU05/HU06) y usuarios (HU01).
 */
@ApplicationScoped
public class adminMascotaServicio {
    private static final Logger LOG = Logger.getLogger(adminMascotaServicio.class);

    static final String PENDIENTE = "PENDIENTE_APROBACION";
    static final String DISPONIBLE = "DISPONIBLE";
    static final String ELIMINADA = "ELIMINADA";
    static final int MOTIVO_MAX = 300;

    // Mismas reglas de fotos que HU05/HU06 (mascotaServicio.validarFotos es private y no se modifica).
    private static final int MAX_FOTOS = 6;
    private static final long MAX_TAMANO_FOTO = 5L * 1024 * 1024;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png");

    @Inject
    mascotaRepositorio mascotaRepositorio;
    @Inject
    fotoMascotaRepositorio fotoMascotaRepositorio;
    @Inject
    usuarioRepositorio usuarioRepositorio;
    @Inject
    publicacionModeracionRepositorio moderacionRepositorio;
    @Inject
    notificadorModeracion notificadorModeracion;

    /** Pendientes de moderación, las más antiguas primero. Sin @Transactional: devuelve records. */
    public List<publicacionPendienteEntidad> listarPendientes() {
        return mascotaRepositorio.list("estado = ?1 order by fecha_publicacion asc, id asc", PENDIENTE)
                .stream()
                .map(this::aPendiente)
                .toList();
    }

    @Transactional
    public void aprobar(Long id, String correoAdmin) {
        usuario admin = buscarAdmin(correoAdmin);
        mascota mascota = buscarPendiente(id);
        mascota.estado = DISPONIBLE;
        registrar(mascota, admin, publicacionModeracion.APROBAR, null);
        notificar(mascota, "Tu publicación fue aprobada y ya es visible en el listado");
    }

    /** Borrado lógico igual que HU07. Una mascota PENDIENTE no tiene postulaciones que cancelar. */
    @Transactional
    public void rechazar(Long id, String motivo, String correoAdmin) {
        usuario admin = buscarAdmin(correoAdmin);
        String motivoLimpio = validarMotivo(motivo);
        mascota mascota = buscarPendiente(id);
        mascota.estado = ELIMINADA;
        registrar(mascota, admin, publicacionModeracion.RECHAZAR, motivoLimpio);
        notificar(mascota, "Tu publicación fue eliminada: " + motivoLimpio);
    }

    /**
     * Mismo contrato que PUT /api/mascotas/{id} (HU06) pero sin chequeo de dueño.
     * Fotos opcionales: si llegan reemplazan a las actuales; si no, se conservan.
     * Coordenadas: las dos → se actualizan; ninguna → se conservan; solo una → 400.
     * La publicación sigue en PENDIENTE_APROBACION.
     */
    @Transactional
    public void editar(Long id, mascotaEntidad datos, List<FileUpload> fotos, String correoAdmin) {
        usuario admin = buscarAdmin(correoAdmin);
        mascota mascota = buscarPendiente(id);
        boolean hayFotos = fotos != null && !fotos.isEmpty();
        if (hayFotos) {
            validarFotos(fotos);
        }
        if ((datos.latitud() == null) != (datos.longitud() == null)) {
            throw new BusinessException(400, "Debes enviar latitud y longitud");
        }

        mascota.nombre = datos.nombre();
        mascota.especie = datos.especie();
        mascota.raza = datos.raza();
        mascota.edad = datos.edad();
        mascota.unidad_edad = datos.unidadEdad();
        mascota.tamano = datos.tamano();
        mascota.sexo = datos.sexo();
        mascota.historial_salud = datos.historialSalud();
        mascota.ubicacion = datos.ubicacion();
        mascota.temperamento = datos.temperamento();
        if (datos.latitud() != null) {
            mascota.latitud = distanciaGeografica.redondear(datos.latitud());
            mascota.longitud = distanciaGeografica.redondear(datos.longitud());
        }

        if (hayFotos) {
            fotoMascotaRepositorio.eliminarPorMascota(id);
            int orden = 0;
            for (FileUpload foto : fotos) {
                fotoMascotaRepositorio.persist(leerFoto(mascota, foto, orden++));
            }
        }

        registrar(mascota, admin, publicacionModeracion.EDITAR, null);
        notificar(mascota, "Tu publicación fue editada por el administrador");
    }

    // ---- privados ----

    private usuario buscarAdmin(String correoAdmin) {
        usuario admin = usuarioRepositorio.findByEmail(correoAdmin);
        if (admin == null) {
            throw new BusinessException(401, "Sesión no válida");
        }
        return admin;
    }

    /**
     * Bloquea la fila (SELECT ... FOR UPDATE) para que dos administradores no moderen la misma
     * publicación a la vez: el segundo espera y luego recibe 409 porque el estado ya cambió.
     */
    private mascota buscarPendiente(Long id) {
        mascota mascota = id == null ? null : mascotaRepositorio.findById(id, LockModeType.PESSIMISTIC_WRITE);
        if (mascota == null) {
            throw new BusinessException(404, "La publicación no existe");
        }
        if (!PENDIENTE.equals(mascota.estado)) {
            throw new BusinessException(409,
                    "El estado de la publicación cambió. Actualiza la lista e inténtalo de nuevo.");
        }
        return mascota;
    }

    private String validarMotivo(String motivo) {
        String limpio = motivo == null ? "" : motivo.trim();
        if (limpio.isEmpty()) {
            throw new BusinessException(400, "El motivo es obligatorio");
        }
        if (limpio.length() > MOTIVO_MAX) {
            throw new BusinessException(400, "El motivo no puede superar los " + MOTIVO_MAX + " caracteres");
        }
        return limpio;
    }

    private void registrar(mascota mascota, usuario admin, String accion, String motivo) {
        publicacionModeracion m = new publicacionModeracion();
        m.mascota = mascota;
        m.admin = admin;
        m.accion = accion;
        m.motivo = motivo;
        moderacionRepositorio.persist(m);
        LOG.infof("Moderación %s de la mascota %d por el admin %d", accion, mascota.id, admin.id);
    }

    private void notificar(mascota mascota, String mensaje) {
        if (mascota.dador != null) {
            notificadorModeracion.notificar(mascota.dador.correo, mascota.nombre, mensaje);
        }
    }

    private publicacionPendienteEntidad aPendiente(mascota m) {
        List<Long> fotosIds = fotoMascotaRepositorio.listarPorMascota(m.id).stream()
                .map(f -> f.id)
                .toList();
        usuario d = m.dador;
        publicacionPendienteEntidad.Dador dador = d == null ? null : new publicacionPendienteEntidad.Dador(
                d.id, d.nombre, d.apellido, d.correo, d.rol == null ? null : d.rol.name(), d.estado);
        return new publicacionPendienteEntidad(
                m.id,
                m.nombre,
                m.especie,
                m.raza,
                m.edad,
                m.unidad_edad,
                m.tamano,
                m.sexo,
                // temperamento es lazy: se copia aquí para no serializar la colección de Hibernate.
                m.temperamento == null ? List.of() : List.copyOf(m.temperamento),
                m.historial_salud,
                m.ubicacion,
                m.estado,
                m.fecha_publicacion,
                d == null ? null : d.id,
                fotosIds,
                m.latitud,
                m.longitud,
                dador
        );
    }

    private void validarFotos(List<FileUpload> fotos) {
        if (fotos.size() > MAX_FOTOS) {
            throw new FotosInvalidasException("No puedes publicar más de " + MAX_FOTOS + " fotos");
        }
        for (FileUpload foto : fotos) {
            String tipo = foto.contentType();
            if (tipo == null || !TIPOS_PERMITIDOS.contains(tipo)) {
                throw new FotosInvalidasException("Las fotos deben ser JPG o PNG");
            }
            if (foto.size() > MAX_TAMANO_FOTO) {
                throw new FotosInvalidasException("Cada foto debe pesar máximo 5 MB");
            }
        }
    }

    private fotoMascota leerFoto(mascota mascota, FileUpload foto, int orden) {
        try {
            byte[] bytes = Files.readAllBytes(foto.uploadedFile());
            return new fotoMascota(mascota, foto.fileName(), foto.contentType(), foto.size(), bytes, orden);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la foto " + foto.fileName(), e);
        }
    }
}
