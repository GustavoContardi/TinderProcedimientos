# 💍 Pasá y Rebotá · v2

> *Evaluación participativa de procedimientos.*

Prototipo para la Defensoría del Pueblo (Design Thinking · Sistemas de Información IV).
Maneja el ciclo de vida de los procedimientos de calidad (ISO 9001):

| Etapa | Qué pasa |
|---|---|
| 🔥 **Votación** | Evaluación periódica y **anónima** por quienes usan cada procedimiento (♥ me sirve / ✕ me sobra). La minoría puede **objetar** sin dar su nombre. |
| 💍 **Casamiento** | Aprobación formal con compromiso: libreta con número, versión, % por área, dueño, padrino, testigos, votos, prenup y condiciones. Oficia el **juez de paz**. |
| ☕ **Segunda cita** | Reunión de 30 minutos con minuta y decisión. Nada cambia sin una **votación rápida** anónima de 7 días. |
| 🕯️ **Velorio** | Baja controlada con **acta de defunción**: causa, herencia, lecciones, reencarnación y **tres firmas**. El procedimiento queda obsoleto, nunca se borra. |
| 📋 **Tablero de mejoras** | Toda objeción, acuerdo y acta genera acciones con responsable y fecha. |

---

## Cambios v2

Después de la devolución de la cátedra:

1. **Umbrales 70 / 40** (antes 60 / 60): 70 % o más en todas las áreas → casamiento; menos de 40 % en todas → velorio; cualquier otra combinación → segunda cita.
2. **Matriz de participación**: cada empleado vota **solo los procedimientos en los que participa** (antes votaba todos los de su área). Fuera de la matriz, la API responde 403.
3. **Objeciones de la minoría** con circuito propio: objeción anónima **obligatoria** al votar NOPE: el motivo es requisito para rechazar (o en la boda / el aniversario), agrupación por categoría, segunda cita, decisión y tablero de mejoras.
4. **Juez de paz** (`JUEZ_PAZ`) reemplaza a Calidad y coordina todo el sistema. La **Dirección** deja de oficiar: es *padrino institucional* (firma, revisa y designa al juez, con rotación anual).
5. **Acta de defunción con trazabilidad**: causa, herencia, lecciones, reencarnación (el procedimiento que lo reemplaza, que puede nacer en BORRADOR) y tres firmas (juez, Dirección y dueño).
6. **Tablero de mejoras** (kanban): NUEVA → EN ANÁLISIS → EN CURSO → RESUELTA / DESCARTADA, con respuesta obligatoria al cerrar.
7. **Segunda cita formal**: cronómetro de 30 minutos en 4 bloques, minuta, decisión (ajustar y revotar, casar con condiciones, velorio) y **votación rápida** de 7 días.

El detalle de lo que se cambió en cada archivo está en [PLAN.md](PLAN.md).

---

## Levantar todo

Requisitos: **Docker**, **Java 21** y **Node 20+**.

**Atajo:** `./levantar.sh` levanta todo (base + backend con el perfil `demo` + frontend) y `./bajar.sh` lo baja. Con `--reset` cualquiera de los dos borra la base. Los logs quedan en `.run/`. El script busca solo el JDK 21 aunque tu Java por defecto sea otro.

A mano, paso a paso:

```bash
docker compose up -d
```

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

```bash
cd frontend && npm i && npm start
```

Abrí **http://localhost:4200**.

- **Perfil `demo`**: además de las migraciones v2 corre el seed de la demo (contexto Liquibase `demo`), que **vacía la base y carga los datos del guion**. Sin el perfil, solo se migran los datos que haya (el seed v1 queda migrado a v2, sin recalcular nada).
- Si ya tenías la base de la v1 levantada, alcanza con arrancar el backend con el perfil `demo`: Liquibase aplica los changesets v2 y el seed nuevo una sola vez. Para volver al estado inicial de la demo: `./bajar.sh --reset && ./levantar.sh`.
- API: http://localhost:8080/api · Swagger: http://localhost:8080/swagger-ui.html (botón *Authorize* con cualquier usuario de demo).
- Para probar el swipe **desde el celular** en la misma red: `npm start -- --host 0.0.0.0` y entrá a `http://<ip-de-tu-pc>:4200`.
- Tests del backend: `cd backend && ./mvnw verify` · build del frontend: `cd frontend && npx ng build`.

