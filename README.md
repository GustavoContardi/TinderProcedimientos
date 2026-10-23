# 💍 Tinder de procedimientos

> *Hasta que la burocracia nos separe.*

Prototipo para la Defensoría del Pueblo (Design Thinking · Sistemas de Información IV).
Los empleados votan **de forma anónima** qué procedimientos ISO 9001 les sirven (♥, swipe a la derecha) y cuáles les sobran (✕, swipe a la izquierda). El resultado define el ciclo de vida del procedimiento:

| Resultado | Destino |
|---|---|
| **MATCH** | 💍 **Casamiento**: el equipo se compromete públicamente (libreta, votos, acuerdo prenupcial, padrino, aniversario). |
| **RECHAZO** | 🕯️ **Velorio**: baja con obituario y acta de defunción firmada por la Dirección. Nunca se borra (ISO 9001 7.5.3). |
| **Votos divididos** | ☕ **Segunda cita**: se simplifica y vuelve a votarse el trimestre siguiente. |

---

## Levantar todo

Requisitos: **Docker**, **Java 21** y **Node 20+**.

```bash
docker compose up -d
```

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm i && npm start
```

Abrí **http://localhost:4200**. (`npm start` corre `ng serve`; si tenés el Angular CLI global, `ng serve` funciona igual.)

- API: http://localhost:8080/api · Swagger: http://localhost:8080/swagger-ui.html (botón *Authorize* con cualquier usuario de prueba).
- Para probar el swipe **desde el celular** en la misma red: `npm start -- --host 0.0.0.0` y entrá a `http://<ip-de-tu-pc>:4200`.
- Para volver la base al estado inicial: `docker compose down -v && docker compose up -d` y reiniciar el backend (Liquibase vuelve a cargar el seed).
- Tests del backend: `cd backend && ./mvnw test`.

> Si tu Java por defecto no es el 21 (error `UnsupportedClassVersionError ... class file version 65.0`), apuntá `JAVA_HOME` al JDK 21 antes de correr el backend. Con Homebrew en macOS:
> `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`
> (`/usr/libexec/java_home -v 21` solo funciona si el JDK está registrado en `/Library/Java/JavaVirtualMachines`; si no, devuelve otra versión sin avisar).

## Usuarios de prueba

Contraseña de todos: **`demo123`**

| Usuario | Rol | Área |
|---|---|---|
| `facundo@demo.com` | EMPLEADO (★ el de la demo: todavía no votó) | Desarrollo |
| `elena@`, `gabi@`, `hernan@demo.com` | EMPLEADO | Desarrollo |
| `ana@`, `bruno@`, `carla@`, `dario@demo.com` | EMPLEADO | Operaciones y Mesa de Ayuda |
| `ines@`, `julian@`, `karen@`, `leo@demo.com` | EMPLEADO | Infraestructura |
| `calidad@demo.com` | CALIDAD | Operaciones y Mesa de Ayuda |
| `direccion@demo.com` | DIRECCION | Desarrollo |

En la pantalla de login hay botones para completar estos usuarios con un clic.

## Guion de demo (3 minutos)

1. **Empleado desliza 5 procedimientos** — Entrá como `facundo@demo.com`. Tenés 5 tarjetas: PR-01, PR-02, PR-03, PR-04 y F-09. Arrastrá a la derecha (♥ *ME SIRVE*) o a la izquierda (✕ *ME SOBRA*), o usá los botones o las flechas ← →. Con cada ♥ aparece *"¡Es un match! Preparen la libreta"*. Al final: *"No hay más procedimientos para vos este trimestre"*.
   - Tip: deslizá **PR-02** a la izquierda para lucir la regla de obligatoriedad en el paso 2.
2. **Calidad cierra el ciclo** — Salí y entrá como `calidad@demo.com` → **Ciclos** → *Cerrar ciclo y calcular resultados*. El resumen muestra el destino de cada procedimiento: PR-03 hace match en las 3 áreas (candidato a **casamiento grupal**), F-08 va al velorio, PR-04 queda en **relación a distancia** (match en Infraestructura, rechazo en Desarrollo) y PR-02 no se puede velar porque es obligatorio por norma. Abajo aparecen las alertas para Calidad.
3. **Resultados** — Pestaña **Resultados**: barras *me sirve / me sobra* por área y chips de veredicto. Donde hubo menos de 3 votos se ve *"Pocos votos, por anonimato no mostramos números"*. (Un empleado solo ve su área.)
4. **Casamiento grupal de PR-03** — **Casamientos** → elegí *PR-03*: se precargan las 3 áreas, el padrino (el autor) y aparece *"Casamiento grupal"*. Completá los votos y el acuerdo prenupcial → *Sí, acepto 💍* → *Ver libreta* (imprimible, con estética de acta civil).
5. **Dirección firma el acta de F-23** — Entrá como `direccion@demo.com` → **Velorios** → *Ver necrológica* de F-23 → *Firmar acta de defunción*. F-23 pasa a **VELADO** y queda como documento obsoleto.

