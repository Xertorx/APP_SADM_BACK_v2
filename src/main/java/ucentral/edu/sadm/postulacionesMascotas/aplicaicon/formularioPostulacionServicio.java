package ucentral.edu.sadm.postulacionesMascotas.aplicaicon;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ucentral.edu.sadm.mascotas.dominio.exception.MascotaNoEncontradaException;
import ucentral.edu.sadm.mascotas.dominio.mascota;
import ucentral.edu.sadm.mascotas.dominio.mascotaRepositorio;
import ucentral.edu.sadm.postulacionesMascotas.dominio.estadoPostulacion;
import ucentral.edu.sadm.postulacionesMascotas.dominio.estadoVisiblePostulacion;
import ucentral.edu.sadm.postulacionesMascotas.dominio.exception.PostulacionException;
import ucentral.edu.sadm.postulacionesMascotas.dominio.postulacion;
import ucentral.edu.sadm.postulacionesMascotas.dominio.postulacionRepositorio;
import ucentral.edu.sadm.postulacionesMascotas.dominio.respuestaFormulario;
import ucentral.edu.sadm.postulacionesMascotas.dominio.respuestaFormularioRepositorio;
import ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto.evaluacionFormularioEntidad;
import ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto.evaluacionFormularioSolicitud;
import ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto.formularioPostulacionEntidad;
import ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto.formularioRespuestasSolicitud;
import ucentral.edu.sadm.postulacionesMascotas.infraestructura.dto.postulanteEntidad;
import ucentral.edu.sadm.seleccion.dominio.configuracionSeleccion;
import ucentral.edu.sadm.seleccion.dominio.configuracionSeleccionRepositorio;
import ucentral.edu.sadm.seleccion.dominio.etapaSeleccion;
import ucentral.edu.sadm.seleccion.dominio.preguntaSeleccion;
import ucentral.edu.sadm.seleccion.dominio.tipoEtapaSeleccion;
import ucentral.edu.sadm.usuarios.dominio.usuario;
import ucentral.edu.sadm.usuarios.dominio.usuarioRepositorio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Etapa 1 del proceso de adopción: formulario de preguntas.
 * <ol>
 *   <li>El adoptante responde las preguntas que configuró el dador (borrador y envío).</li>
 *   <li>Al enviar, las respuestas quedan guardadas, el formulario ya no se puede editar y la
 *       postulación pasa directamente a la etapa 2 (ETAPA_2_EN_CURSO).</li>
 *   <li>El dador revisa las respuestas y las califica (0..puntaje de la pregunta). Puede
 *       mantener al postulante en el proceso o DESCARTARLO (DESCARTADA_ETAPA_1, con comentario).</li>
 *   <li>ETAPA_1_EN_EVALUACION queda solo para postulaciones antiguas enviadas con el flujo anterior.</li>
 * </ol>
 */
@ApplicationScoped
public class formularioPostulacionServicio {

    private static final int MAX_TEXTO = 2000;
    private static final int MAX_COMENTARIO = 1000;

    @Inject
    postulacionRepositorio postulacionRepositorio;

    @Inject
    respuestaFormularioRepositorio respuestaRepositorio;

    @Inject
    configuracionSeleccionRepositorio configuracionRepositorio;

    @Inject
    mascotaRepositorio mascotaRepositorio;

    @Inject
    usuarioRepositorio usuarioRepositorio;

    // ------------------------------------------------------------------ adoptante

    @Transactional
    public formularioPostulacionEntidad consultar(Long postulacionId, String correoAdoptante) {
        postulacion postulacion = buscarDelAdoptante(postulacionId, correoAdoptante);
        return convertirFormulario(postulacion);
    }

