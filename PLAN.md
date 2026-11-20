# PLAN v2 · Tinder de procedimientos

Actualización de la app a la versión 2 del diseño (devolución de la cátedra).

## 1. Qué había en el repo (nombres reales)

| Pieza | Nombre real en v1 | Nota |
|---|---|---|
| Paquete backend | `ar.gob.defensoria.tinder` (`config`, `dominio`, `repositorio`, `servicio`, `web`, `web/dto`, `excepcion`) | Spring Boot 3.3.13, Java 21 |
| Roles | enum `Rol { EMPLEADO, CALIDAD, DIRECCION }`, `CHECK ck_usuario_rol` | |
| Quién votó | tabla `participacion(usuario_id, procedimiento_id, ciclo_id)`, entidad `Participacion` | → `voto_emitido` |
| Qué se votó | tabla `voto(id UUID, procedimiento_id, ciclo_id, area_id, valor)`, entidad `Voto` | sin usuario ni fecha |
| Veredicto por área | `resultado_ciclo.veredicto ∈ {MATCH, RECHAZO, SEGUNDA_CITA, QUORUM_INSUFICIENTE}` | |
| Umbrales | `tinder.umbral-match=0.60`, `tinder.umbral-rechazo=0.60`, `tinder.quorum-minimo=3` (`TinderProperties`) | |
| Estados | `EstadoProcedimiento { EN_TINDER, SEGUNDA_CITA, PENDIENTE_CASAMIENTO, CASADO, PENDIENTE_VELORIO, VELADO }` | máquina en `MaquinaEstados` |
| Ciclos | `ciclo_votacion` + índice parcial `ux_ciclo_un_solo_abierto` | |
| Casamiento | `casamiento` (+ `casamiento_area`), oficiante DIRECCION | |
| Velorio | `velorio(causa_muerte, epitafio, …, acta_firmada, firmado_por, fecha_firma)` | una firma |
| Alertas | `alerta` (relación a distancia, obligatorio no velable, casamiento grupal) | se mantiene |
| Liquibase | `db/changelog/db.changelog-master.yaml` + `changes/001…012` + `100-seed` (`seed/seed.sql`, sin contexto) | no se tocan |
| Tests | JUnit 5 + Mockito, sin base: `ClasificadorVeredictoTest`, `MaquinaEstadosTest`, `CicloServiceTest`, `TinderServiceTest`, `VotoAnonimatoTest` | |
| Frontend | Angular 19 standalone, estética propia (marco de teléfono, hojas inferiores, sin componentes Material). Páginas: `login`, `shell`, `votar`, `resultados`, `registro` (casamientos/velorios/aniversarios en pestañas), `gestion` (procedimientos/ciclos), `shared/documento` (libreta y necrológica) | |
| Rutas | `/votar` (alias `/swipe`), `/resultados`, `/registro`, `/gestion` (CALIDAD) | |

## 2. Cambios por archivo

### Fase 1 · Modelo de datos (changesets nuevos en `db/changelog/v2/`)

