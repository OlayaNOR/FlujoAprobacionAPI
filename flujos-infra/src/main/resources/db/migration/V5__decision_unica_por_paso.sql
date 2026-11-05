ALTER TABLE aprobacion ADD CONSTRAINT uq_aprobacion_paso UNIQUE (solicitud_id, paso);