    /**
     * Guarda las respuestas. Con {@code enviar = false} es un borrador (se permiten preguntas
     * sin responder); con {@code enviar = true} exige todas las preguntas y cierra la edición.
     */
    @Transactional
    public formularioPostulacionEntidad guardar(
            Long postulacionId,
            String correoAdoptante,
            formularioRespuestasSolicitud solicitud,
            boolean enviar
    ) {
        postulacion postulacion = buscarDelAdoptante(postulacionId, correoAdoptante);
        if (postulacion.estado != estadoPostulacion.ETAPA_1_EN_CURSO) {
            throw error(409, "FORMULARIO_NO_EDITABLE",
                    "El formulario ya fue enviado y no se puede modificar",
                    "Solo se puede responder el formulario mientras la postulación está en la etapa 1");
        }
        if (!estaDisponible(postulacion.mascota)) {
            throw error(409, "MASCOTA_NO_DISPONIBLE",
                    "La mascota ya no está disponible",
                    "Solo se puede responder el formulario de mascotas en estado Disponible");
        }

        List<preguntaSeleccion> preguntas = preguntasDe(postulacion.mascota.id);
        Map<Long, preguntaSeleccion> preguntasPorId = new HashMap<>();
        preguntas.forEach(p -> preguntasPorId.put(p.id, p));

        Map<Long, respuestaFormulario> actuales = respuestasPorPregunta(postulacion.id);
        List<formularioRespuestasSolicitud.respuesta> recibidas =
                solicitud == null || solicitud.respuestas() == null ? List.of() : solicitud.respuestas();

        Set<Long> vistas = new HashSet<>();
        for (formularioRespuestasSolicitud.respuesta recibida : recibidas) {
            if (recibida == null || recibida.preguntaId() == null) {
                throw error(400, "RESPUESTA_INVALIDA", "Cada respuesta debe indicar su pregunta",
                        "El campo preguntaId es obligatorio");
            }
            preguntaSeleccion pregunta = preguntasPorId.get(recibida.preguntaId());
            if (pregunta == null) {
                throw error(400, "PREGUNTA_NO_PERTENECE", "Una de las preguntas no pertenece a este formulario",
                        "La pregunta " + recibida.preguntaId() + " no está configurada para esta mascota");
            }
            if (!vistas.add(pregunta.id)) {
                throw error(400, "RESPUESTA_DUPLICADA", "No se puede responder la misma pregunta dos veces",
                        "La pregunta " + pregunta.id + " aparece más de una vez");
            }

            valorNormalizado valor = normalizar(pregunta, recibida);
            respuestaFormulario existente = actuales.get(pregunta.id);
            if (valor == null) {
                if (existente != null) {
                    respuestaRepositorio.delete(existente);
                    actuales.remove(pregunta.id);
                }
                continue;
            }
            respuestaFormulario respuesta = existente != null ? existente : new respuestaFormulario();
            respuesta.postulacion = postulacion;
            respuesta.pregunta = pregunta;
            respuesta.valor = valor.valor();
            respuesta.opciones.clear();
            respuesta.opciones.addAll(valor.opciones());
            if (existente == null) {
                respuestaRepositorio.persist(respuesta);
                actuales.put(pregunta.id, respuesta);
            }
        }

        if (enviar) {
            long faltantes = preguntas.stream().filter(p -> !actuales.containsKey(p.id)).count();
            if (faltantes > 0) {
                throw error(400, "FORMULARIO_INCOMPLETO",
                        faltantes == 1
                                ? "Falta responder 1 pregunta"
                                : "Faltan " + faltantes + " preguntas por responder",
                        "Debes responder todas las preguntas antes de enviar el formulario");
            }
            postulacion.estado = estadoPostulacion.ETAPA_2_EN_CURSO;
            postulacion.fecha_envio_formulario = Instant.now();
        }

        respuestaRepositorio.flush();
        return convertirFormulario(postulacion, preguntas, actuales);
    }

    // ------------------------------------------------------------------ dador

    @Transactional
    public List<postulanteEntidad> listarPostulantes(Long mascotaId, String correoDador) {
        mascota mascota = mascotaRepositorio.findById(mascotaId);
        if (mascota == null || "ELIMINADA".equals(mascota.estado)) {
            throw new MascotaNoEncontradaException();
        }
        usuario dador = usuarioRepositorio.findByEmail(correoDador);
        if (dador == null || mascota.dador == null || !mascota.dador.id.equals(dador.id)) {
            throw error(403, "MASCOTA_NO_AUTORIZADA", "No puedes ver los postulantes de esta mascota",
                    "Solo el dador propietario puede consultar sus postulantes");
        }
        return postulacionRepositorio.listarPorMascota(mascotaId).stream()
                .map(p -> new postulanteEntidad(
                        p.id,
                        p.adoptante.id,
                        nombreCompleto(p.adoptante),
                        p.fecha_postulacion,
                        p.estado.name(),
                        p.estado.getEtiqueta(),
                        estadoVisiblePostulacion.etapaDe(p.estado),
                        estadoVisiblePostulacion.nombreEtapa(estadoVisiblePostulacion.etapaDe(p.estado)),
                        p.puntaje == null ? 0 : p.puntaje,
                        p.fecha_envio_formulario != null,
                        p.fecha_envio_formulario,
                        pendienteDeCalificar(p)
                ))
                .toList();
    }

