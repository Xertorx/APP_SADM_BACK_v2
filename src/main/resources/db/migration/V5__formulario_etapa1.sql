-- Etapa 1 del proceso de adopción: respuestas del formulario y su evaluación.
-- (Con hibernate schema-management "update" estas tablas/columnas se crean solas;
--  este script documenta el esquema y sirve para crearlo a mano.)

ALTER TABLE postulacion ADD COLUMN IF NOT EXISTS fecha_envio_formulario TIMESTAMP WITH TIME ZONE;
ALTER TABLE postulacion ADD COLUMN IF NOT EXISTS fecha_evaluacion_formulario TIMESTAMP WITH TIME ZONE;
ALTER TABLE postulacion ADD COLUMN IF NOT EXISTS comentario_evaluacion VARCHAR(1000);

CREATE TABLE IF NOT EXISTS respuesta_formulario (
    id BIGINT PRIMARY KEY,
    postulacion_id BIGINT NOT NULL,
    pregunta_id BIGINT NOT NULL,
    valor VARCHAR(2000),
    puntaje_asignado INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_respuesta_formulario_postulacion
        FOREIGN KEY (postulacion_id) REFERENCES postulacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_respuesta_formulario_pregunta
        FOREIGN KEY (pregunta_id) REFERENCES pregunta_seleccion (id) ON DELETE CASCADE,
    CONSTRAINT uk_respuesta_formulario_postulacion_pregunta UNIQUE (postulacion_id, pregunta_id)
);

CREATE TABLE IF NOT EXISTS respuesta_formulario_opcion (
    respuesta_id BIGINT NOT NULL,
    orden INTEGER NOT NULL,
    opcion VARCHAR(200) NOT NULL,
    PRIMARY KEY (respuesta_id, orden),
    CONSTRAINT fk_respuesta_formulario_opcion_respuesta
        FOREIGN KEY (respuesta_id) REFERENCES respuesta_formulario (id) ON DELETE CASCADE
);
