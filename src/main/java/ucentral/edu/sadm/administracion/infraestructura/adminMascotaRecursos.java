package ucentral.edu.sadm.administracion.infraestructura;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import ucentral.edu.sadm.administracion.aplicacion.adminMascotaServicio;
import ucentral.edu.sadm.administracion.infraestructura.dto.publicacionPendienteEntidad;
import ucentral.edu.sadm.administracion.infraestructura.dto.rechazoPublicacionSolicitud;
import ucentral.edu.sadm.common.infraestructure.ResponseApi;
import ucentral.edu.sadm.mascotas.infraestructura.dto.mascotaFormulario;

import java.util.List;

/** HU26: moderación de publicaciones. Solo ADMINISTRADOR; el admin sale del token. */
@Path("/api/admin/mascotas")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMINISTRADOR")
public class adminMascotaRecursos {

    @Inject
    adminMascotaServicio servicio;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Path("/pendientes")
    @Operation(summary = "Listar las publicaciones pendientes de aprobación (las más antiguas primero)")
    public Response pendientes() {
        List<publicacionPendienteEntidad> datos = servicio.listarPendientes();
        return Response.ok(new ResponseApi<>(200, "Publicaciones pendientes consultadas", datos)).build();
    }

    @POST
    @Path("/{id}/aprobar")
    @Operation(summary = "Aprobar una publicación pendiente (pasa a DISPONIBLE)")
    public Response aprobar(@PathParam("id") Long id) {
        servicio.aprobar(id, correoAutenticado());
        return Response.ok(new ResponseApi<>(200, "Publicación aprobada correctamente.")).build();
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Rechazar (eliminar) una publicación pendiente",
            description = "Borrado lógico (estado ELIMINADA). Motivo obligatorio, máximo 300 caracteres"
    )
    public Response rechazar(@PathParam("id") Long id, rechazoPublicacionSolicitud solicitud) {
        String motivo = solicitud == null ? null : solicitud.motivo();
        servicio.rechazar(id, motivo, correoAutenticado());
        return Response.ok(new ResponseApi<>(200, "Publicación rechazada correctamente.")).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
            summary = "Editar una publicación pendiente",
            description = "Mismo contrato que PUT /api/mascotas/{id} (parte 'datos' JSON + 'fotos' opcionales). "
                    + "Sin fotos se conservan las actuales. La publicación sigue en PENDIENTE_APROBACION"
    )
    public Response editar(@PathParam("id") Long id, @Valid @BeanParam mascotaFormulario formulario) {
        servicio.editar(id, formulario.datos, formulario.fotos, correoAutenticado());
        return Response.ok(new ResponseApi<>(200,
                "Publicación editada y enviada nuevamente al flujo de moderación.")).build();
    }

    private String correoAutenticado() {
        return securityIdentity.getPrincipal().getName();
    }
}