    @Transactional
    public evaluacionFormularioEntidad consultarEvaluacion(Long postulacionId, String correoDador) {
        postulacion postulacion = buscarDelDador(postulacionId, correoDador);
        return convertirEvaluacion(postulacion);
    }

    @Transactional
    public evaluacionFormularioEntidad evaluar(
            Long postulacionId,
            String correoDador,
            evaluacionFormularioSolicitud solicitud
    ) {
        postulacion postulacion = buscarDelDador(postulacionId, correoDador);
        if (!pendienteDeCalificar(postulacion)) {
            throw error(409, "FORMULARIO_NO_EVALUABLE",
                    "Este formulario no está pendiente de calificación",
                    "Solo se califican formularios enviados por el adoptante, aún sin calificar y con la postulación activa");
        }
        if (solicitud == null) {
            throw error(400, "EVALUACION_INVALIDA", "La evaluación es obligatoria", "El cuerpo no puede estar vacío");
        }

        String decision = solicitud.decision() == null ? "" : solicitud.decision().trim().toUpperCase(Locale.ROOT);
        if (!decision.equals("APROBAR") && !decision.equals("DESCARTAR")) {
            throw error(400, "DECISION_INVALIDA", "Debes indicar si apruebas o descartas la postulación",
                    "decision debe ser APROBAR o DESCARTAR");
        }
        String comentario = solicitud.comentario() == null ? null : solicitud.comentario().trim();
        if (comentario != null && comentario.isEmpty()) {
            comentario = null;
        }
        if (decision.equals("DESCARTAR") && comentario == null) {
            throw error(400, "COMENTARIO_OBLIGATORIO", "Indica el motivo por el que descartas la postulación",
                    "El comentario es obligatorio al descartar");
        }
        if (comentario != null && comentario.length() > MAX_COMENTARIO) {
            throw error(400, "COMENTARIO_INVALIDO", "El comentario no puede superar los 1000 caracteres",
                    "Longitud máxima del comentario: " + MAX_COMENTARIO);
        }

        List<preguntaSeleccion> preguntas = preguntasDe(postulacion.mascota.id);
        Map<Long, preguntaSeleccion> preguntasPorId = new HashMap<>();
        preguntas.forEach(p -> preguntasPorId.put(p.id, p));
        Map<Long, respuestaFormulario> respuestas = respuestasPorPregunta(postulacion.id);

        Map<Long, Integer> puntajes = new HashMap<>();
        List<evaluacionFormularioSolicitud.calificacion> calificaciones =
                solicitud.calificaciones() == null ? List.of() : solicitud.calificaciones();
        for (evaluacionFormularioSolicitud.calificacion calificacion : calificaciones) {
            if (calificacion == null || calificacion.preguntaId() == null || calificacion.puntaje() == null) {
                throw error(400, "CALIFICACION_INVALIDA", "Cada calificación debe tener pregunta y puntaje",
                        "preguntaId y puntaje son obligatorios");
            }
            preguntaSeleccion pregunta = preguntasPorId.get(calificacion.preguntaId());
            if (pregunta == null || !respuestas.containsKey(pregunta.id)) {
                throw error(400, "PREGUNTA_NO_PERTENECE", "Una de las calificaciones no corresponde a este formulario",
                        "La pregunta " + calificacion.preguntaId() + " no tiene respuesta en esta postulación");
            }
            if (puntajes.put(pregunta.id, calificacion.puntaje()) != null) {
                throw error(400, "CALIFICACION_DUPLICADA", "No se puede calificar la misma pregunta dos veces",
                        "La pregunta " + pregunta.id + " aparece más de una vez");
            }
            int maximo = pregunta.puntaje == null ? 0 : pregunta.puntaje;
            if (calificacion.puntaje() < 0 || calificacion.puntaje() > maximo) {
                throw error(400, "PUNTAJE_FUERA_DE_RANGO",
                        "El puntaje de \"" + pregunta.texto + "\" debe estar entre 0 y " + maximo,
                        "Puntaje recibido: " + calificacion.puntaje());
            }
        }
        long sinCalificar = respuestas.keySet().stream().filter(id -> !puntajes.containsKey(id)).count();
        if (sinCalificar > 0) {
            throw error(400, "CALIFICACION_INCOMPLETA", "Debes calificar todas las respuestas",
                    "Faltan " + sinCalificar + " respuestas por calificar");
        }

        int total = 0;
        for (respuestaFormulario respuesta : respuestas.values()) {
            respuesta.puntajeAsignado = puntajes.get(respuesta.pregunta.id);
            total += respuesta.puntajeAsignado;
        }
        postulacion.puntaje = total;
        postulacion.comentario_evaluacion = comentario;
        postulacion.fecha_evaluacion_formulario = Instant.now();
        if (decision.equals("DESCARTAR")) {
            postulacion.estado = estadoPostulacion.DESCARTADA_ETAPA_1;
        } else if (postulacion.estado == estadoPostulacion.ETAPA_1_EN_EVALUACION) {
            // Postulaciones del flujo anterior: al aprobar pasan a la etapa 2.
            postulacion.estado = estadoPostulacion.ETAPA_2_EN_CURSO;
        }

        postulacionRepositorio.flush();
        return convertirEvaluacion(postulacion, preguntas, respuestas);
    }

