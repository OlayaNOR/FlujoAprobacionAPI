-- Los mismos pasos que hasta ahora estaban en las clases de regla.
-- El umbral es inclusivo: en compras y gastos es un monto en la moneda base, en vacaciones son días.
INSERT INTO flujo (id, tipo, version) VALUES
    (gen_random_uuid(), 'VACACIONES', 1),
    (gen_random_uuid(), 'COMPRA', 1),
    (gen_random_uuid(), 'GASTO', 1);

INSERT INTO paso_aprobacion (id, flujo_id, orden, rol_aprobador, umbral_minimo, tiempo_limite_horas)
SELECT gen_random_uuid(), f.id, p.orden, p.rol, p.umbral, 48
FROM flujo f
JOIN (VALUES
    ('VACACIONES', 1, 'JEFE',     NULL::NUMERIC),
    ('VACACIONES', 2, 'RRHH',     4),
    ('COMPRA',     1, 'JEFE',     NULL),
    ('COMPRA',     2, 'GERENTE',  1000),
    ('COMPRA',     3, 'DIRECTOR', 10000.01),
    ('GASTO',      1, 'JEFE',     200)
) AS p (tipo, orden, rol, umbral) ON p.tipo = f.tipo;