Extra: en **Aniversarios** (Calidad/Dirección) está PR-06, que cumple 6 meses de casado en pocos días: se puede *renovar votos* o *divorciar*.

---

## Estructura

```
.
├── docker-compose.yml               # PostgreSQL 16
├── backend/                         # Java 21 · Spring Boot 3.3 · Maven
│   ├── pom.xml, mvnw
│   └── src/
│       ├── main/java/ar/gob/defensoria/tinder/
│       │   ├── config/              # Seguridad (HTTP Basic stateless, CORS), OpenAPI, propiedades "tinder.*"
│       │   ├── dominio/             # Entidades JPA y enums
│       │   ├── repositorio/         # Spring Data JPA
│       │   ├── servicio/            # Reglas de negocio: MaquinaEstados, ClasificadorVeredicto, Tinder, Ciclos, Casamientos, Velorios…
│       │   ├── web/                 # Controllers REST, DTOs (records) y @RestControllerAdvice
│       │   └── excepcion/
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/changelog/        # Liquibase: master YAML, un changeset por tabla y uno de seed
│       └── test/java/…              # JUnit 5
└── frontend/                        # Angular 19 standalone · Angular Material · CDK
    ├── package.json, angular.json, proxy.conf.json
    └── src/app/
        ├── core/                    # AuthService, interceptor, guards, ApiService, modelos
        ├── shared/                  # Barra de votos (CSS puro), chips de veredicto y estado
        └── pages/                   # login, swipe, resultados, procedimientos, ciclos, casamientos, velorios, aniversarios
```

## API (prefijo `/api`)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/auth/me` | todos |
| GET | `/tinder/mazo` | todos |
| POST | `/tinder/votos` `{procedimientoId, valor: LIKE\|NOPE}` → 201 `{match}` · 409 si ya votó | todos |
| GET | `/ciclos`, `/ciclos/actual` (204 si no hay) | todos |
| POST | `/ciclos`, `/ciclos/{id}/cerrar` | CALIDAD |
| GET | `/resultados?cicloId=&areaId=` | EMPLEADO: su área · resto: todas |
| GET | `/procedimientos?estado=`, `/procedimientos/{id}` | todos |
| POST/PUT/DELETE | `/procedimientos[/{id}]` | CALIDAD |
| POST | `/casamientos` | CALIDAD |
| GET | `/casamientos`, `/casamientos/{id}` | todos |
| GET | `/casamientos/aniversarios` · POST `/casamientos/{id}/renovar` · `/casamientos/{id}/divorciar` | CALIDAD, DIRECCION |
| POST | `/velorios` · GET `/velorios/sugerencia?procedimientoId=` | CALIDAD |
| GET | `/velorios`, `/velorios/{id}` | todos |
| POST | `/velorios/{id}/firmar` | DIRECCION |
| GET | `/areas` (todos) · `/usuarios?rol=` (CALIDAD, DIRECCION) | |
| GET | `/alertas` (CALIDAD, DIRECCION) · POST `/alertas/{id}/revisar` (CALIDAD) | |

Errores siempre como `{codigo, mensaje}` en español (400 validación, 401, 403, 404, 409 conflicto, 422 regla de negocio / transición inválida).

## Cómo se garantiza el anonimato

- Al votar, en **una sola transacción** se inserta `participacion(usuario, procedimiento, ciclo)` —quién votó, para impedir el doble voto— y `voto(procedimiento, ciclo, área, valor)` —qué se votó— **sin usuario ni timestamp**. No hay columna que una ambas tablas.
- El id del voto es un **UUID aleatorio**, así ni el orden de inserción delata a nadie.
- Los resultados solo se calculan agregados al cerrar el ciclo, y con menos de `tinder.quorum-minimo` votos la API no devuelve números.
- No se loguea SQL (`show-sql: false`) ni cuerpos de requests; el servicio de votos no loguea nada.
- Un test por reflexión verifica que la entidad `Voto` no tenga ningún campo de usuario ni de fecha.

## Tests (JUnit 5)

`./mvnw test` corre 31 tests:

- `ClasificadorVeredictoTest`: veredicto por umbrales y quórum (incluye los bordes de 60 %) y parámetros configurables.
- `MaquinaEstadosTest` / `CicloServiceTest`: obligatorio rechazado → SEGUNDA_CITA y nunca PENDIENTE_VELORIO; veredictos mezclados → SEGUNDA_CITA (relación a distancia) con alerta; match en 3 áreas → casamiento grupal; transiciones inválidas → 422.
- `TinderServiceTest`: doble voto en el mismo ciclo → 409 (también ante la carrera que choca contra la PK).
- `VotoAnonimatoTest`: `Voto` no tiene campos de usuario ni de fecha (reflexión).

## Decisiones

Cuando algo era ambiguo se eligió lo más simple que respetara las reglas:

1. **Enums como `VARCHAR` + `CHECK`** en lugar de tipos `ENUM` nativos de PostgreSQL: mapean directo con `@Enumerated(STRING)` y pasan `ddl-auto=validate` sin conversores.
2. **Un solo ciclo abierto** con índice parcial único `ON ciclo_votacion(estado) WHERE estado = 'ABIERTO'` y además un chequeo en el servicio (409).
3. **Calidad y Dirección tienen área** (Operaciones y Desarrollo) porque `/swipe` es para todos los roles y el mazo depende del área.
4. **Se evalúan al cierre** solo los procedimientos en EN_TINDER o SEGUNDA_CITA. Se guarda un resultado por cada una de sus áreas, aunque tenga 0 votos (queda QUORUM_INSUFICIENTE).
5. **Sin quórum en ninguna área**: el procedimiento queda en su estado actual (EN_TINDER, o SEGUNDA_CITA si ya venía de una segunda cita), o sea, sigue en el mazo.
6. **MATCH en un área y SEGUNDA_CITA en otra** (sin rechazo) → SEGUNDA_CITA por votos divididos, sin alerta. La alerta de "relación a distancia" se genera solo con MATCH y RECHAZO a la vez.
7. **Alertas**: se agregó la tabla `alerta` (relación a distancia, obligatorio no velable, candidato a casamiento grupal) y una pantalla para que Calidad las marque como revisadas.
8. **Quórum insuficiente**: los conteos se guardan en `resultado_ciclo` (son agregados), pero la API los devuelve en `null`.
9. **`usoTexto`** ("Me usan X de cada 10"): sale del % de "me sirve" del último ciclo con quórum en el área del usuario. Si no hay datos, se muestra la frecuencia ("Me usan 5 veces por semana").
10. **Casamiento grupal** = 2 o más áreas. Por defecto se casan todas las áreas del procedimiento; solo se pueden elegir áreas que lo comparten. Fecha por defecto: hoy.
11. **Renovar** crea un casamiento nuevo con fecha de hoy y aniversario a 6 meses; el anterior queda RENOVADO. **Divorciar** un obligatorio lo manda a SEGUNDA_CITA.
12. **Borrar procedimientos**: el `DELETE` solo funciona si nunca entró en juego (EN_TINDER, sin votos ni resultados), por ejemplo un alta cargada por error. Con historia responde 422: *"los procedimientos no se borran, se velan"*. Los VELADOS no se pueden editar.
13. **Velorio**: si no se manda causa o epitafio se usan los sugeridos. La causa suma los "nopes" del último ciclo en el que se evaluó el procedimiento. Un velorio por procedimiento.
14. **401 sin `WWW-Authenticate`**: el backend responde JSON propio para que el navegador no muestre su popup de Basic Auth.
15. **Frontend → backend vía proxy** (`/api` → `localhost:8080`, en `proxy.conf.json`). Así también funciona desde el celular. CORS para `http://localhost:4200` queda habilitado igual (por ejemplo, para llamadas directas).
16. **Angular 19** (cumple "17+"), con signals y control flow nuevo. Los tests pedidos son los de JUnit del backend; no se incluyó Karma/Jasmine en el frontend.
17. **Datos de prueba pensados para la demo**:
    - Ciclo **2026-T1** cerrado, con el MATCH de PR-06 (casado el 30/04/2026, aniversario el 30/10/2026, para que aparezca en Aniversarios).
    - Ciclo **2026-T3** cerrado, con MATCH (PR-05 en Operaciones → pendiente de casamiento), RECHAZO (F-23 en las 3 áreas), SEGUNDA_CITA (F-09) y QUORUM_INSUFICIENTE (F-07).
    - Ciclo **2026-T4** abierto, con votos precargados de compañeros (todos menos Facundo y Leandro). Así, al cerrarlo, PR-03 hace match en las 3 áreas, PR-04 queda en relación a distancia, PR-02 (obligatorio) no se puede velar y F-08 va al velorio.
    - El **velorio de F-23** ya está organizado por Calidad y solo espera la firma de la Dirección.
    - Las participaciones y los votos del seed se insertan por separado, sin usuario en los votos.
