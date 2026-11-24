package ucentral.edu.sadm.postulacionesMascotas.infraestructura;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ucentral.edu.sadm.common.infraestructure.ResponseApi;
import ucentral.edu.sadm.postulacionesMascotas.aplicaicon.formularioPostulacionServicio;

/** Vista del dador: postulantes de una de sus mascotas. */
@Path("/api/mascotas/{mascotaId}/postulaciones")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"USUARIO", "FUNDACION"})
public class postulantesMascotaRecursos {

    @Inject
    formularioPostulacionServicio servicio;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Operation(
            summary = "Listar los postulantes de una mascota",
            description = "Solo el dador propietario. Incluye etapa, puntaje y si el formulario está pendiente de evaluación"
    )
    @APIResponse(responseCode = "200", description = "Lista de postulantes (puede venir vacía)")
    @APIResponse(responseCode = "403", description = "La mascota no pertenece al usuario")
    @APIResponse(responseCode = "404", description = "La mascota no existe")
    public Response listar(@PathParam("mascotaId") Long mascotaId) {
        return Response.ok(new ResponseApi<>(200, "Postulantes consultados",
                servicio.listarPostulantes(mascotaId, securityIdentity.getPrincipal().getName()))).build();
    }
}