    // ------------------------------------------------------------------ búsquedas

    private postulacion buscarPostulacion(Long postulacionId) {
        postulacion postulacion = postulacionRepositorio.findById(postulacionId);
        if (postulacion == null) {
            throw error(404, "POSTULACION_NO_ENCONTRADA", "La postulación no existe",
                    "No se encontró una postulación con el identificador solicitado");
        }
        return postulacion;
    }

    private postulacion buscarDelAdoptante(Long postulacionId, String correo) {
        postulacion postulacion = buscarPostulacion(postulacionId);
        usuario usuario = usuarioRepositorio.findByEmail(correo);
        if (usuario == null || !postulacion.adoptante.id.equals(usuario.id)) {
            throw error(403, "POSTULACION_NO_AUTORIZADA", "Esta postulación no te pertenece",
                    "Solo el adoptante de la postulación puede ver o responder su formulario");
        }
        return postulacion;
    }

    private postulacion buscarDelDador(Long postulacionId, String correo) {
        postulacion postulacion = buscarPostulacion(postulacionId);
        usuario usuario = usuarioRepositorio.findByEmail(correo);
        mascota mascota = postulacion.mascota;
        if (usuario == null || mascota == null || mascota.dador == null || !mascota.dador.id.equals(usuario.id)) {
            throw error(403, "POSTULACION_NO_AUTORIZADA", "No puedes evaluar esta postulación",
                    "Solo el dador de la mascota puede evaluar el formulario");
        }
        return postulacion;
    }

    private etapaSeleccion etapaPreguntas(Long mascotaId) {
        configuracionSeleccion configuracion = configuracionRepositorio.buscarPorMascota(mascotaId);
        if (configuracion == null) {
            return null;
        }
        return configuracion.etapas.stream()
                .filter(e -> e.tipo == tipoEtapaSeleccion.PREGUNTAS)
                .findFirst()
                .orElse(null);
    }

    private List<preguntaSeleccion> preguntasDe(Long mascotaId) {
        etapaSeleccion etapa = etapaPreguntas(mascotaId);
        if (etapa == null) {
            return List.of();
        }
        List<preguntaSeleccion> preguntas = new ArrayList<>(etapa.preguntas);
        preguntas.sort(Comparator.comparing(p -> p.id));
        return preguntas;
    }

