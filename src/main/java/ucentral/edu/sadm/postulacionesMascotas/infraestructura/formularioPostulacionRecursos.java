package ucentral.edu.sadm.postulacionesMascotas.infraestructura;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import ucentral.edu.sadm.common.infraestructure.ResponseApi;
import ucentral.edu.sadm.postulacionesMascotas.aplicaicon.formularioPostulacionServicio;
import ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto.evaluacionFormularioSolicitud;
import ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto.formularioRespuestasSolicitud;

/** Etapa 1: formulario de preguntas (adoptante) y su evaluación (dador). */
@Path("/api/postulaciones/{postulacionId}/formulario")
@Produces(MediaType.APPLICATION_JSON)
public class formularioPostulacionRecursos {

    @Inject
    formularioPostulacionServicio servicio;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @RolesAllowed("USUARIO")
    @Operation(
            summary = "Consultar el formulario de la etapa 1",
            description = "Preguntas configuradas por el dador con las respuestas guardadas del adoptante autenticado"
    )
    @APIResponse(responseCode = "200", description = "Formulario de la postulación")
    @APIResponse(responseCode = "403", description = "La postulación no pertenece al usuario")
    @APIResponse(responseCode = "404", description = "La postulación no existe")
    public Response consultar(@PathParam("postulacionId") Long postulacionId) {
        return Response.ok(new ResponseApi<>(200, "Formulario consultado",
                servicio.consultar(postulacionId, correo()))).build();
    }

    @PUT
    @RolesAllowed("USUARIO")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Guardar borrador del formulario",
            description = "Guarda las respuestas enviadas; se pueden dejar preguntas sin responder. Una respuesta vacía borra la guardada."
    )
    @APIResponse(responseCode = "200", description = "Borrador guardado")
    @APIResponse(responseCode = "400", description = "Respuesta inválida para el tipo de pregunta")
    @APIResponse(responseCode = "409", description = "El formulario ya fue enviado o la mascota no está disponible")
    public Response guardar(
            @PathParam("postulacionId") Long postulacionId,
            formularioRespuestasSolicitud solicitud
    ) {
        return Response.ok(new ResponseApi<>(200, "Respuestas guardadas",
                servicio.guardar(postulacionId, correo(), solicitud, false))).build();
    }

    @POST
    @Path("/enviar")
    @RolesAllowed("USUARIO")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Enviar el formulario de la etapa 1",
            description = "Guarda las respuestas, exige todas las preguntas respondidas y pasa la postulación a la etapa 2 (ETAPA_2_EN_CURSO)"
    )
    @APIResponse(responseCode = "200", description = "Formulario enviado")
    @APIResponse(responseCode = "400", description = "Formulario incompleto o respuesta inválida")
    @APIResponse(responseCode = "409", description = "El formulario ya fue enviado o la mascota no está disponible")
    public Response enviar(
            @PathParam("postulacionId") Long postulacionId,
            formularioRespuestasSolicitud solicitud
    ) {
        return Response.ok(new ResponseApi<>(200, "¡Formulario enviado! Tus respuestas quedaron guardadas y pasaste a la etapa 2: Documentos.",
                servicio.guardar(postulacionId, correo(), solicitud, true))).build();
    }

    @GET
    @Path("/evaluacion")
    @RolesAllowed({"USUARIO", "FUNDACION"})
    @Operation(
            summary = "Consultar las respuestas para evaluar (dador)",
            description = "Respuestas del adoptante con el puntaje máximo de cada pregunta"
    )
    @APIResponse(responseCode = "200", description = "Respuestas del formulario")
    @APIResponse(responseCode = "403", description = "Solo el dador de la mascota puede evaluar")
    public Response consultarEvaluacion(@PathParam("postulacionId") Long postulacionId) {
        return Response.ok(new ResponseApi<>(200, "Formulario consultado",
                servicio.consultarEvaluacion(postulacionId, correo()))).build();
    }

    @POST
    @Path("/evaluacion")
    @RolesAllowed({"USUARIO", "FUNDACION"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Calificar el formulario y decidir la etapa 1 (dador)",
            description = "Puntaje 0..máximo por pregunta respondida. decision APROBAR guarda la calificación y el postulante sigue en el proceso; DESCARTAR (con comentario) pasa a DESCARTADA_ETAPA_1"
    )
    @APIResponse(responseCode = "200", description = "Evaluación registrada")
    @APIResponse(responseCode = "400", description = "Calificaciones incompletas, fuera de rango o falta el comentario")
    @APIResponse(responseCode = "409", description = "El formulario no está pendiente de evaluación")
    public Response evaluar(
            @PathParam("postulacionId") Long postulacionId,
            evaluacionFormularioSolicitud solicitud
    ) {
        return Response.ok(new ResponseApi<>(200, "Evaluación registrada",
                servicio.evaluar(postulacionId, correo(), solicitud))).build();
    }

    private String correo() {
        return securityIdentity.getPrincipal().getName();
    }
}
