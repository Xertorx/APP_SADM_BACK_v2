package ucentral.edu.sadm.administracion.infraestructura;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import ucentral.edu.sadm.common.infraestructure.ResponseApi;
import ucentral.edu.sadm.mascotas.aplicaicon.mascotaModeracionServicio;

/** HU26: moderación de publicaciones de mascotas por el administrador. */
@Path("/api/admin/mascotas")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
public class adminMascotaRecursos {

    @Inject
    mascotaModeracionServicio servicio;

    @GET
    @Path("/pendientes")
    @Operation(summary = "Listar las mascotas enviadas a revisión (PENDIENTE_APROBACION)")
    public Response pendientes() {
        return Response.ok(new ResponseApi<>(200, "Publicaciones pendientes", servicio.listarPendientes())).build();
    }

    @POST
    @Path("/{id}/aprobar")
    @Operation(summary = "Aprobar una publicación: la mascota pasa a DISPONIBLE y recibe postulaciones")
    public Response aprobar(@PathParam("id") Long id) {
        servicio.aprobar(id);
        return Response.ok(new ResponseApi<>(200, "Publicación aprobada correctamente")).build();
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Rechazar una publicación con motivo obligatorio: la mascota pasa a RECHAZADA")
    public Response rechazar(@PathParam("id") Long id, rechazoSolicitud solicitud) {
        servicio.rechazar(id, solicitud == null ? null : solicitud.motivo());
        return Response.ok(new ResponseApi<>(200, "Publicación rechazada")).build();
    }

    public record rechazoSolicitud(String motivo) {
    }
}
