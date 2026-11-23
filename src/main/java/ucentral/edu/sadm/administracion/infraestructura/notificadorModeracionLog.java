package ucentral.edu.sadm.administracion.infraestructura;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import ucentral.edu.sadm.administracion.dominio.notificadorModeracion;

// TODO HU42: enviar correo o notificación real al dador (hoy solo se registra en el log)
@ApplicationScoped
public class notificadorModeracionLog implements notificadorModeracion {
    private static final Logger LOG = Logger.getLogger(notificadorModeracionLog.class);

    @Override
    public void notificar(String correoDador, String nombreMascota, String mensaje) {
        LOG.infof("Notificación pendiente para %s (mascota %s): %s", correoDador, nombreMascota, mensaje);
    }
}
