ALTER TABLE usuario
    ADD COLUMN cargo   VARCHAR(20) CHECK (cargo IN ('JEFE','RRHH','GERENTE','DIRECTOR')),
    ADD COLUMN jefe_id UUID REFERENCES usuario(id),
    ADD CONSTRAINT chk_jefe_distinto CHECK (jefe_id <> id);

CREATE INDEX idx_usuario_cargo ON usuario(cargo, creado_en) WHERE activo AND cargo IS NOT NULL;
