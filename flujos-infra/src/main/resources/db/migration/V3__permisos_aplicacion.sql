GRANT SELECT, INSERT, UPDATE, DELETE ON
    usuario, usuario_rol, refresh_token, flujo, paso_aprobacion,
    solicitud, aprobacion, outbox, idempotencia
TO flujos_app;

-- La auditoría es inmutable: sin UPDATE ni DELETE para la aplicación.
GRANT SELECT, INSERT ON registro_auditoria TO flujos_app;

GRANT USAGE, SELECT ON SEQUENCE registro_auditoria_id_seq, outbox_id_seq TO flujos_app;
