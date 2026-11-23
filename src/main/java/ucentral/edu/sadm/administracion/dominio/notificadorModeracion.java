package ucentral.edu.sadm.administracion.dominio;

/**
 * HU26: punto de enchufe para avisar al dador del resultado de la moderación de su publicación.
 * El servicio depende solo de esta interfaz (mismo patrón que notificadorCancelacion de HU07).
 */
public interface notificadorModeracion {

    void notificar(String correoDador, String nombreMascota, String mensaje);
}