    private Map<Long, respuestaFormulario> respuestasPorPregunta(Long postulacionId) {
        Map<Long, respuestaFormulario> respuestas = new HashMap<>();
        for (respuestaFormulario respuesta : respuestaRepositorio.listarPorPostulacion(postulacionId)) {
            respuestas.put(respuesta.pregunta.id, respuesta);
        }
        return respuestas;
    }

    // ------------------------------------------------------------------ validación

    private record valorNormalizado(String valor, List<String> opciones) {
    }

    /** Valida la respuesta según el tipo de pregunta; null si viene vacía. */
    private valorNormalizado normalizar(preguntaSeleccion pregunta, formularioRespuestasSolicitud.respuesta recibida) {
        String texto = recibida.valor() == null ? "" : recibida.valor().trim();
        return switch (pregunta.tipoRespuesta) {
            case TEXTO -> {
                if (texto.isEmpty()) {
                    yield null;
                }
                if (texto.length() > MAX_TEXTO) {
                    throw error(400, "RESPUESTA_INVALIDA",
                            "La respuesta a \"" + pregunta.texto + "\" no puede superar los 2000 caracteres",
                            "Longitud máxima: " + MAX_TEXTO);
                }
                yield new valorNormalizado(texto, List.of());
            }
            case SI_NO -> {
                if (texto.isEmpty()) {
                    yield null;
                }
                String normalizado = texto.toUpperCase(Locale.ROOT).replace("Í", "I");
                if (!normalizado.equals("SI") && !normalizado.equals("NO")) {
                    throw error(400, "RESPUESTA_INVALIDA",
                            "La respuesta a \"" + pregunta.texto + "\" debe ser Sí o No",
                            "Valores permitidos: SI, NO");
                }
                yield new valorNormalizado(normalizado, List.of());
            }
            case SELECCION_UNICA -> {
                if (texto.isEmpty()) {
                    yield null;
                }
                yield new valorNormalizado(opcionValida(pregunta, texto), List.of());
            }
            case SELECCION_MULTIPLE -> {
                List<String> recibidas = recibida.opciones() == null ? List.of() : recibida.opciones();
                Set<String> elegidas = new LinkedHashSet<>();
                for (String opcion : recibidas) {
                    if (opcion == null || opcion.isBlank()) {
                        continue;
                    }
                    if (!elegidas.add(opcionValida(pregunta, opcion.trim()))) {
                        throw error(400, "RESPUESTA_INVALIDA",
                                "No repitas opciones en \"" + pregunta.texto + "\"",
                                "Opción repetida: " + opcion.trim());
                    }
                }
                yield elegidas.isEmpty() ? null : new valorNormalizado(null, List.copyOf(elegidas));
            }
        };
    }

    /** Devuelve la opción tal como la configuró el dador (comparación sin mayúsculas). */
    private String opcionValida(preguntaSeleccion pregunta, String elegida) {
        for (String opcion : pregunta.opciones) {
            if (opcion.trim().equalsIgnoreCase(elegida)) {
                return opcion;
            }
        }
        throw error(400, "OPCION_INVALIDA",
                "\"" + elegida + "\" no es una opción válida para \"" + pregunta.texto + "\"",
                "Opciones permitidas: " + String.join(", ", pregunta.opciones));
    }

    // ------------------------------------------------------------------ conversión

    private formularioPostulacionEntidad convertirFormulario(postulacion postulacion) {
        return convertirFormulario(
                postulacion,
                preguntasDe(postulacion.mascota.id),
                respuestasPorPregunta(postulacion.id));
    }