> Si tu Java por defecto no es el 21 (error `UnsupportedClassVersionError ... class file version 65.0`), apuntá `JAVA_HOME` al JDK 21. Con Homebrew en macOS:
> `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`

## Usuarios de demo

Contraseña de todos: **`demo123`**. En el login hay botones para entrar con un clic como Facundo, Ana, el juez, la Dirección y Bruno.

| Usuario | Rol | Área | Para qué sirve en la demo |
|---|---|---|---|
| `facundo@demo.com` | EMPLEADO | Desarrollo | ★ Todavía no votó el T4. Su matriz: PR-01, PR-02, PR-04 y F-09 (no ve F-07 ni F-08). |
| `ana@demo.com` | EMPLEADO | Operaciones y Mesa de Ayuda | Está en la matriz de F-07: ve la **votación rápida**. |
| `juez@demo.com` (Mónica Calderón) | **JUEZ_PAZ** | Operaciones y Mesa de Ayuda | Coordina todo; también vota lo suyo. |
| `direccion@demo.com` (Ricardo Ferreyra) | **DIRECCION** | Desarrollo | Firma actas y designa al juez. No vota. |
| `bruno@demo.com` | EMPLEADO | Operaciones y Mesa de Ayuda | **Dueño de F-23**: firma su acta. |
| `carla@`, `dario@`, `eva@`, `tomas@demo.com` | EMPLEADO | Operaciones y Mesa de Ayuda | Carla fue jueza en 2025 · Eva es dueña de F-07 · Darío, de PR-06. |
| `elena@`, `gabi@`, `hernan@`, `paula@demo.com` | EMPLEADO | Desarrollo | |
| `ines@`, `julian@`, `karen@`, `leo@`, `oscar@demo.com` | EMPLEADO | Infraestructura | Julián es dueño de PR-02 (puede ajustarlo en *Mis procedimientos*). |
| `rocio@`, `santi@`, `uma@`, `vicente@demo.com` | EMPLEADO | Atención al Público | Reciben el trabajo de PR-03: son sus **testigos**. |

**Estado de la demo:** 2026-T3 cerrado con PR-03 (86 % / 75 % / 80 % → pendiente de casamiento, grupal), F-07 (55 % → segunda cita, con 4 objeciones *Demasiados pasos*), F-23 (18 %, 6 objeciones *Duplica otro registro* → pendiente de velorio), PR-02 obligatorio (36 % → segunda cita, nunca velorio; ya van 2 seguidas: requiere reformulación), PR-05 (quórum insuficiente en Infraestructura) y PR-01 (participación insuficiente). 2026-T4 abierto. Seis mejoras en distintos estados (una vencida), PR-06 casado con aniversario en 6 días y F-02 en el archivo de obsoletos.

## Guion de demo de Pasá y Rebotá (4 minutos)

0. **Presentación** (~10 s): abrí http://localhost:4200. El login muestra **Pasá y Rebotá** · *Evaluación participativa de procedimientos* y los botones para entrar con un clic como cada usuario de prueba.
1. **Empleado: votar y objetar** (`facundo@demo.com`, ~1 min)
   - En **Swipe** aparece *"Votás solo los procedimientos en los que participás"*: el mazo trae solo PR-01, PR-02, PR-04 y F-09 (su matriz). F-07 y F-08 no aparecen.
   - Cada carta muestra cuánto tiempo lleva, qué aporta, cuánto se usa, el **dueño** y el **padrino**. Tocá la carta (o el botón ⓘ) para abrir el **detalle completo**: bio, beneficio, áreas y base normativa si es obligatorio. Desde ahí también se puede votar.
   - Deslizá uno a la derecha (♥ *Me sirve*): aparece el cartel **Voto registrado**, que explica que pasa a casamiento si al cierre alcanza el 70 % en las áreas que lo usan.
   - Deslizá otro a la izquierda (✕ *Me sobra*): se abre *"¿Por qué te sobra?"*: el **motivo es obligatorio** y anónimo. Elegí el motivo (si es *Otro*, describilo), agregá un detalle si querés y tocá **Confirmar "Me sobra"**. Con **Cancelar** la carta vuelve al mazo sin votar.
