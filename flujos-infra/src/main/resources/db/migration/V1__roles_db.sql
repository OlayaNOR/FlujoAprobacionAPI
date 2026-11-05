-- El usuario de la aplicación no tiene permisos de DDL. Flyway corre con flujos_migrador.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'flujos_app') THEN
        CREATE ROLE flujos_app LOGIN PASSWORD '${appPassword}';
    END IF;
END
$$;
