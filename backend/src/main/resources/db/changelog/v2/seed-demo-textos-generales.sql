-- =====================================================================
-- Textos formales del resto del seed demo (changeset 302).
-- Los mensajes de resultados son los mismos que genera ClasificadorVeredicto.
-- =====================================================================

-- Mensajes de los resultados por procedimiento
UPDATE resultado_procedimiento SET mensaje = 'Aprobado en todas las áreas: pasa a casamiento.'
WHERE mensaje = '¡Es un match! Preparen la libreta.';
UPDATE resultado_procedimiento SET mensaje = 'Votos divididos: se convoca a segunda cita para escuchar a la minoría.'
WHERE mensaje = 'Votos divididos: segunda cita para escuchar a la minoría.';
UPDATE resultado_procedimiento SET mensaje = 'Votó el 40 % de la matriz (mínimo 50 %): se mantiene sin cambios.'
WHERE mensaje = 'Votó el 40 % de la matriz (mínimo 50 %): queda como estaba.';
UPDATE resultado_procedimiento SET mensaje = 'Menos del 40 % en todas las áreas: pasa a velorio.'
WHERE mensaje = 'Menos del 40 % en todas las áreas: vayan comprando las flores.';

-- Objeciones (siguen anónimas: solo cambia la redacción)
UPDATE objecion SET texto = 'Ante una caída del servidor fuera de horario, un procedimiento de 12 pasos no es aplicable.',
  respuesta = 'Objeción fundada: se elaborará un checklist de restauración de una página.'
WHERE id = 'a1000000-0000-4000-8000-000000000001';
UPDATE objecion SET texto = 'Restaurar un archivo requiere tres aprobaciones.'
WHERE id = 'a1000000-0000-4000-8000-000000000002';
UPDATE objecion SET texto = 'Infraestructura no contrata proveedores de servicios.'
WHERE id = 'a1000000-0000-4000-8000-000000000003';
UPDATE objecion SET texto = 'Los avisos podrían enviarse de forma automática.',
  respuesta = 'Es viable, pero este año no hay presupuesto. Se revisará en el aniversario.'
WHERE id = 'a1000000-0000-4000-8000-000000000004';
UPDATE objecion SET texto = 'La encuesta tiene cinco preguntas y la mayoría de los usuarios no la completa.'
WHERE texto = 'Son 5 preguntas, nadie las contesta';
UPDATE objecion SET texto = 'Duplica la información que ya se carga en el sistema de tickets.'
WHERE texto = 'Es lo mismo que ya cargamos en el sistema de tickets';
UPDATE objecion SET texto = 'Todos los lunes se copia a mano la información del sistema de tickets.'
WHERE texto = 'Copiamos y pegamos del sistema de tickets todos los lunes';
UPDATE objecion SET texto = 'La información ya está disponible en el tablero del equipo.'
WHERE texto = 'Está todo en el tablero del equipo';
UPDATE objecion SET texto = 'Duplica el parte semanal de Infraestructura.'
WHERE texto = 'Duplica el parte semanal de Infraestructura';
UPDATE objecion SET texto = 'La información ya figura en el informe mensual.'
WHERE texto = 'Ya sale en el informe mensual';
UPDATE objecion SET texto = 'Conserva columnas definidas en 2016 que ya no se usan.'
WHERE texto = 'Tiene columnas de 2016';

-- Minuta de la segunda cita de PR-02 (2026-T2)
UPDATE segunda_cita SET minuta = 'Se escuchó a la minoría: los pasos del backup diario no resultan claros. Julián Ortega separará el backup de la restauración y lo presentará nuevamente en el ciclo 2026-T3.'
WHERE id = 1;

-- Votos del casamiento de PR-06
UPDATE casamiento SET votos = 'Nos comprometemos a responder en los plazos acordados, a avisar antes de un incumplimiento y a publicar los indicadores de cumplimiento.'
WHERE id = 1;

-- Acta de defunción de F-02
UPDATE velorio SET epitafio = 'Insumía 50 minutos por semana',
  deudos = 'ninguno',
  lecciones = 'Un registro que se completa sin un uso definido no debe mantenerse.'
WHERE id = 1;

-- Tablero de mejoras
UPDATE mejora SET descripcion = 'Informar al equipo por qué no se cambia (No aplica a mi tarea): aclarar el alcance para Infraestructura.',
  respuesta = 'Se aclaró el alcance en la bio del procedimiento y se informó en la reunión de Infraestructura.'
WHERE codigo = 'M-002';
UPDATE mejora SET descripcion = 'Reformular PR-02: 2 segundas citas seguidas sin un resultado claro.'
WHERE codigo = 'M-005';
UPDATE mejora SET descripcion = 'Informar al equipo por qué no se cambia (Otro): envío automático de avisos.',
  respuesta = 'Este año no hay presupuesto para automatizar los avisos. Se revisará en el aniversario.'
WHERE codigo = 'M-006';