- `db.changelog-master.yaml`: solo se agregan `include` de `v2/…` al final (los changesets viejos quedan intactos).
- `v2/200-roles.yaml`: `CHECK` nuevo de rol; `UPDATE usuario SET rol='JUEZ_PAZ' WHERE rol='CALIDAD'`; tabla `designacion_juez` + índice único parcial `(area_id) WHERE hasta IS NULL`; designación inicial de los jueces migrados.
- `v2/201-procedimiento.yaml`: columnas `dueno_id`, `version`, `reemplaza_a_id`, `segundas_citas_consecutivas`, `requiere_reformulacion`; estado `BORRADOR`; `dueno_id := autor_id`.
- `v2/202-matriz.yaml`: `participacion` → `voto_emitido` (rename de tabla, PK y FKs); tabla `matriz_participacion` poblada con la regla vieja (usuarios del área de cada procedimiento); tabla `matriz_revision` (bitácora de revisiones de la matriz, la usa el calendario).
- `v2/203-objecion.yaml`: tabla `objecion` (id UUID, sin autor y sin timestamp).
- `v2/204-ciclo.yaml`: `tipo` y `procedimiento_id` en `ciclo_votacion`; nombre a 40 caracteres; el índice "un solo abierto" pasa a ser solo para TRIMESTRAL; un único RAPIDA abierto por procedimiento.
- `v2/205-resultados.yaml`: `MATCH→CASAMIENTO`, `RECHAZO→VELORIO` (renombre, sin recalcular); nuevo `CHECK`; tabla `resultado_procedimiento`.
- `v2/206-segunda-cita.yaml`: tabla `segunda_cita`.
- `v2/207-casamiento.yaml`: `numero`, `version_procedimiento`, `dueno_id`, `condiciones`, `informe_aniversario`, estado `POSTERGADO`; tablas `casamiento_testigo` y `casamiento_resultado`; numeración de los casamientos viejos.
- `v2/208-velorio.yaml`: `causa_muerte → causa`, `numero_acta`, `herencia`, `lecciones`, `reencarnacion_id`, `sin_reemplazo_motivo`, tres firmas; migra `acta_firmada/firmado_por/fecha_firma` a la firma de Dirección y después las borra.
- `v2/209-mejora.yaml`: tabla `mejora` + secuencia `mejora_codigo_seq`.
- `v2/300-seed-demo.yaml` (+ `v2/seed-demo.sql`): `context: demo`. Vacía todo y carga el seed v2.
- `v2/301-seed-demo-textos.yaml` (+ `v2/seed-demo-textos.sql`): `context: demo`. Textos formales de los procedimientos (nombre, bio, beneficio, versión express) sin tocar el changeset 300.
- `application.yml`: umbrales nuevos (`umbral-casamiento`, `umbral-velorio`, `quorum-area`, `participacion-minima`), `spring.liquibase.contexts: base`. `application-demo.yml`: `contexts: base,demo`.

Dominio (`dominio/`):
- `Rol`: `JUEZ_PAZ` reemplaza a `CALIDAD`.
- `Participacion` → `VotoEmitido` (tabla `voto_emitido`).
- `Procedimiento`: dueño, versión, reemplaza a, contador de segundas citas, reformulación.
- `EstadoProcedimiento`: + `BORRADOR`. `Veredicto`: `CASAMIENTO, SEGUNDA_CITA, VELORIO, QUORUM_INSUFICIENTE, PARTICIPACION_INSUFICIENTE`.
- `CicloVotacion`: `tipo` (`TipoCiclo`), `procedimiento`.
- `Casamiento`: número, versión, dueño, condiciones, testigos, `CasamientoResultado`, informe; `EstadoCasamiento` + `POSTERGADO`.
- `Velorio`: acta con causa, herencia, lecciones, reencarnación y tres firmas.
- Nuevas: `DesignacionJuez`, `MatrizParticipacion`, `MatrizRevision`, `Objecion` (+ `OrigenObjecion`, `CategoriaObjecion`, `EstadoObjecion`), `ResultadoProcedimiento`, `SegundaCita` (+ `EstadoSegundaCita`, `DecisionSegundaCita`), `Mejora` (+ `OrigenMejora`, `EstadoMejora`), `TipoCiclo`.

### Fase 2 · Reglas de negocio (`servicio/` y `web/`)

