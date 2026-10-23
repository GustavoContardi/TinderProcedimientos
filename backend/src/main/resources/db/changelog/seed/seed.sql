-- =====================================================================
-- SEED del Tinder de procedimientos. Contraseña de todos: demo123
-- =====================================================================

-- Áreas
INSERT INTO area (id, nombre) VALUES
  (1, 'Operaciones y Mesa de Ayuda'),
  (2, 'Desarrollo'),
  (3, 'Infraestructura');

-- Usuarios (nombres ficticios). Hash BCrypt de "demo123".
INSERT INTO usuario (id, nombre, email, password_hash, rol, area_id) VALUES
  (1,  'Ana Paredes',       'ana@demo.com',       '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (2,  'Bruno Lescano',     'bruno@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (3,  'Carla Medina',      'carla@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (4,  'Darío Funes',       'dario@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  1),
  (5,  'Elena Quiroga',     'elena@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (6,  'Facundo Ibarra',    'facundo@demo.com',   '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (7,  'Gabriela Sosa',     'gabi@demo.com',      '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (8,  'Hernán Vidal',      'hernan@demo.com',    '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  2),
  (9,  'Inés Barrios',      'ines@demo.com',      '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (10, 'Julián Ortega',     'julian@demo.com',    '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (11, 'Karen Molina',      'karen@demo.com',     '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (12, 'Leandro Ruiz',      'leo@demo.com',       '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'EMPLEADO',  3),
  (13, 'Mónica Calderón',   'calidad@demo.com',   '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'CALIDAD',   1),
  (14, 'Ricardo Ferreyra',  'direccion@demo.com', '$2a$10$HWpWN58dHx7TN3LPqstKqu2t6klqM3zz7oAxQDNu7eoRgVcZP3OEe', 'DIRECCION', 2);

-- Procedimientos (con bios en primera persona)
INSERT INTO procedimiento (id, codigo, nombre, tipo, bio, fecha_creacion, minutos_por_uso, usos_por_semana, beneficio, obligatorio, base_obligatoriedad, autor_id, version_express, estado) VALUES
  (1, 'PR-01', 'Desarrollo y mantenimiento de software', 'PROCEDIMIENTO',
   'Soy metódico, me gustan los tickets bien escritos y odio los cambios en producción un viernes a las 18. Si me seguís, tu código llega sano a producción.',
   '2019-03-01', 30, 5, 'que nadie te culpe cuando se cae el sistema: está todo documentado', false, NULL, 5, NULL, 'EN_TINDER'),
  (2, 'PR-02', 'Backup y recuperación', 'PROCEDIMIENTO',
   'No soy el más divertido, pero cuando todo se prende fuego soy el único que aparece. Busco una relación estable, con copias en dos lugares distintos.',
   '2018-06-15', 20, 5, 'dormir tranquilo aunque explote un disco', true, 'ISO 9001 7.1.3 / continuidad', 10, NULL, 'EN_TINDER'),
  (3, 'PR-03', 'Peticiones urgentes no planificables', 'PROCEDIMIENTO',
   'Llego sin avisar, siempre con apuro, y aun así todos me necesitan. Soy de todas las áreas y no me avergüenza. Si me ordenás, prometo dejar de ser un caos.',
   '2021-09-10', 15, 10, 'un orden de prioridades para cuando el jefe dice que es para ayer', false, NULL, 1, NULL, 'EN_TINDER'),
  (4, 'PR-04', 'Gestión de configuración y versionado', 'PROCEDIMIENTO',
   'Me acuerdo de cada versión que tuviste. Algunos dicen que soy intenso, yo digo que soy trazable. Mi lenguaje del amor es el commit con buen mensaje.',
   '2020-02-20', 10, 15, 'saber qué cambió, cuándo y quién (sin dramas)', false, NULL, 8, NULL, 'EN_TINDER'),
  (5, 'PR-05', 'Evaluación de proveedores', 'PROCEDIMIENTO',
   'Una vez por año evalúo a todos con una planilla de 40 columnas. Juzgo, pero con criterio. Busco a alguien que no me deje para último momento.',
   '2017-11-01', 45, 1, 'proveedores que cumplen (o al menos una prueba de que no cumplieron)', false, NULL, NULL, NULL, 'PENDIENTE_CASAMIENTO'),
  (6, 'PR-06', 'Acuerdos de nivel de servicio', 'PROCEDIMIENTO',
   'Soy de cumplir lo que prometo, y si no, te muestro el indicador. Me encantan los compromisos con fecha y hora.',
   '2019-08-01', 20, 3, 'que el usuario sepa cuándo le resolvemos el problema', false, NULL, 2, 'SLA express: crítico 4 h, alto 1 día, el resto 3 días. Si no llegás, avisá antes.', 'CASADO'),
  (7, 'F-07', 'Encuesta de satisfacción de mesa de ayuda', 'REGISTRO',
   'Tengo 2 años. Te llevo 1 minuto. Busco a alguien que me complete después de cada ticket',
   '2024-10-01', 1, 40, 'saber si la gente se va contenta o se va puteando', false, NULL, 3, NULL, 'EN_TINDER'),
  (8, 'F-08', 'Seguimiento de desempeño individual', 'REGISTRO',
   'Me lleno una vez por semana y nadie me lee. Igual sigo acá, esperando que algún jefe me abra. Tengo más columnas que respuestas.',
   '2016-05-01', 25, 1, 'una planilla prolija para la evaluación anual', false, NULL, NULL, NULL, 'EN_TINDER'),
  (9, 'F-09', 'Matriz de comunicación', 'REGISTRO',
   'Dicen que soy burocrática. Dame una oportunidad',
   '2022-04-01', 15, 1, 'saber a quién avisarle antes de romper algo', false, NULL, 7, NULL, 'SEGUNDA_CITA'),
  (10, 'F-23', 'Planilla de seguimiento semanal', 'REGISTRO',
   'Existo desde 2015. Nadie sabe quién me creó ni para qué sirvo, pero todos los lunes me completan por las dudas.',
   '2015-03-01', 10, 4, 'la sensación de que alguien controla algo', false, NULL, NULL, NULL, 'PENDIENTE_VELORIO');

-- Áreas de cada procedimiento (1=Operaciones, 2=Desarrollo, 3=Infraestructura)
INSERT INTO procedimiento_area (procedimiento_id, area_id) VALUES
  (1, 2),
  (2, 2), (2, 3),
  (3, 1), (3, 2), (3, 3),
  (4, 2), (4, 3),
  (5, 1), (5, 3),
  (6, 1),
  (7, 1),
  (8, 1),
  (9, 2),
  (10, 1), (10, 2), (10, 3);

-- Ciclos: dos cerrados con historia y uno abierto
INSERT INTO ciclo_votacion (id, nombre, fecha_inicio, fecha_fin, estado) VALUES
  (1, '2026-T1', '2026-01-01', '2026-03-31', 'CERRADO'),
  (2, '2026-T3', '2026-07-01', '2026-09-30', 'CERRADO'),
  (3, '2026-T4', '2026-10-01', '2026-12-31', 'ABIERTO');

-- ---------------------------------------------------------------------
-- ANONIMATO: participacion dice QUIÉN votó, voto dice QUÉ se votó.
-- No hay columna que los una (voto no tiene usuario ni timestamp y su id es un UUID aleatorio).
-- ---------------------------------------------------------------------

-- Ciclo 2026-T1: PR-06 (Operaciones) 4 likes => MATCH => se casó
INSERT INTO participacion (usuario_id, procedimiento_id, ciclo_id) VALUES
  (1, 6, 1), (2, 6, 1), (3, 6, 1), (4, 6, 1);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) VALUES
  (gen_random_uuid(), 6, 1, 1, 'LIKE'), (gen_random_uuid(), 6, 1, 1, 'LIKE'),
  (gen_random_uuid(), 6, 1, 1, 'LIKE'), (gen_random_uuid(), 6, 1, 1, 'LIKE');

-- Ciclo 2026-T3
INSERT INTO participacion (usuario_id, procedimiento_id, ciclo_id) VALUES
  (1, 5, 2), (2, 5, 2), (3, 5, 2),
  (1, 10, 2), (2, 10, 2), (3, 10, 2), (4, 10, 2),
  (5, 10, 2), (6, 10, 2), (7, 10, 2), (8, 10, 2),
  (9, 10, 2), (10, 10, 2), (11, 10, 2),
  (5, 9, 2), (6, 9, 2), (7, 9, 2), (8, 9, 2),
  (1, 7, 2), (2, 7, 2);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) VALUES
  (gen_random_uuid(), 10, 2, 3, 'NOPE'), (gen_random_uuid(), 9, 2, 2, 'LIKE'),
  (gen_random_uuid(), 5, 2, 1, 'LIKE'),  (gen_random_uuid(), 10, 2, 1, 'NOPE'),
  (gen_random_uuid(), 7, 2, 1, 'NOPE'),  (gen_random_uuid(), 10, 2, 2, 'NOPE'),
  (gen_random_uuid(), 9, 2, 2, 'NOPE'),  (gen_random_uuid(), 10, 2, 1, 'NOPE'),
  (gen_random_uuid(), 5, 2, 1, 'LIKE'),  (gen_random_uuid(), 10, 2, 2, 'LIKE'),
  (gen_random_uuid(), 10, 2, 3, 'NOPE'), (gen_random_uuid(), 9, 2, 2, 'NOPE'),
  (gen_random_uuid(), 10, 2, 1, 'NOPE'), (gen_random_uuid(), 7, 2, 1, 'LIKE'),
  (gen_random_uuid(), 10, 2, 2, 'NOPE'), (gen_random_uuid(), 5, 2, 1, 'LIKE'),
  (gen_random_uuid(), 10, 2, 3, 'NOPE'), (gen_random_uuid(), 9, 2, 2, 'LIKE'),
  (gen_random_uuid(), 10, 2, 1, 'NOPE'), (gen_random_uuid(), 10, 2, 2, 'NOPE');

-- Resultados de ciclos cerrados (incluye MATCH, RECHAZO, SEGUNDA_CITA y QUORUM_INSUFICIENTE)
INSERT INTO resultado_ciclo (ciclo_id, procedimiento_id, area_id, likes, nopes, total, pct_like, veredicto) VALUES
  (1, 6, 1, 4, 0, 4, 1.0000, 'MATCH'),
  (2, 5, 1, 3, 0, 3, 1.0000, 'MATCH'),
  (2, 5, 3, 0, 0, 0, 0.0000, 'QUORUM_INSUFICIENTE'),
  (2, 10, 1, 0, 4, 4, 0.0000, 'RECHAZO'),
  (2, 10, 2, 1, 3, 4, 0.2500, 'RECHAZO'),
  (2, 10, 3, 0, 3, 3, 0.0000, 'RECHAZO'),
  (2, 9, 2, 2, 2, 4, 0.5000, 'SEGUNDA_CITA'),
  (2, 7, 1, 1, 1, 2, 0.5000, 'QUORUM_INSUFICIENTE');

-- Ciclo 2026-T4 (ABIERTO): algunos compañeros ya votaron. Facundo (Desarrollo) y Leandro (Infraestructura) no.
INSERT INTO participacion (usuario_id, procedimiento_id, ciclo_id) VALUES
  (1, 3, 3), (2, 3, 3), (3, 3, 3), (5, 3, 3), (7, 3, 3), (8, 3, 3), (9, 3, 3), (10, 3, 3), (11, 3, 3),
  (5, 4, 3), (7, 4, 3), (8, 4, 3), (9, 4, 3), (10, 4, 3), (11, 4, 3),
  (5, 2, 3), (7, 2, 3), (8, 2, 3), (9, 2, 3), (10, 2, 3), (11, 2, 3),
  (1, 8, 3), (2, 8, 3), (3, 8, 3),
  (5, 1, 3), (7, 1, 3);
INSERT INTO voto (id, procedimiento_id, ciclo_id, area_id, valor) VALUES
  (gen_random_uuid(), 4, 3, 3, 'LIKE'), (gen_random_uuid(), 3, 3, 2, 'LIKE'),
  (gen_random_uuid(), 2, 3, 3, 'NOPE'), (gen_random_uuid(), 8, 3, 1, 'NOPE'),
  (gen_random_uuid(), 3, 3, 1, 'LIKE'), (gen_random_uuid(), 1, 3, 2, 'LIKE'),
  (gen_random_uuid(), 4, 3, 2, 'NOPE'), (gen_random_uuid(), 3, 3, 3, 'LIKE'),
  (gen_random_uuid(), 2, 3, 2, 'NOPE'), (gen_random_uuid(), 3, 3, 2, 'LIKE'),
  (gen_random_uuid(), 8, 3, 1, 'NOPE'), (gen_random_uuid(), 4, 3, 3, 'LIKE'),
  (gen_random_uuid(), 3, 3, 1, 'LIKE'), (gen_random_uuid(), 2, 3, 3, 'NOPE'),
  (gen_random_uuid(), 4, 3, 2, 'NOPE'), (gen_random_uuid(), 2, 3, 2, 'LIKE'),
  (gen_random_uuid(), 3, 3, 3, 'LIKE'), (gen_random_uuid(), 1, 3, 2, 'LIKE'),
  (gen_random_uuid(), 3, 3, 2, 'LIKE'), (gen_random_uuid(), 8, 3, 1, 'NOPE'),
  (gen_random_uuid(), 4, 3, 3, 'LIKE'), (gen_random_uuid(), 2, 3, 2, 'NOPE'),
  (gen_random_uuid(), 3, 3, 1, 'LIKE'), (gen_random_uuid(), 4, 3, 2, 'NOPE'),
  (gen_random_uuid(), 3, 3, 3, 'LIKE'), (gen_random_uuid(), 2, 3, 3, 'NOPE');

-- Casamiento vigente de PR-06 (aniversario cercano, para la pantalla /aniversarios)
INSERT INTO casamiento (procedimiento_id, fecha, votos, padrino_id, oficiante_id, grupal, fecha_aniversario, estado) VALUES
  (6, '2026-04-30',
   'Prometemos responder en tiempo y forma, avisar antes de incumplir y no esconder los indicadores abajo de la alfombra.',
   2, 14, false, '2026-10-30', 'VIGENTE');
INSERT INTO casamiento_area (casamiento_id, area_id)
  SELECT id, 1 FROM casamiento WHERE procedimiento_id = 6;

-- Velorio de F-23 ya preparado por Calidad, esperando la firma de la Dirección
INSERT INTO velorio (procedimiento_id, fecha, causa_muerte, epitafio, minutos_semanales_perdidos, deudos, acta_firmada) VALUES
  (10, '2026-10-15',
   '10 swipes a la izquierda en el ciclo 2026-T3',
   'Aquí yace F-23 Planilla de seguimiento semanal. Nos robó 40 minutos por semana. No lo vamos a extrañar.',
   40, 'nadie', false);

-- Sincronizar las secuencias de identidad después de insertar ids explícitos
SELECT setval(pg_get_serial_sequence('area', 'id'), (SELECT MAX(id) FROM area));
SELECT setval(pg_get_serial_sequence('usuario', 'id'), (SELECT MAX(id) FROM usuario));
SELECT setval(pg_get_serial_sequence('procedimiento', 'id'), (SELECT MAX(id) FROM procedimiento));
SELECT setval(pg_get_serial_sequence('ciclo_votacion', 'id'), (SELECT MAX(id) FROM ciclo_votacion));