2. **Juez: objeciones, mejoras y segunda cita de F-07** (`juez@demo.com`, ~1 min)
   - **Juez de paz → Objeciones**: agrupadas por procedimiento y categoría, sin autor. Marcá una de F-23 como *Fundada* con su respuesta → aparece el link a la mejora **M-007** creada en el tablero.
   - **Juez de paz → Segundas citas → F-07** (*Encuesta de satisfacción de la mesa de ayuda*): *Empezar la cita* arranca el cronómetro de 30 min (presentación, debate, propuestas, decisión) y abajo están las 4 objeciones *"La encuesta tiene cinco preguntas y la mayoría de los usuarios no la completa."*.
   - Escribí la minuta, elegí **Casar con condiciones**, escribí las condiciones y tocá **Lanzar votación rápida (7 días)**. (Si entrás como `ana@demo.com`, F-07 aparece con el badge **Votación rápida**.)
3. **Juez: casamiento grupal de PR-03** (~40 s)
   - **Registro → Casamientos → Registrar casamiento** en PR-03 (*Atención de peticiones urgentes*): *Casamiento grupal con Operaciones, Desarrollo e Infraestructura*. Ya vienen el dueño (Carla) y el padrino (Ana); elegí testigos de Atención al Público; oficia el juez.
   - *Sí, acepto* abre la **libreta L-012/2026** imprimible: versión, % por área (86 / 75 / 80), dueño, padrino, testigos, votos, prenup, condiciones, objeciones tratadas y aniversario.
4. **Velorio de F-23 con tres firmas** (~1 min)
   - **Registro → Velorios → Labrar acta de defunción**: título *Acta de defunción N.º 004/2026*, causa y epitafio sugeridos (*"18% de aprobación y 6 objeciones…"*, *"Insumía 40 minutos por semana"*). Completá herencia y lecciones, y en reencarnación elegí **Crear borrador**: `PR-11` *Parte semanal en el sistema de tickets*.
   - En el acta, **Firmar** (mantener apretado) como juez → entrá como `direccion@demo.com` (tiene el badge **1** en Registro) y firmá → entrá como `bruno@demo.com` (dueño) y firmá.
   - Con la tercera firma F-23 queda **VELADO** y aparece en **Registro → Archivo** con su acta y *"Lo reemplaza PR-11"*.
5. **Tablero de mejoras** (cualquiera, ~20 s)
   - **Tablero**: kanban NUEVA / EN ANÁLISIS / EN CURSO / RESUELTA / DESCARTADA, con las mejoras nuevas (objeción de F-23, condiciones de F-07, herencia de F-23 y *"Redactar el reemplazo PR-11 de F-23 (acta 004/2026) y publicarlo en Pasá y Rebotá"*) y la vencida en rojo. Filtrá por procedimiento u origen; al pasar una a *Resuelta* o *Descartada* sin respuesta, no te deja.

Extras: **Aniversarios** (PR-06 cumple 6 meses: informe con objeciones y mejoras abiertas, campo de uso real y *Renovar / Segunda cita / Divorciar*), **Matriz** (grilla usuario × procedimiento con guardado por lote) y **Juez de paz** como Dirección (designación con historial).

---

## Estructura

```
.
├── docker-compose.yml               # PostgreSQL 16
├── PLAN.md                          # Relevamiento y cambios v2 por archivo
├── backend/                         # Java 21 · Spring Boot 3.3 · Maven
│   └── src/
│       ├── main/java/ar/gob/defensoria/tinder/
│       │   ├── config/              # Seguridad (HTTP Basic stateless, CORS), OpenAPI, propiedades "tinder.*"
│       │   ├── dominio/             # Entidades JPA y enums
│       │   ├── repositorio/         # Spring Data JPA
│       │   ├── servicio/            # MaquinaEstados, ClasificadorVeredicto, Votación (mazo), Ciclos, Objeciones, Segundas citas,
│       │   │                        # Casamientos, Velorios, Mejoras, Matriz, Juez de paz…
│       │   └── web/                 # Controllers REST, DTOs (records) y @RestControllerAdvice
│       ├── main/resources/
│       │   ├── application.yml, application-demo.yml
│       │   └── db/changelog/        # changes/ (v1, intactos) · v2/ (migraciones + seed demo)
│       └── test/java/…              # JUnit 5 + Mockito
└── frontend/                        # Angular 19 standalone · diseño mobile-first
    └── src/app/
        ├── core/                    # Auth, interceptor, guards, ApiService, modelos, UiService, diseno.ts (paleta y textos)
        ├── shared/                  # Libreta y acta de defunción (imprimibles)
        └── pages/                   # login, shell, votar (/swipe), resultados, registro (casamientos, velorios,
                                     # aniversarios, archivo), tablero, matriz, objeciones, segundas-citas,
                                     # juez-de-paz, gestion, mis-procedimientos
```

