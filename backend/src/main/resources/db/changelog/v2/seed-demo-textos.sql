-- =====================================================================
-- Textos formales de los procedimientos del seed demo (changeset 301).
-- "beneficio" completa la frase "Te doy ..." de la tarjeta.
-- =====================================================================

UPDATE procedimiento SET
  bio = 'Ordeno el ciclo de vida de cada cambio de software: requerimiento registrado, desarrollo, pruebas y pase a producción con aprobación. Los pases se planifican en ventanas acordadas, nunca a último momento.',
  beneficio = 'cambios trazables y probados antes de llegar a producción'
WHERE codigo = 'PR-01';

UPDATE procedimiento SET
  bio = 'Defino qué información se respalda, con qué frecuencia y cómo se restaura. Mantengo copias en dos ubicaciones distintas y pruebas de restauración periódicas. La norma de continuidad me exige.',
  beneficio = 'la información crítica recuperable ante una falla o un incidente'
WHERE codigo = 'PR-02';

UPDATE procedimiento SET
  nombre = 'Atención de peticiones urgentes',
  bio = 'Recibo los pedidos que no pueden esperar la cola habitual. Los clasifico por impacto y cantidad de personas afectadas, asigno un responsable y exijo el registro posterior. Intervengo en Operaciones, Desarrollo e Infraestructura.',
  beneficio = 'un criterio único de prioridad cuando todo parece urgente',
  version_express = E'1. Comunicarse con la mesa de ayuda (interno 4000).\n2. Indicar qué servicio falla y cuántas personas están afectadas.\n3. Si afecta a más de 10 personas, se atiende de inmediato y se documenta dentro de las 24 h.'
WHERE codigo = 'PR-03';

UPDATE procedimiento SET
  bio = 'Registro cada versión del código y de la configuración de los servidores: qué cambió, cuándo, quién lo hizo y por qué. Pido mensajes de commit claros y una etiqueta en cada entrega.',
  beneficio = 'poder reconstruir cualquier versión y saber quién la aprobó'
WHERE codigo = 'PR-04';

UPDATE procedimiento SET
  bio = 'Evalúo una vez por año a los proveedores con criterios de calidad, plazos y soporte. El resultado define renovaciones y advertencias. Infraestructura participa solo en la evaluación de los proveedores de hardware.',
  beneficio = 'proveedores evaluados con evidencia documentada'
WHERE codigo = 'PR-05';

UPDATE procedimiento SET
  bio = 'Fijo tiempos de respuesta y de resolución según la criticidad de cada pedido y los mido con indicadores mensuales. Si un plazo no se va a cumplir, se avisa antes de que venza.',
  beneficio = 'que el usuario sepa en qué plazo se resuelve su pedido',
  version_express = 'Plazos de resolución: crítico 4 h, alto 1 día hábil, resto 3 días hábiles. Si el plazo no se va a cumplir, avisar al usuario antes del vencimiento.'
WHERE codigo = 'PR-06';

UPDATE procedimiento SET
  nombre = 'Encuesta de satisfacción de la mesa de ayuda',
  bio = 'Soy una encuesta de cinco preguntas que se envía al cerrar cada ticket. Mido la satisfacción del usuario con la atención recibida y alimento los indicadores de la mesa de ayuda.',
  beneficio = 'una medida concreta de la satisfacción de los usuarios'
WHERE codigo = 'F-07';

UPDATE procedimiento SET
  bio = 'Registro cada semana las tareas y los resultados de cada integrante de Operaciones para la evaluación anual. Mi diseño es de 2016 y no se revisó desde entonces.',
  beneficio = 'información de respaldo para la evaluación de desempeño'
WHERE codigo = 'F-08';

UPDATE procedimiento SET
  bio = 'Indico a quién hay que informar, por qué canal y con cuánta anticipación antes de un cambio o de una interrupción del servicio.',
  beneficio = 'que las áreas afectadas se enteren antes de un cambio'
WHERE codigo = 'F-09';

UPDATE procedimiento SET
  bio = 'Me completan todos los lunes desde 2015 con el avance de las tareas de cada área. No tengo un responsable original documentado y mi contenido se superpone con el sistema de tickets.',
  beneficio = 'un resumen semanal del avance de las áreas'
WHERE codigo = 'F-23';

UPDATE procedimiento SET
  bio = 'Ordeno la recepción de reclamos presenciales en la Defensoría: escucha, registro con número, derivación al área competente y seguimiento hasta la respuesta.',
  beneficio = 'que cada reclamo tenga número, responsable y respuesta'
WHERE codigo = 'PR-07';

UPDATE procedimiento SET
  bio = 'Fui el registro manuscrito de las novedades de la mesa de ayuda. Mi información se cargaba dos veces y hoy la reemplaza el sistema de tickets.',
  beneficio = 'un registro de las novedades de cada turno'
WHERE codigo = 'F-02';