- `TinderProperties`: `umbralCasamiento`, `umbralVelorio`, `quorumArea`, `participacionMinima`.
- `ClasificadorVeredicto`: veredicto por área (70/40/quórum) y `evaluar(...)` del procedimiento completo (participación, quórum, grupal, relación a distancia, regla del obligatorio). Devuelve `EvaluacionProcedimiento`.
- `MaquinaEstados`: `BORRADOR → EN_TINDER`; sigue siendo la única que cambia estados (422 si la transición es inválida).
- `TinderService`: mazo por matriz (ciclo trimestral + ciclos rápidos), voto con `cicloId` y objeción opcional en la misma transacción, 403 fuera de la matriz, 400 si objeción con LIKE.
- `CicloService`: cierre trimestral y rápido; `resultado_procedimiento`; segundas citas automáticas; contador y reformulación (mejora REFORMULACION).
- `ObjecionService` (nuevo): agrupadas, evaluar (PATCH), objeción de boda/aniversario.
- `SegundaCitaService` (nuevo): listar, realizar (minuta + decisión), ciclo RAPIDA de 7 días.
- `CasamientoService`: requisitos v2, número de libreta, snapshot por área, testigos, condiciones, informe de aniversario, renovar/segunda cita/divorciar.
- `VelorioService`: acta v2, sugerencias, reencarnación (existente / BORRADOR / sin reemplazo), mejoras automáticas, tres firmas (409 si repite).
- `MejoraService` (nuevo): CRUD, permisos (juez o responsable), respuesta obligatoria al cerrar, código M-001.
- `MatrizService` (nuevo): grilla y guardado por lote.
- `JuezPazService` (nuevo): designaciones con historial y calendario trimestral calculado.
- `ProcedimientoService`: dueño, versión+1 al cambiar contenido, edición del dueño tras AJUSTAR_Y_REVOTAR, publicar BORRADOR, archivo de velados, sin velados en las vistas operativas.
- `ResultadoService`: resultados por procedimiento con sus áreas.
- Controllers nuevos: `ObjecionController`, `SegundaCitaController`, `MejoraController`, `MatrizController`, `JuezPazController`. Todos los `@PreAuthorize` pasan de `CALIDAD` a `JUEZ_PAZ`.

### Fase 3 · Frontend (`frontend/src/app`)

- `core/models.ts`, `core/api.service.ts`, `core/diseno.ts`: tipos, endpoints, colores de destinos y estados nuevos.
- `app.routes.ts`: rutas `/swipe`, `/resultados`, `/casamientos`, `/aniversarios`, `/velorios`, `/archivo`, `/tablero`, `/matriz`, `/objeciones`, `/segundas-citas`, `/juez-de-paz`, `/gestion`, `/mis-procedimientos`; guards con `JUEZ_PAZ`.
- `pages/shell`: navegación por rol (Swipe, Resultados, Registro, Tablero, Juez).
- `pages/votar`: leyenda de matriz, hoja de objeción al hacer NOPE, badge "Votación rápida", dueño y padrino.
- `pages/resultados`: tarjeta por procedimiento con barras por área y líneas guía 40/70, chips de destino, participación, "Casamiento grupal" y "Relación a distancia".
- `pages/registro`: casamientos (form v2, "¿Alguien se opone?", banner POSTERGADO), aniversarios (informe + Renovar/Segunda cita/Divorciar), velorios (acta v2 con reencarnación), archivo de obsoletos.
- `shared/documento`: libreta v2 y "ACTA DE DEFUNCIÓN N.º …" con 3 firmas y botón Firmar según rol.
- Nuevas páginas: `tablero` (kanban), `matriz`, `objeciones`, `segundas-citas` (cronómetro 30 min en 4 bloques), `juez-de-paz` (checklist / designación), `mis-procedimientos` (edición del dueño).
- `pages/gestion`: CALIDAD → JUEZ_PAZ, dueño en el formulario, publicar borradores.

### Fase 4 · Seed demo (`v2/seed-demo.sql`, contexto `demo`)
Usuarios con `demo123`, 4 áreas (se suma Atención al Público para los testigos), matriz realista, 2026-T1 y T2 cerrados (historia de PR-06 y PR-02), 2026-T3 cerrado con PR-03 / F-07 / F-23 / PR-02 / PR-05 (quórum insuficiente en un área) / PR-01 (participación insuficiente), 2026-T4 abierto, mejoras en distintos estados, votos y objeciones sin usuario ni timestamp.

### Fase 5 · Tests
Actualizar los existentes y sumar: umbrales 70/69,9/40/39,9, obligatorio 10 %, relación a distancia 85/20, participación 40 %, voto fuera de matriz 403, objeción con LIKE 400, reflexión sobre `Voto` y `Objecion`, casamiento con objeción fundada sin respuesta 422, oficiante no juez 422, acta con 3 firmas y firma repetida 409, reencarnación BORRADOR → mejora, dos segundas citas → reformulación, mejora DESCARTADA sin respuesta 422, designación de juez.