## API (prefijo `/api`)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/auth/me` | todos |
| GET | `/tinder/mazo` (matriz del usuario: ciclo trimestral + votaciones rápidas) | todos |
| POST | `/tinder/votos` `{procedimientoId, cicloId, valor, objecion: {categoria, texto}}` (objeción obligatoria con NOPE, prohibida con LIKE) → 201 · 400 objeción con LIKE · 400 `MOTIVO_OBLIGATORIO` (NOPE sin motivo, u *Otro* sin texto) · 403 fuera de la matriz · 409 ya votó | todos |
| GET | `/ciclos`, `/ciclos/actual` (trimestral, 204 si no hay) | todos |
| POST | `/ciclos`, `/ciclos/{id}/cerrar` (trimestral o rápido) | JUEZ_PAZ |
| GET | `/resultados?cicloId=&areaId=` (por procedimiento, con sus áreas) | EMPLEADO: su área · resto: todas |
| GET | `/procedimientos?estado=` (sin VELADOS), `/procedimientos/archivo`, `/procedimientos/mios`, `/procedimientos/{id}` | todos |
| POST/PUT/DELETE | `/procedimientos[/{id}]` · POST `/procedimientos/{id}/publicar` | JUEZ_PAZ |
| PUT | `/procedimientos/{id}/contenido` (versión + 1) | dueño tras AJUSTAR_Y_REVOTAR, o JUEZ_PAZ |
| GET · PUT | `/matriz?areaId=` · `/matriz` `{altas, bajas}` | JUEZ_PAZ, DIRECCION · JUEZ_PAZ |
| GET · PATCH | `/objeciones?procedimientoId=` (agrupadas) · `/objeciones/{id}` `{fundada, respuesta, fechaCompromiso?}` | JUEZ_PAZ, DIRECCION · JUEZ_PAZ |
| GET · PUT | `/segundas-citas[?todas=true]`, `/segundas-citas/{id}` · `/segundas-citas/{id}` `{minuta, decision, condiciones, lanzarRapida}` | JUEZ_PAZ, DIRECCION · JUEZ_PAZ |
| POST | `/casamientos` | JUEZ_PAZ |
| GET | `/casamientos`, `/casamientos/{id}` | todos |
| POST | `/casamientos/{id}/objeciones` `{categoria, texto}` (anónima; boda o aniversario) | quien esté en la matriz |
| GET | `/casamientos/aniversarios` (informe) | JUEZ_PAZ, DIRECCION |
| POST | `/casamientos/{id}/renovar` · `/segunda-cita` · `/divorciar` `{usoReal?}` | JUEZ_PAZ |
| GET · POST | `/velorios/sugerencia?procedimientoId=` · `/velorios` | JUEZ_PAZ |
| GET | `/velorios`, `/velorios/{id}` | todos |
| POST | `/velorios/{id}/firmar` (409 si repite) | JUEZ_PAZ, DIRECCION o el dueño |
| GET · POST · PUT · DELETE | `/mejoras` · `/mejoras` · `/mejoras/{id}` · `/mejoras/{id}` | todos · JUEZ_PAZ · JUEZ_PAZ o responsable · JUEZ_PAZ |
| GET · POST | `/juez-de-paz/designaciones` | JUEZ_PAZ, DIRECCION · DIRECCION |
| GET | `/juez-de-paz/calendario` | JUEZ_PAZ, DIRECCION |
| GET | `/areas` (todos) · `/usuarios?rol=&areaId=` (JUEZ_PAZ, DIRECCION) · `/alertas` | |

Errores siempre como `{codigo, mensaje}` en español (400 validación, 401, 403, 404, 409 conflicto, 422 regla de negocio / transición inválida).