    private formularioPostulacionEntidad convertirFormulario(
            postulacion postulacion,
            List<preguntaSeleccion> preguntas,
            Map<Long, respuestaFormulario> respuestas
    ) {
        List<formularioPostulacionEntidad.pregunta> items = preguntas.stream()
                .map(p -> {
                    respuestaFormulario r = respuestas.get(p.id);
                    return new formularioPostulacionEntidad.pregunta(
                            p.id, p.texto, p.tipoRespuesta, List.copyOf(p.opciones),
                            r == null ? null : r.valor,
                            r == null ? List.of() : List.copyOf(r.opciones));
                })
                .toList();
        int respondidas = (int) preguntas.stream().filter(p -> respuestas.containsKey(p.id)).count();
        boolean editable = postulacion.estado == estadoPostulacion.ETAPA_1_EN_CURSO
                && estaDisponible(postulacion.mascota);
        boolean descartada = postulacion.estado == estadoPostulacion.DESCARTADA_ETAPA_1;
        return new formularioPostulacionEntidad(
                postulacion.id,
                postulacion.mascota.id,
                postulacion.mascota.nombre,
                postulacion.estado.name(),
                postulacion.estado.getEtiqueta(),
                estadoVisiblePostulacion.etapaDe(postulacion.estado),
                estadoVisiblePostulacion.nombreEtapa(estadoVisiblePostulacion.etapaDe(postulacion.estado)),
                editable,
                postulacion.fecha_envio_formulario != null,
                postulacion.fecha_envio_formulario,
                preguntas.size(),
                respondidas,
                descartada ? postulacion.comentario_evaluacion : null,
                items
        );
    }

    private evaluacionFormularioEntidad convertirEvaluacion(postulacion postulacion) {
        // El dador solo ve las respuestas cuando el adoptante envió el formulario (no los borradores).
        return convertirEvaluacion(
                postulacion,
                preguntasDe(postulacion.mascota.id),
                postulacion.fecha_envio_formulario == null ? Map.of() : respuestasPorPregunta(postulacion.id));
    }

    private evaluacionFormularioEntidad convertirEvaluacion(
            postulacion postulacion,
            List<preguntaSeleccion> preguntas,
            Map<Long, respuestaFormulario> respuestas
    ) {
        etapaSeleccion etapa = etapaPreguntas(postulacion.mascota.id);
        int puntajeObtenido = respuestas.values().stream()
                .mapToInt(r -> r.puntajeAsignado == null ? 0 : r.puntajeAsignado)
                .sum();
        List<evaluacionFormularioEntidad.pregunta> items = preguntas.stream()
                .map(p -> {
                    respuestaFormulario r = respuestas.get(p.id);
                    return new evaluacionFormularioEntidad.pregunta(
                            p.id, p.texto, p.tipoRespuesta, List.copyOf(p.opciones),
                            p.puntaje == null ? 0 : p.puntaje,
                            r == null ? null : r.valor,
                            r == null ? List.of() : List.copyOf(r.opciones),
                            r == null ? null : r.puntajeAsignado);
                })
                .toList();
        return new evaluacionFormularioEntidad(
                postulacion.id,
                postulacion.mascota.id,
                postulacion.mascota.nombre,
                postulacion.adoptante.id,
                nombreCompleto(postulacion.adoptante),
                postulacion.estado.name(),
                postulacion.estado.getEtiqueta(),
                estadoVisiblePostulacion.etapaDe(postulacion.estado),
                estadoVisiblePostulacion.nombreEtapa(estadoVisiblePostulacion.etapaDe(postulacion.estado)),
                pendienteDeCalificar(postulacion),
                postulacion.fecha_envio_formulario,
                postulacion.fecha_evaluacion_formulario,
                etapa == null || etapa.puntajeMaximo == null ? 0 : etapa.puntajeMaximo,
                puntajeObtenido,
                postulacion.comentario_evaluacion,
                items
        );
    }

    /** Formulario enviado, aún sin calificar y con la postulación todavía activa. */
    private boolean pendienteDeCalificar(postulacion p) {
        return p.fecha_envio_formulario != null
                && p.fecha_evaluacion_formulario == null
                && (p.estado == estadoPostulacion.ETAPA_1_EN_EVALUACION
                || p.estado == estadoPostulacion.ETAPA_2_EN_CURSO);
    }

    private boolean estaDisponible(mascota mascota) {
        return mascota != null && mascota.estado != null && "DISPONIBLE".equalsIgnoreCase(mascota.estado.trim());
    }

    private String nombreCompleto(usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String nombre = usuario.nombre == null ? "" : usuario.nombre.trim();
        String apellido = usuario.apellido == null ? "" : usuario.apellido.trim();
        return (nombre + " " + apellido).trim();
    }

    private PostulacionException error(int status, String codigo, String mensaje, String detalle) {
        return new PostulacionException(status, codigo, mensaje, detalle);
    }
}
