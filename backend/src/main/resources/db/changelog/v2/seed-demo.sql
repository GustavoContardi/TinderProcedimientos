-- =====================================================================
-- SEED DEMO v2 del Tinder de procedimientos. Contraseña de todos: demo123
-- Reemplaza al seed viejo (100-seed) solo en el perfil "demo".
-- =====================================================================

TRUNCATE TABLE mejora, objecion, segunda_cita, resultado_procedimiento, resultado_ciclo, voto, voto_emitido,
  matriz_participacion, matriz_revision, alerta, casamiento_testigo, casamiento_resultado, casamiento_area, casamiento,
  velorio, procedimiento_area, ciclo_votacion, procedimiento, designacion_juez, usuario, area
  RESTART IDENTITY CASCADE;
ALTER SEQUENCE mejora_codigo_seq RESTART WITH 1;

-- Áreas
INSERT INTO area (id, nombre) VALUES
  (1, 'Operaciones y Mesa de Ayuda'),
  (2, 'Desarrollo'),
  (3, 'Infraestructura'),
  (4, 'Atención al Público');

-- Usuarios (nombres ficticios). Hash BCrypt de "demo123".
INSERT INTO usuario (id, nombre, email, password_hash, rol, area_id) VALUES
  (1,  'Ana Paredes',      'ana@demo.com',       '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (2,  'Bruno Lescano',    'bruno@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (3,  'Carla Medina',     'carla@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (4,  'Darío Funes',      'dario@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (5,  'Elena Quiroga',    'elena@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (6,  'Facundo Ibarra',   'facundo@demo.com',   '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (7,  'Gabriela Sosa',    'gabi@demo.com',      '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (8,  'Hernán Vidal',     'hernan@demo.com',    '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (9,  'Inés Barrios',     'ines@demo.com',      '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (10, 'Julián Ortega',    'julian@demo.com',    '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (11, 'Karen Molina',     'karen@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (12, 'Leandro Ruiz',     'leo@demo.com',       '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (13, 'Mónica Calderón',  'juez@demo.com',      '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'JUEZ_PAZ',  1),
  (14, 'Ricardo Ferreyra', 'direccion@demo.com', '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'DIRECCION', 2),
  (15, 'Paula Giménez',    'paula@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (16, 'Óscar Benítez',    'oscar@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (17, 'Eva Romero',       'eva@demo.com',       '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (18, 'Tomás Acosta',     'tomas@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (19, 'Rocío Almada',     'rocio@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  4),
  (20, 'Santiago Pereyra', 'santi@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  4),
  (21, 'Uma Ledesma',      'uma@demo.com',       '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  4),
  (22, 'Vicente Correa',   'vicente@demo.com',   '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  4);

-- Rotación del juez de paz (Operaciones): Carla en 2025, Mónica desde 2026
INSERT INTO designacion_juez (usuario_id, area_id, desde, hasta) VALUES
  (3, 1, '2025-01-01', '2025-12-31'),
  (13, 1, '2026-01-01', NULL);

-- Procedimientos. autor = padrino; dueno = responsable del contenido
INSERT INTO procedimiento (id, codigo, nombre, tipo, bio, fecha_creacion, minutos_por_uso, usos_por_semana, beneficio,
                           obligatorio, base_obligatoriedad, autor_id, dueno_id, version, version_express, estado,
                           segundas_citas_consecutivas, requiere_reformulacion) VALUES
  (1, 'PR-01', 'Desarrollo y mantenimiento de software', 'PROCEDIMIENTO',
   'Soy metódico, me gustan los tickets bien escritos y odio los cambios en producción un viernes a las 18. Si me seguís, tu código llega sano a producción.',
   '2019-03-01', 30, 5, 'que nadie te culpe cuando se cae el sistema: está todo documentado', false, NULL, 5, 5, 1, NULL, 'EN_TINDER', 0, false),
  (2, 'PR-02', 'Backup y recuperación', 'PROCEDIMIENTO',
   'No soy el más divertido, pero cuando todo se prende fuego soy el único que aparece. Busco una relación estable, con copias en dos lugares distintos.',
   '2018-06-15', 20, 5, 'dormir tranquilo aunque explote un disco', true, 'ISO 9001 7.1.3 / continuidad', 10, 10, 2, NULL, 'SEGUNDA_CITA', 2, true),
  (3, 'PR-03', 'Peticiones urgentes', 'PROCEDIMIENTO',
   'Llego sin avisar, siempre con apuro, y aun así todos me necesitan. Soy de todas las áreas y no me avergüenza. Si me ordenás, prometo dejar de ser un caos.',
   '2021-09-10', 15, 10, 'un orden de prioridades para cuando el jefe dice que es para ayer', false, NULL, 1, 3, 1,
   E'1. Llamá a la mesa (interno 4000).\n2. Decí qué se rompió y a cuántas personas afecta.\n3. Si afecta a más de 10, es urgente: se atiende ya y se documenta después.', 'PENDIENTE_CASAMIENTO', 0, false),
  (4, 'PR-04', 'Gestión de configuración y versionado', 'PROCEDIMIENTO',
   'Me acuerdo de cada versión que tuviste. Algunos dicen que soy intenso, yo digo que soy trazable. Mi lenguaje del amor es el commit con buen mensaje.',
   '2020-02-20', 10, 15, 'saber qué cambió, cuándo y quién (sin dramas)', false, NULL, 8, 8, 1, NULL, 'EN_TINDER', 0, false),
  (5, 'PR-05', 'Evaluación de proveedores', 'PROCEDIMIENTO',
   'Una vez por año evalúo a todos con una planilla de 40 columnas. Juzgo, pero con criterio. Busco a alguien que no me deje para último momento.',
   '2017-11-01', 45, 1, 'proveedores que cumplen (o al menos una prueba de que no cumplieron)', false, NULL, NULL, 18, 1, NULL, 'PENDIENTE_CASAMIENTO', 0, false),
  (6, 'PR-06', 'Acuerdos de nivel de servicio', 'PROCEDIMIENTO',
   'Soy de cumplir lo que prometo, y si no, te muestro el indicador. Me encantan los compromisos con fecha y hora.',
   '2019-08-01', 20, 3, 'que el usuario sepa cuándo le resolvemos el problema', false, NULL, 2, 4, 1,
   'SLA express: crítico 4 h, alto 1 día, el resto 3 días. Si no llegás, avisá antes.', 'CASADO', 0, false),
  (7, 'F-07', 'Encuesta de mesa de ayuda', 'REGISTRO',
   'Tengo 2 años. Te llevo 1 minuto. Busco a alguien que me complete después de cada ticket.',
   '2024-10-01', 1, 40, 'saber si la gente se va contenta o se va puteando', false, NULL, 3, 17, 1, NULL, 'SEGUNDA_CITA', 1, false),
  (8, 'F-08', 'Seguimiento de desempeño individual', 'REGISTRO',
   'Me lleno una vez por semana y nadie me lee. Igual sigo acá, esperando que algún jefe me abra. Tengo más columnas que respuestas.',
   '2016-05-01', 25, 1, 'una planilla prolija para la evaluación anual', false, NULL, NULL, 1, 1, NULL, 'EN_TINDER', 0, false),
  (9, 'F-09', 'Matriz de comunicación', 'REGISTRO',
   'Dicen que soy burocrática. Dame una oportunidad.',
   '2022-04-01', 15, 1, 'saber a quién avisarle antes de romper algo', false, NULL, 7, 7, 1, NULL, 'EN_TINDER', 0, false),
  (10, 'F-23', 'Planilla de seguimiento semanal', 'REGISTRO',
   'Existo desde 2015. Nadie sabe quién me creó ni para qué sirvo, pero todos los lunes me completan por las dudas.',
   '2015-03-01', 10, 4, 'la sensación de que alguien controla algo', false, NULL, NULL, 2, 1, NULL, 'PENDIENTE_VELORIO', 0, false),
  (11, 'PR-07', 'Atención de reclamos presenciales', 'PROCEDIMIENTO',
   'Soy la cara visible de la Defensoría. Escucho, anoto y derivo. Si me salteás, el reclamo se pierde en un cajón.',
   '2023-03-15', 12, 20, 'que ningún reclamo quede en el aire', false, NULL, 19, 20, 1, NULL, 'EN_TINDER', 0, false),
  (12, 'F-02', 'Libro de novedades en papel', 'REGISTRO',
   'Fui el diario íntimo de la mesa de ayuda. Tinta azul, letra apurada y café derramado.',
   '2014-02-01', 10, 5, 'tener todo anotado (en algún cajón)', false, NULL, NULL, 12, 1, NULL, 'VELADO', 0, false);

INSERT INTO procedimiento_area (procedimiento_id, area_id) VALUES
  (1, 2), (2, 2), (2, 3), (3, 1), (3, 2), (3, 3), (4, 2), (4, 3), (5, 1), (5, 3), (6, 1),
  (7, 1), (7, 3), (8, 1), (9, 2), (10, 1), (10, 2), (10, 3), (11, 4), (12, 1), (12, 3);

-- Matriz de participación realista: no todos usan todo (la Dirección no vota)
INSERT INTO matriz_participacion (usuario_id, procedimiento_id, alta) VALUES
  -- PR-01: Desarrollo
  (5, 1, '2026-09-28'), (6, 1, '2026-09-28'), (7, 1, '2026-09-28'), (8, 1, '2026-09-28'), (15, 1, '2026-09-28'),
  -- PR-02: Desarrollo, Infraestructura y dos de Operaciones que restauran archivos
  (5, 2, '2026-01-10'), (6, 2, '2026-01-10'), (7, 2, '2026-01-10'), (8, 2, '2026-01-10'), (15, 2, '2026-01-10'),
  (9, 2, '2026-01-10'), (10, 2, '2026-01-10'), (11, 2, '2026-01-10'), (12, 2, '2026-01-10'), (16, 2, '2026-01-10'),
  (4, 2, '2026-01-10'), (13, 2, '2026-01-10'),
  -- PR-03: las tres áreas que atienden urgencias
  (1, 3, '2026-06-20'), (2, 3, '2026-06-20'), (3, 3, '2026-06-20'), (4, 3, '2026-06-20'), (13, 3, '2026-06-20'),
  (17, 3, '2026-06-20'), (18, 3, '2026-06-20'), (5, 3, '2026-06-20'), (6, 3, '2026-06-20'), (7, 3, '2026-06-20'),
  (8, 3, '2026-06-20'), (15, 3, '2026-06-20'), (9, 3, '2026-06-20'), (10, 3, '2026-06-20'), (11, 3, '2026-06-20'),
  (12, 3, '2026-06-20'), (16, 3, '2026-06-20'),
  -- PR-04: los que tocan código o servidores
  (5, 4, '2026-09-28'), (6, 4, '2026-09-28'), (8, 4, '2026-09-28'), (15, 4, '2026-09-28'),
  (9, 4, '2026-09-28'), (10, 4, '2026-09-28'), (11, 4, '2026-09-28'), (16, 4, '2026-09-28'),
  -- PR-05: los que evalúan proveedores
  (1, 5, '2026-06-20'), (2, 5, '2026-06-20'), (3, 5, '2026-06-20'), (13, 5, '2026-06-20'), (9, 5, '2026-06-20'), (10, 5, '2026-06-20'),
  -- PR-06: Operaciones
  (1, 6, '2026-01-10'), (2, 6, '2026-01-10'), (3, 6, '2026-01-10'), (4, 6, '2026-01-10'), (13, 6, '2026-01-10'),
  (17, 6, '2026-01-10'), (18, 6, '2026-01-10'),
  -- F-07: la mesa, Infraestructura (que cierra tickets) y Paula, que cubre guardias
  (1, 7, '2026-06-20'), (2, 7, '2026-06-20'), (3, 7, '2026-06-20'), (4, 7, '2026-06-20'), (13, 7, '2026-06-20'),
  (17, 7, '2026-06-20'), (18, 7, '2026-06-20'), (9, 7, '2026-06-20'), (10, 7, '2026-06-20'), (11, 7, '2026-06-20'),
  (12, 7, '2026-06-20'), (16, 7, '2026-06-20'), (15, 7, '2026-06-20'),
  -- F-08: los que lo completan
  (1, 8, '2026-09-28'), (2, 8, '2026-09-28'), (3, 8, '2026-09-28'), (17, 8, '2026-09-28'),
  -- F-09
  (5, 9, '2026-09-28'), (6, 9, '2026-09-28'), (7, 9, '2026-09-28'), (8, 9, '2026-09-28'),
  -- F-23: todos los lunes, todo el mundo
  (1, 10, '2026-06-20'), (2, 10, '2026-06-20'), (3, 10, '2026-06-20'), (4, 10, '2026-06-20'), (13, 10, '2026-06-20'),
  (17, 10, '2026-06-20'), (18, 10, '2026-06-20'), (5, 10, '2026-06-20'), (6, 10, '2026-06-20'), (7, 10, '2026-06-20'),
  (8, 10, '2026-06-20'), (15, 10, '2026-06-20'), (9, 10, '2026-06-20'), (10, 10, '2026-06-20'), (11, 10, '2026-06-20'),
  (12, 10, '2026-06-20'), (16, 10, '2026-06-20'),
  -- PR-07: Atención al Público
  (19, 11, '2026-09-28'), (20, 11, '2026-09-28'), (21, 11, '2026-09-28'), (22, 11, '2026-09-28');

INSERT INTO matriz_revision (fecha, usuario_id, altas, bajas) VALUES ('2026-09-28', 13, 21, 6);

-- Ciclos: tres cerrados y uno abierto
INSERT INTO ciclo_votacion (id, nombre, fecha_inicio, fecha_fin, estado, tipo) VALUES
  (1, '2026-T1', '2026-01-01', '2026-03-31', 'CERRADO', 'TRIMESTRAL'),
  (2, '2026-T2', '2026-04-01', '2026-06-30', 'CERRADO', 'TRIMESTRAL'),
  (3, '2026-T3', '2026-07-01', '2026-09-30', 'CERRADO', 'TRIMESTRAL'),
  (4, '2026-T4', '2026-10-01', '2026-12-31', 'ABIERTO', 'TRIMESTRAL');

-- ---------------------------------------------------------------------
-- ANONIMATO: voto_emitido dice QUIÉN votó, voto dice QUÉ se votó y objecion POR QUÉ objetó.
-- Ninguna tiene columnas que las unan, ni usuario ni timestamp en voto / objecion.
-- ---------------------------------------------------------------------

-- 2026-T1: PR-06 (Operaciones) 4 de 4 => CASAMIENTO
INSERT INTO voto_emitido (usuario_id, procedimiento_id, ciclo_id) VALUES (1, 6, 1), (2, 6, 1), (3, 6, 1), (4, 6, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 6, 1, 1, 'LIKE' FROM generate_series(1, 4);

-- 2026-T2: PR-02 40 % / 40 % => SEGUNDA_CITA (primera)
INSERT INTO voto_emitido (usuario_id, procedimiento_id, ciclo_id) VALUES
  (5, 2, 2), (6, 2, 2), (7, 2, 2), (8, 2, 2), (15, 2, 2), (9, 2, 2), (10, 2, 2), (11, 2, 2), (12, 2, 2), (16, 2, 2);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 2, 2, 'LIKE' FROM generate_series(1, 2);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 2, 2, 'NOPE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 2, 3, 'LIKE' FROM generate_series(1, 2);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 2, 3, 'NOPE' FROM generate_series(1, 3);

-- 2026-T3 -------------------------------------------------------------
INSERT INTO voto_emitido (usuario_id, procedimiento_id, ciclo_id) VALUES
  -- PR-01: 2 de 5 (participación insuficiente)
  (5, 1, 3), (7, 1, 3),
  -- PR-02: 11 de 12
  (5, 2, 3), (7, 2, 3), (8, 2, 3), (15, 2, 3), (9, 2, 3), (10, 2, 3), (11, 2, 3), (12, 2, 3), (16, 2, 3), (4, 2, 3), (13, 2, 3),
  -- PR-03: 16 de 17
  (1, 3, 3), (2, 3, 3), (3, 3, 3), (4, 3, 3), (13, 3, 3), (17, 3, 3), (18, 3, 3),
  (5, 3, 3), (7, 3, 3), (8, 3, 3), (15, 3, 3), (9, 3, 3), (10, 3, 3), (11, 3, 3), (12, 3, 3), (16, 3, 3),
  -- PR-05: 6 de 6
  (1, 5, 3), (2, 5, 3), (3, 5, 3), (13, 5, 3), (9, 5, 3), (10, 5, 3),
  -- F-07: 11 de 13
  (1, 7, 3), (2, 7, 3), (3, 7, 3), (4, 7, 3), (17, 7, 3), (9, 7, 3), (10, 7, 3), (11, 7, 3), (12, 7, 3), (16, 7, 3), (15, 7, 3),
  -- F-23: 11 de 17
  (1, 10, 3), (2, 10, 3), (3, 10, 3), (4, 10, 3), (5, 10, 3), (7, 10, 3), (8, 10, 3), (15, 10, 3), (9, 10, 3), (10, 10, 3), (11, 10, 3);

-- PR-01: Desarrollo 1 / 1
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 1, 3, 2, 'LIKE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 1, 3, 2, 'NOPE' FROM generate_series(1, 1);
-- PR-02: Desarrollo 1/4 (25 %), Infraestructura 1/5 (20 %), Operaciones 2/2 (sin quórum) => 36 %
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 3, 2, 'LIKE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 3, 2, 'NOPE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 3, 3, 'LIKE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 3, 3, 'NOPE' FROM generate_series(1, 4);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 2, 3, 1, 'LIKE' FROM generate_series(1, 2);
-- PR-03: Operaciones 6/7 (86 %), Desarrollo 3/4 (75 %), Infraestructura 4/5 (80 %)
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 3, 3, 1, 'LIKE' FROM generate_series(1, 6);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 3, 3, 1, 'NOPE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 3, 3, 2, 'LIKE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 3, 3, 2, 'NOPE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 3, 3, 3, 'LIKE' FROM generate_series(1, 4);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 3, 3, 3, 'NOPE' FROM generate_series(1, 1);
-- PR-05: Operaciones 3/4 (75 %), Infraestructura 2/2 (sin quórum)
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 5, 3, 1, 'LIKE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 5, 3, 1, 'NOPE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 5, 3, 3, 'LIKE' FROM generate_series(1, 2);
-- F-07: Operaciones 3/5 (60 %), Infraestructura 2/5 (40 %), Desarrollo 1/1 (sin quórum) => 55 %
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 7, 3, 1, 'LIKE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 7, 3, 1, 'NOPE' FROM generate_series(1, 2);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 7, 3, 3, 'LIKE' FROM generate_series(1, 2);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 7, 3, 3, 'NOPE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 7, 3, 2, 'LIKE' FROM generate_series(1, 1);
-- F-23: Operaciones 1/4, Desarrollo 1/4, Infraestructura 0/3 => 18 %
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 10, 3, 1, 'LIKE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 10, 3, 1, 'NOPE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 10, 3, 2, 'LIKE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 10, 3, 2, 'NOPE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 10, 3, 3, 'NOPE' FROM generate_series(1, 3);

-- Resultados por área de los ciclos cerrados
INSERT INTO resultado_ciclo (ciclo_id, procedimiento_id, area_id, likes, nopes, total, pct_like, veredicto) VALUES
  (1, 6, 1, 4, 0, 4, 1.0000, 'CASAMIENTO'),
  (2, 2, 2, 2, 3, 5, 0.4000, 'SEGUNDA_CITA'),
  (2, 2, 3, 2, 3, 5, 0.4000, 'SEGUNDA_CITA'),
  (3, 1, 2, 1, 1, 2, 0.5000, 'QUORUM_INSUFICIENTE'),
  (3, 2, 1, 2, 0, 2, 1.0000, 'QUORUM_INSUFICIENTE'),
  (3, 2, 2, 1, 3, 4, 0.2500, 'VELORIO'),
  (3, 2, 3, 1, 4, 5, 0.2000, 'VELORIO'),
  (3, 3, 1, 6, 1, 7, 0.8571, 'CASAMIENTO'),
  (3, 3, 2, 3, 1, 4, 0.7500, 'CASAMIENTO'),
  (3, 3, 3, 4, 1, 5, 0.8000, 'CASAMIENTO'),
  (3, 5, 1, 3, 1, 4, 0.7500, 'CASAMIENTO'),
  (3, 5, 3, 2, 0, 2, 1.0000, 'QUORUM_INSUFICIENTE'),
  (3, 7, 1, 3, 2, 5, 0.6000, 'SEGUNDA_CITA'),
  (3, 7, 2, 1, 0, 1, 1.0000, 'QUORUM_INSUFICIENTE'),
  (3, 7, 3, 2, 3, 5, 0.4000, 'SEGUNDA_CITA'),
  (3, 10, 1, 1, 3, 4, 0.2500, 'VELORIO'),
  (3, 10, 2, 1, 3, 4, 0.2500, 'VELORIO'),
  (3, 10, 3, 0, 3, 3, 0.0000, 'VELORIO');

-- Resultados por procedimiento
INSERT INTO resultado_procedimiento (ciclo_id, procedimiento_id, pct_global, votantes, habilitados, destino, relacion_a_distancia, grupal, mensaje) VALUES
  (1, 6, 1.0000, 4, 7, 'CASAMIENTO', false, false, '¡Es un match! Preparen la libreta.'),
  (2, 2, 0.4000, 10, 12, 'SEGUNDA_CITA', false, false, 'Votos divididos: segunda cita para escuchar a la minoría.'),
  (3, 1, 0.5000, 2, 5, 'PARTICIPACION_INSUFICIENTE', false, false, 'Votó el 40 % de la matriz (mínimo 50 %): queda como estaba.'),
  (3, 2, 0.3636, 11, 12, 'SEGUNDA_CITA', false, false,
   'Obligatorio por norma: se simplifica, no se vela (ISO 9001 7.1.3 / continuidad). Ya van 2 segundas citas seguidas: requiere reformulación.'),
  (3, 3, 0.8125, 16, 17, 'CASAMIENTO', false, true,
   'Casamiento grupal: 70 % o más en Desarrollo, Infraestructura, Operaciones y Mesa de Ayuda.'),
  (3, 5, 0.8333, 6, 6, 'CASAMIENTO', false, false, '¡Es un match! Preparen la libreta.'),
  (3, 7, 0.5455, 11, 13, 'SEGUNDA_CITA', false, false, 'Votos divididos: segunda cita para escuchar a la minoría.'),
  (3, 10, 0.1818, 11, 17, 'VELORIO', false, false, 'Menos del 40 % en todas las áreas: vayan comprando las flores.');

INSERT INTO alerta (procedimiento_id, ciclo_id, tipo, mensaje, revisada, created_at) VALUES
  (3, 3, 'CANDIDATO_CASAMIENTO_GRUPAL', 'PR-03: Casamiento grupal: 70 % o más en Desarrollo, Infraestructura, Operaciones y Mesa de Ayuda.', false, '2026-09-30 18:00'),
  (2, 3, 'OBLIGATORIO_NO_VELABLE', 'PR-02: Obligatorio por norma: se simplifica, no se vela (ISO 9001 7.1.3 / continuidad).', true, '2026-09-30 18:00');

-- Objeciones (anónimas: sin usuario ni timestamp)
INSERT INTO objecion (id, procedimiento_id, ciclo_id, area_id, origen, categoria, texto, fundada, respuesta, estado, evaluada_por) VALUES
  ('a1000000-0000-4000-8000-000000000001', 2, 3, 2, 'VOTO', 'NO_SE_PUEDE_EN_URGENCIAS',
   'Si se cae el servidor a las 3 de la mañana nadie va a leer 12 pasos', true,
   'Tiene razón: se arma un checklist de restauración de una página.', 'RESPONDIDA', 13),
  ('a1000000-0000-4000-8000-000000000002', 2, 3, 3, 'VOTO', 'DEMASIADOS_PASOS',
   'Para restaurar un archivo hacen falta tres aprobaciones', NULL, NULL, 'PENDIENTE', NULL),
  ('a1000000-0000-4000-8000-000000000003', 5, 3, 3, 'VOTO', 'NO_APLICA_A_MI_TAREA',
   'En Infraestructura no contratamos proveedores de servicios', false,
   'Infraestructura evalúa solo a los proveedores de hardware: se aclara en el alcance.', 'RESPONDIDA', 13),
  ('a1000000-0000-4000-8000-000000000004', 6, 1, 1, 'VOTO', 'OTRO',
   '¿Y si los avisos los manda un bot?', false,
   'Buena idea, pero hoy no hay presupuesto. Se revisa en el aniversario.', 'RESPONDIDA', 13);
INSERT INTO objecion (id, procedimiento_id, ciclo_id, area_id, origen, categoria, texto, estado)
  SELECT gen_random_uuid(), 7, 3, a, 'VOTO', 'DEMASIADOS_PASOS', 'Son 5 preguntas, nadie las contesta', 'PENDIENTE'
  FROM unnest(ARRAY[1, 1, 3, 3]) AS a;
INSERT INTO objecion (id, procedimiento_id, ciclo_id, area_id, origen, categoria, texto, estado) VALUES
  (gen_random_uuid(), 10, 3, 1, 'VOTO', 'DUPLICA_OTRO_REGISTRO', 'Es lo mismo que ya cargamos en el sistema de tickets', 'PENDIENTE'),
  (gen_random_uuid(), 10, 3, 1, 'VOTO', 'DUPLICA_OTRO_REGISTRO', 'Copiamos y pegamos del sistema de tickets todos los lunes', 'PENDIENTE'),
  (gen_random_uuid(), 10, 3, 2, 'VOTO', 'DUPLICA_OTRO_REGISTRO', 'Está todo en el tablero del equipo', 'PENDIENTE'),
  (gen_random_uuid(), 10, 3, 2, 'VOTO', 'DUPLICA_OTRO_REGISTRO', NULL, 'PENDIENTE'),
  (gen_random_uuid(), 10, 3, 3, 'VOTO', 'DUPLICA_OTRO_REGISTRO', 'Duplica el parte semanal de Infraestructura', 'PENDIENTE'),
  (gen_random_uuid(), 10, 3, 3, 'VOTO', 'DUPLICA_OTRO_REGISTRO', 'Ya sale en el informe mensual', 'PENDIENTE'),
  (gen_random_uuid(), 8, 4, 1, 'VOTO', 'DESACTUALIZADO', 'Tiene columnas de 2016', 'PENDIENTE');

-- Segundas citas: la de PR-02 en T2 (ya realizada) y las convocadas al cerrar T3
INSERT INTO segunda_cita (id, procedimiento_id, ciclo_id, fecha_programada, estado, minuta, decision, condiciones) VALUES
  (1, 2, 2, '2026-07-07', 'REALIZADA',
   'Se escuchó a la minoría: el backup diario tiene pasos que nadie entiende. Julián separa el backup de la restauración y lo vuelve a presentar en T3.',
   'AJUSTAR_Y_REVOTAR', NULL),
  (2, 7, 3, CURRENT_DATE + 2, 'CONVOCADA', NULL, NULL, NULL),
  (3, 2, 3, CURRENT_DATE + 4, 'CONVOCADA', NULL, NULL, NULL);

-- Casamiento vigente de PR-06, con aniversario en 6 días (aparece en /aniversarios)
INSERT INTO casamiento (id, numero, procedimiento_id, version_procedimiento, fecha, votos, padrino_id, dueno_id, oficiante_id,
                        grupal, condiciones, fecha_aniversario, estado) VALUES
  (1, 'L-011/' || EXTRACT(YEAR FROM (CURRENT_DATE + 6 - INTERVAL '6 months'))::INT, 6, 1,
   (CURRENT_DATE + 6 - INTERVAL '6 months')::DATE,
   'Prometemos responder en tiempo y forma, avisar antes de incumplir y no esconder los indicadores abajo de la alfombra.',
   2, 4, 13, false, NULL, CURRENT_DATE + 6, 'VIGENTE');
INSERT INTO casamiento_area (casamiento_id, area_id) VALUES (1, 1);
INSERT INTO casamiento_testigo (casamiento_id, usuario_id) VALUES (1, 19);
INSERT INTO casamiento_resultado (casamiento_id, area_id, pct) VALUES (1, 1, 1.0000);

-- Archivo de obsoletos: F-02, velado con sus tres firmas
INSERT INTO velorio (id, numero_acta, procedimiento_id, fecha, causa, epitafio, minutos_semanales_perdidos, deudos, herencia,
                     lecciones, reencarnacion_id, sin_reemplazo_motivo, firma_juez_por, firma_juez_fecha,
                     firma_direccion_por, firma_direccion_fecha, firma_dueno_por, firma_dueno_fecha) VALUES
  (1, '003/2026', 12, '2026-08-20',
   '12% de aprobación y 3 objeciones. Lo que más se repitió: duplica otro registro (3).',
   'Nos robó 50 minutos por semana', 50, 'nadie',
   'Las novedades se cargan en el sistema de tickets, con fecha y responsable.',
   'Si algo se completa "por las dudas", no se completa.', NULL,
   'Lo reemplaza el sistema de tickets, que ya existía.',
   13, '2026-08-21 10:00', 14, '2026-08-22 12:30', 12, '2026-08-21 16:15');

-- 2026-T4 (ABIERTO): algunos ya votaron. Facundo todavía no.
INSERT INTO voto_emitido (usuario_id, procedimiento_id, ciclo_id) VALUES
  (5, 4, 4), (8, 4, 4), (9, 4, 4), (10, 4, 4),
  (1, 8, 4), (2, 8, 4), (3, 8, 4),
  (19, 11, 4), (20, 11, 4), (21, 11, 4),
  (7, 9, 4);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 4, 4, 2, 'LIKE' FROM generate_series(1, 2);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 4, 4, 3, 'LIKE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 4, 4, 3, 'NOPE' FROM generate_series(1, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 8, 4, 1, 'NOPE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 11, 4, 4, 'LIKE' FROM generate_series(1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) SELECT gen_random_uuid(), 9, 4, 2, 'LIKE' FROM generate_series(1, 1);

-- Tablero de mejoras en distintos estados
INSERT INTO mejora (id, codigo, origen, origen_id, procedimiento_id, descripcion, responsable_id, fecha_compromiso, estado, respuesta) VALUES
  (1, 'M-001', 'OBJECION', 'a1000000-0000-4000-8000-000000000001', 2,
   'Atender objeción fundada (No se puede en urgencias): checklist de restauración de una página.', 10, CURRENT_DATE + 10, 'EN_CURSO', NULL),
  (2, 'M-002', 'OBJECION', 'a1000000-0000-4000-8000-000000000003', 5,
   'Contarle al equipo por qué no se cambia (No aplica a mi tarea): aclarar el alcance para Infraestructura.', 18, '2026-10-30', 'RESUELTA',
   'Se aclaró el alcance en la bio del procedimiento y se avisó en la reunión de Infraestructura.'),
  (3, 'M-003', 'SEGUNDA_CITA', '1', 2,
   'Ajustar PR-02 según la minuta de la segunda cita: separar el backup de la restauración.', 10, '2026-08-07', 'RESUELTA',
   'Versión 2 publicada: el backup diario quedó en 3 pasos.'),
  (4, 'M-004', 'ANIVERSARIO', '1', 6,
   'Mostrar el semáforo de SLA en la cartelera de la mesa de ayuda.', 4, CURRENT_DATE - 3, 'EN_ANALISIS', NULL),
  (5, 'M-005', 'REFORMULACION', '2', 2,
   'Reformular PR-02: 2 segundas citas seguidas. Así como está no enamora a nadie.', 10, CURRENT_DATE + 20, 'NUEVA', NULL),
  (6, 'M-006', 'OBJECION', 'a1000000-0000-4000-8000-000000000004', 6,
   'Contarle al equipo por qué no se cambia (Otro): avisos automáticos con un bot.', 4, '2026-04-30', 'DESCARTADA',
   'Sin presupuesto para el bot este año. Se vuelve a mirar en el aniversario.');

-- Sincronizar secuencias después de insertar ids explícitos
SELECT setval(pg_get_serial_sequence('area', 'id'), (SELECT MAX(id) FROM area));
SELECT setval(pg_get_serial_sequence('usuario', 'id'), (SELECT MAX(id) FROM usuario));
SELECT setval(pg_get_serial_sequence('procedimiento', 'id'), (SELECT MAX(id) FROM procedimiento));
SELECT setval(pg_get_serial_sequence('ciclo_votacion', 'id'), (SELECT MAX(id) FROM ciclo_votacion));
SELECT setval(pg_get_serial_sequence('segunda_cita', 'id'), (SELECT MAX(id) FROM segunda_cita));
SELECT setval(pg_get_serial_sequence('casamiento', 'id'), (SELECT MAX(id) FROM casamiento));
SELECT setval(pg_get_serial_sequence('velorio', 'id'), (SELECT MAX(id) FROM velorio));
SELECT setval(pg_get_serial_sequence('mejora', 'id'), (SELECT MAX(id) FROM mejora));
SELECT setval('mejora_codigo_seq', (SELECT COUNT(*) FROM mejora));
