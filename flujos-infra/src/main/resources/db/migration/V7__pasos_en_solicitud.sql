-- Copia de los pasos que le aplicaron a la solicitud al enviarla. El paso actual es una posición en esta
-- lista, y si dependiera del flujo vigente, cambiarlo correría los pasos de las solicitudes en curso.
ALTER TABLE solicitud ADD COLUMN pasos JSONB NOT NULL DEFAULT '[]';

-- Las solicitudes ya en curso reciben los pasos que tenían por las reglas anteriores, que son los de V6.
UPDATE solicitud s
SET pasos = (
    SELECT coalesce(jsonb_agg(jsonb_build_object(
               'orden', a.orden, 'rolAprobador', a.rol_aprobador,
               'umbralMinimo', a.umbral_minimo, 'tiempoLimiteHoras', a.tiempo_limite_horas) ORDER BY a.orden), '[]')
    FROM (
        SELECT row_number() OVER (ORDER BY p.orden) AS orden, p.rol_aprobador, p.umbral_minimo, p.tiempo_limite_horas
        FROM paso_aprobacion p
        JOIN flujo f ON f.id = p.flujo_id
        WHERE f.tipo = s.tipo
          AND (p.umbral_minimo IS NULL OR p.umbral_minimo <= CASE
              WHEN s.tipo = 'VACACIONES' THEN s.periodo_fin - s.periodo_inicio + 1
              ELSE s.monto END)
    ) a
)
WHERE s.estado = 'PENDIENTE';
