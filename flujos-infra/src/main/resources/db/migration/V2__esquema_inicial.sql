CREATE TABLE usuario (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    nombre          VARCHAR(120) NOT NULL,
    password_hash   VARCHAR(72)  NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE usuario_rol (
    usuario_id      UUID        NOT NULL REFERENCES usuario(id),
    rol             VARCHAR(20) NOT NULL CHECK (rol IN ('EMPLEADO','APROBADOR','ADMIN')),
    PRIMARY KEY (usuario_id, rol)
);

CREATE TABLE refresh_token (
    id              UUID PRIMARY KEY,
    usuario_id      UUID        NOT NULL REFERENCES usuario(id),
    token_hash      CHAR(64)    NOT NULL UNIQUE,
    expira_en       TIMESTAMPTZ NOT NULL,
    revocado        BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE flujo (
    id              UUID PRIMARY KEY,
    tipo            VARCHAR(20) NOT NULL UNIQUE,
    activo          BOOLEAN     NOT NULL DEFAULT TRUE,
    version         BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE paso_aprobacion (
    id                  UUID PRIMARY KEY,
    flujo_id            UUID        NOT NULL REFERENCES flujo(id) ON DELETE CASCADE,
    orden               INT         NOT NULL,
    rol_aprobador       VARCHAR(20) NOT NULL,
    umbral_minimo       NUMERIC(14,2),
    tiempo_limite_horas INT         NOT NULL DEFAULT 48,
    UNIQUE (flujo_id, orden)
);

CREATE TABLE solicitud (
    id                  UUID PRIMARY KEY,
    solicitante_id      UUID          NOT NULL REFERENCES usuario(id),
    tipo                VARCHAR(20)   NOT NULL,
    estado              VARCHAR(20)   NOT NULL,
    monto               NUMERIC(14,2),
    moneda              CHAR(3),
    periodo_inicio      DATE,
    periodo_fin         DATE,
    motivo              VARCHAR(500),
    paso_actual         INT           NOT NULL DEFAULT 0,  -- 0 en borrador, los pasos empiezan en 1
    aprobador_actual_id UUID          REFERENCES usuario(id),
    fecha_limite_paso   TIMESTAMPTZ,
    version             BIGINT        NOT NULL DEFAULT 0,
    creada_en           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    actualizada_en      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_estado CHECK (estado IN
        ('BORRADOR','PENDIENTE','APROBADA','RECHAZADA','CANCELADA','CERRADA')),
    CONSTRAINT chk_monto_positivo CHECK (monto IS NULL OR monto > 0),
    CONSTRAINT chk_periodo CHECK (periodo_inicio IS NULL OR periodo_fin >= periodo_inicio)
);

CREATE INDEX idx_solicitud_solicitante ON solicitud(solicitante_id, creada_en DESC);
CREATE INDEX idx_solicitud_aprobador ON solicitud(aprobador_actual_id) WHERE estado = 'PENDIENTE';
-- índice parcial para el job de escalamiento
CREATE INDEX idx_solicitud_estado_limite ON solicitud(estado, fecha_limite_paso)
    WHERE estado = 'PENDIENTE';

CREATE TABLE aprobacion (
    id              UUID PRIMARY KEY,
    solicitud_id    UUID        NOT NULL REFERENCES solicitud(id),
    paso            INT         NOT NULL,
    aprobador_id    UUID        NOT NULL REFERENCES usuario(id),
    decision        VARCHAR(10) NOT NULL CHECK (decision IN ('APROBADA','RECHAZADA')),
    comentario      VARCHAR(500),
    decidida_en     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE registro_auditoria (
    id                  BIGSERIAL PRIMARY KEY,
    solicitud_id        UUID        NOT NULL,
    accion              VARCHAR(30) NOT NULL,
    actor_id            UUID,                      -- null cuando actúa el sistema
    estado_anterior     VARCHAR(20),
    estado_nuevo        VARCHAR(20),
    detalle             JSONB,
    ocurrido_en         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auditoria_solicitud ON registro_auditoria(solicitud_id, ocurrido_en);

CREATE TABLE outbox (
    id              BIGSERIAL PRIMARY KEY,
    agregado_id     UUID         NOT NULL,
    tipo_evento     VARCHAR(60)  NOT NULL,
    payload         JSONB        NOT NULL,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    publicado_en    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pendiente ON outbox(creado_en) WHERE publicado_en IS NULL;

CREATE TABLE idempotencia (
    clave           VARCHAR(100) NOT NULL,
    usuario_id      UUID         NOT NULL,
    hash_request    CHAR(64)     NOT NULL,
    status_code     INT          NOT NULL,
    respuesta       JSONB,
    creado_en       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (clave, usuario_id)
);