## Cómo se garantiza el anonimato

- Al votar, en **una sola transacción** se insertan `voto_emitido(usuario, procedimiento, ciclo)` —quién votó, para impedir el doble voto—, `voto(procedimiento, ciclo, área, valor)` —qué se votó— y, si hubo, `objecion(procedimiento, ciclo, área, categoría, texto)`. **Ni `voto` ni `objecion` tienen usuario ni timestamp**, y no hay columna que una las tablas.
- Los ids de `voto` y `objecion` son **UUID aleatorios**: ni el orden de inserción delata a nadie.
- El juez ve las objeciones agrupadas por categoría, **sin área ni autor**. La única persona asociada a una objeción es el juez que la evaluó (`evaluada_por`).
- Los resultados solo se calculan agregados al cerrar el ciclo. Un área con menos de 3 votos no muestra números.
- No se loguea SQL ni cuerpos de requests. Tests por reflexión verifican que `Voto` y `Objecion` no tengan campos de usuario ni de fecha.

## Tests (JUnit 5)

`./mvnw verify` corre 63 tests, sin base de datos (Mockito):

- `ClasificadorVeredictoTest`: 70 % → CASAMIENTO, 69,9 % → SEGUNDA_CITA, 40 % → SEGUNDA_CITA, 39,9 % → VELORIO; obligatorio con 10 % → SEGUNDA_CITA; áreas 85 % y 20 % → SEGUNDA_CITA con relación a distancia; 40 % de participación → PARTICIPACION_INSUFICIENTE; quórum por área; grupal; parámetros configurables.
- `CicloServiceTest`: participación insuficiente sin cambio de estado; obligatorio nunca a velorio (con cita convocada); dos segundas citas seguidas → `requiere_reformulacion` + mejora REFORMULACION; el contador vuelve a 0; votación rápida de CASAR_CON_CONDICIONES.
- `TinderServiceTest`: voto + objeción en la misma transacción; fuera de la matriz → 403; objeción con LIKE → 400; NOPE sin motivo u *Otro* sin texto → 400; doble voto → 409.
- `AnonimatoTest`: `Voto` y `Objecion` sin campos de usuario ni fecha/hora (reflexión).
- `CasamientoServiceTest`: objeción fundada sin respuesta → 422; oficiante que no es JUEZ_PAZ → 422; testigo de un área que se casa → 422; libreta completa.
- `VelorioServiceTest`: VELADO solo con las 3 firmas; firma repetida → 409; reencarnación BORRADOR → mejora "redactar el reemplazo"; causa sugerida.
- `MejoraServiceTest`: DESCARTADA sin respuesta → 422; permisos de edición.
- `JuezPazServiceTest`: nueva designación → el anterior vuelve a EMPLEADO.
- `ObjecionServiceTest`: no fundada sin respuesta → 422; evaluar crea mejora; objeción de boda fundada → POSTERGADO + SEGUNDA_CITA.
- `MaquinaEstadosTest`: transiciones válidas e inválidas (422), BORRADOR → EN_TINDER.

## Decisiones v2

Cuando algo era ambiguo se eligió lo más simple que respetara las reglas:

1. **Liquibase**: los changesets v1 (`changes/`) no se tocaron; todo lo nuevo está en `db/changelog/v2/` (renombres y migraciones de datos explícitos). El seed v2 tiene `context: demo`; `application.yml` fija `spring.liquibase.contexts: base`, así el seed solo corre con el perfil `demo`. El seed vacía las tablas (TRUNCATE) y carga todo de nuevo.
2. **Migración de roles**: `CALIDAD` → `JUEZ_PAZ` por UPDATE, y cada uno queda con una designación vigente en su área desde el día de la migración.
3. **Migración de datos v1**: el dueño arranca igual al autor (padrino); la matriz se puebla con la regla vieja (todos los usuarios del área, también Dirección); MATCH/RECHAZO se renombran sin recalcular; `causa_muerte` se renombra a `causa`; `acta_firmada/firmado_por/fecha_firma` pasan a la firma de Dirección y se borran; las actas viejas quedan con motivo "sin reemplazo"; los casamientos viejos reciben número `L-NNN/AAAA` y una foto de % por área. Los ciclos v1 no tienen resumen por procedimiento: la pantalla de resultados lo aclara.
4. **Matriz**: un procedimiento nuevo arranca con la gente de sus áreas (sin Dirección) y el juez la ajusta. La Dirección no entra en la matriz y no vota. Los velados salen de la matriz, del mazo y de las vistas operativas (solo están en el Archivo).
5. **Áreas del cierre**: se evalúan las áreas de la gente de la matriz más las que tengan votos. El voto cuenta para el área de quien vota. `votantes` = votos del ciclo; `habilitados` = matriz al momento del cierre; con exactamente 50 % de participación se evalúa.
6. **Contador de segundas citas**: con cualquier destino que no sea SEGUNDA_CITA vuelve a 0, incluidos los insuficientes (lectura literal de la regla). La mejora REFORMULACION se crea una sola vez, al llegar a 2.
7. **Segunda cita**: se programa a 7 días del cierre (`tinder.dias-hasta-segunda-cita`). Si nace de una objeción fundada en la boda/aniversario, de la acción "segunda cita" del aniversario o de divorciar un obligatorio, no tiene ciclo (`ciclo_id` NULL).
8. **Votación rápida**: un solo ciclo RAPIDA abierto por procedimiento (índice parcial). Mientras está abierta, ese procedimiento no está en el mazo ni en el cierre trimestral. La cierra el juez desde *Gestión → Ciclos → Historial*. Con AJUSTAR_Y_REVOTAR se aplica el algoritmo completo; con CASAR_CON_CONDICIONES y VELORIO se mira el % global (con las mismas reglas de participación y quórum); si no se cumple, sigue en SEGUNDA_CITA. `VIGENTE_RAPIDA` no hizo falta.
9. **Objeciones**: el PATCH acepta *fundada* sin respuesta (queda EVALUADA y bloquea el casamiento); la pantalla del juez pide respuesta siempre. La primera evaluación crea la mejora (fundada o no) con responsable el dueño y fecha a 30 días, editable. Una objeción de casamiento es de ANIVERSARIO si faltan 15 días o menos para el aniversario; si no, de BODA. Fundada → casamiento POSTERGADO, procedimiento a SEGUNDA_CITA y cita convocada.
10. **Casamiento**: los testigos tienen que ser de áreas que no se casan; la foto por área sale del último resultado con quórum; si no se escriben condiciones, se copian las de la última segunda cita CASAR_CON_CONDICIONES. Número `L-NNN/AAAA` = el mayor del año + 1.
11. **Aniversario**: renovar, segunda cita y divorciar guardan el "uso real" en la libreta (`informe_aniversario`); la segunda cita del aniversario además crea una mejora ANIVERSARIO. La Dirección ve el informe; las acciones son del juez.
12. **Acta**: número = mayor del año + 1. La firma se asigna por rol (juez, Dirección) y, si no, por ser dueño. **Una persona, una firma** (409 si repite): si el juez es además el dueño, firma otro juez o se cambia el dueño.
13. **Reencarnación**: el BORRADOR copia dueño (que queda también de padrino), áreas, tiempos y matriz del difunto, con una bio provisoria; si se elige uno existente se le anota `reemplaza_a` si no tenía. Al publicarlo pasa a EN_TINDER y entra en el ciclo trimestral abierto (o en el próximo).
14. **Versiones**: la versión sube cada vez que cambia el contenido (nombre, bio, tiempos, beneficio, versión express), lo edite el juez o el dueño. El dueño edita desde *Mis procedimientos* un BORRADOR o un procedimiento en segunda cita cuya última cita decidió ajustarlo.
15. **Mejoras**: código con secuencia propia (`mejora_codigo_seq`); responsable por defecto el dueño, si no el padrino, si no quien la dispara; el responsable no puede reasignarla; solo se borra una NUEVA (el resto se descarta con respuesta). `origen_id` es texto porque puede ser un UUID (objeción) o un número.
16. **Juez de paz**: se elige entre la gente del área (422 si no); la Dirección no puede serlo; el anterior vuelve a EMPLEADO si no tiene otra designación vigente.
17. **Calendario**: "matriz revisada" = una revisión registrada (tabla `matriz_revision`) entre 30 días antes y 7 días después del inicio del ciclo (en la grilla hay "Marcar revisada" aunque no haya cambios); participación = votos emitidos / matriz de los procedimientos votables; ceremonias = nadie pendiente de casamiento ni de velorio sin acta; tablero = ninguna mejora vencida.
18. **Resultados**: el empleado ve solo las barras de su área (más el resumen global, que es agregado); con quórum insuficiente en el procedimiento no se muestra el %. Se mantienen las **alertas** de la v1 (relación a distancia, obligatorio no velable, casamiento grupal).
19. **Seed**: el 88 % de PR-03 necesita 8 votos en un área, y el máximo posible con 4 a 6 empleados por área más el juez es 7: Operaciones queda en **86 % (6 de 7)**; 75 % y 80 % son exactos. Se sumó el área **Atención al Público** para que PR-03, que es de las otras tres, tenga testigos. Se agregó el ciclo 2026-T2 para que PR-02 llegue a dos segundas citas seguidas, y el velado F-02 (acta 003/2026) para que el acta de F-23 sea la **004/2026** y la libreta de PR-03 la **L-012/2026**, como en el ejemplo. Los textos de la demo tienen tono formal: no se reescribió el changeset 300, se agregaron `301-seed-demo-textos` (procedimientos) y `302-seed-demo-textos-generales` (resultados, objeciones, minuta, casamiento, acta y mejoras), también con contexto `demo`.
20. **Frontend**: `/swipe` es la ruta del mazo (`/votar` redirige). `/casamientos`, `/velorios`, `/aniversarios` y `/archivo` son pestañas de Registro. Las páginas del juez (`/matriz`, `/objeciones`, `/segundas-citas`, `/gestion`) cuelgan de **Juez de paz** en la barra de navegación. Se agregó `/mis-procedimientos` (desde el perfil) para que el dueño ajuste su procedimiento.
21. **Nombre "Pasá y Rebotá"**: el nombre visible de la aplicación (login, encabezado, título de la pestaña, Swagger, `spring.application.name`, `pom.xml`, proyecto Angular) pasó de "Tinder de procedimientos" a **Pasá y Rebotá**, y los textos que decían "el Tinder" dicen "votación" o "ciclo trimestral". Quedan con `tinder` solo los identificadores técnicos, para no romper datos ni sesiones: el paquete Java `ar.gob.defensoria.tinder`, las propiedades `tinder.*`, la ruta `/api/tinder`, el estado `EN_TINDER` (se muestra como "En votación"), la base y el volumen de Docker `tinder`, las claves de `localStorage` y el autor `tinder-v2` de los changesets.
22. **Tono formal**: los mensajes que genera el backend (veredictos, mejoras automáticas, errores) y los textos de la interfaz se redactaron en tono formal. El cartel "¡Es un match!" pasó a ser **Voto registrado**: aparece con cada voto *Me sirve* (el backend devuelve `match: true` en todo LIKE), así que confirma el voto en lugar de anunciar un acuerdo del área. Se mantienen las metáforas del diseño (casamiento, velorio, segunda cita, padrino, juez de paz) porque son los nombres de las etapas.
23. **Motivo obligatorio al rechazar**: un voto NOPE siempre lleva una objeción con categoría; si la categoría es *Otro*, también texto. Lo valida el backend (400 `MOTIVO_OBLIGATORIO`) y la hoja *"¿Por qué te sobra?"* ya no tiene "Saltar": se confirma con motivo o se cancela y la carta vuelve al mazo. El texto sigue siendo opcional para el resto de las categorías. Los votos NOPE del seed anteriores a esta regla quedan como están.

### Decisiones de la v1 que siguen vigentes

- Enums como `VARCHAR` + `CHECK` (no tipos `ENUM` de PostgreSQL): mapean directo con `@Enumerated(STRING)` y pasan `ddl-auto=validate`.
- Un solo ciclo trimestral abierto (índice parcial único + chequeo en el servicio, 409).
- Los procedimientos no se borran, se velan: el DELETE solo funciona si nunca entró en juego (borrador o en votación sin votos ni resultados).
- 401 sin `WWW-Authenticate` (el navegador no muestra su popup) y frontend → backend vía proxy (`/api` → `localhost:8080`).
- Angular 19 (cumple "17+"), con signals y control flow nuevo; los tests pedidos son los de JUnit del backend.
