# Sudoku MVP — Design Spec

- **Fecha:** 2026-09-04
- **Estado:** Aprobado (pendiente review del spec escrito)
- **Plataforma:** Android (única)

## 1. Objetivo

App Android nativa de Sudoku 9×9, 100% offline, construida con las prácticas
recomendadas del ecosistema Android (Kotlin, Jetpack Compose, Material 3, MVVM +
UDF, Room, Hilt).

El MVP entrega:

1. **Modo campaña único** con curva de dificultad creciente. Cada puzzle tiene un
   número secuencial (1, 2, 3…) y un *score de dificultad continuo*. La campaña no
   tiene selección libre de dificultad: los "niveles" son **bandas etiquetadas**
   sobre la curva continua.
2. **Progresión persistente**: partida en curso retomable + historial de puzzles
   completados + estadísticas agregadas.
3. **Generador de sudokus propio** que produce puzzles con solución única y los
   puntúa simulando técnicas humanas de resolución, apuntando al score objetivo
   que fija la campaña.

### Fuera de alcance (MVP)

- Modo de juego libre con selección de dificultad.
- Logros / achievements.
- Cuentas, sync en la nube, multijugador, leaderboards.
- Sudokus de tamaños distintos a 9×9 o variantes (Killer, X, etc.).
- Multi-módulo Gradle (anotado como evolución futura).
- WorkManager para generación (se usa un coroutine scope de aplicación; anotado
  como posible evolución si el refill en background lo requiere).

## 2. Stack técnico

| Área | Elección |
|------|----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 (dynamic color, tema claro/oscuro por sistema + override manual) |
| Arquitectura | MVVM + Unidirectional Data Flow. `ViewModel` expone `StateFlow<XxxUiState>`; los eventos de UI son funciones del VM |
| Persistencia | Room (datos estructurados) + DataStore Preferences (settings) |
| DI | Hilt |
| Navegación | Navigation Compose (versión estable) |
| Async | Kotlin Coroutines + Flow en todas las capas |
| Build | Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`) |
| Módulos | Un solo módulo `:app`, paquetes por capa |
| minSdk | 24 |
| targetSdk / compileSdk | 36 |
| Permisos | Ninguno |

### Estructura de paquetes

```
com.<org>.sudoku
├── SudokuApplication.kt
├── di/                # módulos Hilt
├── domain/
│   ├── model/
│   ├── generator/
│   ├── solver/
│   ├── campaign/
│   └── usecase/
├── data/
│   ├── db/            # Room: entities, DAOs, database, converters, migrations
│   ├── datastore/     # settings
│   ├── repository/    # implementaciones
│   └── mapper/        # entity <-> domain
└── ui/
    ├── theme/
    ├── navigation/
    ├── components/    # SudokuBoard, NumberPad, GameToolbar, MistakeCounter, TimerText
    ├── home/
    ├── game/
    ├── stats/
    └── settings/
```

Las interfaces de repositorio viven en `domain/` (o en `data/repository` como
contrato); las implementaciones en `data/repository`. Se mantiene una regla de
dependencia: `ui → domain`, `data → domain`, `domain` no depende de nada de
Android salvo utilidades neutras.

## 3. Capa `domain` (lógica pura, testeable en JVM)

### 3.1 Modelo

| Tipo | Descripción |
|------|-------------|
| `CellValue` | `0` = vacía, `1..9` = valor |
| `Grid` | wrapper inmutable de `IntArray(81)` con acceso `get(row, col)` / `get(box)`; helpers de fila/columna/caja |
| `Cell` | `row`, `col`, `value`, `isGiven` (pista original), `notes: Set<Int>` |
| `Board` | estado jugable: 81 `Cell`, derivado de `Puzzle` + movimientos aplicados |
| `Puzzle` | `id`, `number` (nº de campaña, o null si buffer sin asignar), `givens: Grid`, `solution: Grid`, `difficultyScore: Double`, `band: DifficultyBand` |
| `DifficultyBand` | enum: `PRINCIPIANTE`, `FACIL`, `MEDIO`, `DIFICIL`, `EXPERTO`, `MAESTRO` con rango de score asociado |
| `Move` | `SetValue(cell, value, previousValue)`, `SetNote(cell, digit, added)`, `ClearCell(cell, previousValue, previousNotes)` — cada uno reversible |
| `GameSnapshot` | verdad del juego en curso: `puzzle`, `board`, `undoStack`, `redoStack`, `elapsedMs`, `mistakes`, `hintsUsed`, `status` |
| `GameStatus` | `IN_PROGRESS`, `COMPLETED`, `FAILED` (3 errores en modo límite) |

### 3.2 Generador (`domain/generator`)

- **`GridBuilder`**
  - Genera una grilla 9×9 completa y válida por *backtracking* con orden de
    candidatos aleatorizado (semilla inyectable para tests deterministas).
- **`ClueRemover`**
  - Parte de la grilla completa, quita celdas en orden aleatorio, y tras cada
    remoción verifica con `UniquenessChecker` que la solución siga siendo única.
    Si deja de serlo, revierte esa celda.
  - Parámetro: nº objetivo de pistas / condición de corte por score.
- **`UniquenessChecker`**
  - Solver de conteo de soluciones que corta apenas encuentra la 2ª solución.
- **`PuzzleFactory`**
  - Orquesta: `GridBuilder` → `ClueRemover` → `DifficultyRater`. Reintenta hasta
    que el score cae dentro de la ventana de tolerancia del target (ver §3.4).

### 3.3 Solver lógico y rating (`domain/solver`)

- **`Technique`** (estrategia): cada una recibe el estado de candidatos y devuelve
  las eliminaciones/colocaciones deducibles, más un `cost` (peso de dificultad).
  Orden de complejidad creciente para el MVP:
  1. Naked Single
  2. Hidden Single
  3. Locked Candidates (Pointing / Claiming)
  4. Naked Pair / Triple
  5. Hidden Pair / Triple
  6. X-Wing

  La lista es **extensible** — agregar técnicas más avanzadas (Swordfish,
  XY-Wing…) no cambia la arquitectura.

- **`LogicalSolver`**
  - Bucle: intenta técnicas de la más barata a la más cara; aplica la primera que
    produce progreso; repite. Registra qué técnicas se usaron y cuántas veces.
  - Si ninguna técnica avanza y el tablero no está completo → puzzle "no resoluble
    por lógica del MVP" (se descarta en generación).

- **`DifficultyRater`**
  - Corre `LogicalSolver` sobre los givens y calcula un **score continuo** ≈ 0–100:
    ```
    score = w_hardest * cost(técnica más difícil usada)
          + w_freq    * Σ (aplicaciones_i * cost_i normalizado)
          + w_clues   * f(cantidad de pistas iniciales)
    ```
    Pesos calibrados contra un set curado de puzzles con dificultad conocida
    (fixtures de test). El score se mapea a `DifficultyBand` por rangos.

### 3.4 Campaña (`domain/campaign`)

- **`CampaignCurve`**
  - `targetScore(puzzleNumber: Int): Double` — función **monótona creciente**,
    saturante (se aplana cerca del máximo), con ruido determinista pequeño por
    número de puzzle (para que dos puzzles consecutivos no sean idénticos en
    dificultad) y `clamp` al rango válido.
  - `toleranceWindow(attempt: Int): Double` — ventana que arranca angosta y se
    ensancha con los intentos fallidos de generación, con tope.
- **`bandFor(score): DifficultyBand`**

### 3.5 Use cases

| Use case | Responsabilidad |
|----------|-----------------|
| `GetNextCampaignPuzzle` | Devuelve el `Puzzle` para el nº de campaña actual: lo toma del buffer si hay uno con score compatible, si no lo genera on-demand |
| `StartGame` | Crea `GameSnapshot` inicial desde un `Puzzle` y lo persiste |
| `ResumeGame` | Rehidrata `GameSnapshot` desde persistencia |
| `ApplyMove` | Valida y aplica un `Move`, actualiza undo/redo, detecta error contra solución, incrementa `mistakes`, marca `COMPLETED` / `FAILED` |
| `UndoMove` / `RedoMove` | Reversión determinista |
| `RequestHint` | Revela el valor correcto de una celda vacía seleccionada (o una elegida por el solver), incrementa `hintsUsed` |
| `CompletePuzzle` | Registra `CompletedPuzzleEntity`, avanza `CampaignProgress`, limpia `CurrentGame` |
| `AbandonGame` | Descarta la partida en curso sin avanzar la campaña |
| `GetProgress` | nº de campaña actual + si hay partida retomable |
| `GetStats` | Agregados para la pantalla de estadísticas |

## 4. Capa `data`

### 4.1 Room

**Entidades**

| Entidad | Campos clave |
|---------|--------------|
| `PuzzleBufferEntity` | `id`, `givens` (81 chars), `solution` (81 chars), `difficultyScore`, `band`, `createdAt` — puzzles pre-generados sin asignar a un nº de campaña |
| `CurrentGameEntity` | `id` (single-row), `puzzleNumber`, `givens`, `solution`, `boardValues`, `notesJson`, `elapsedMs`, `mistakes`, `hintsUsed`, `status`, `undoStackJson`, `redoStackJson`, `updatedAt` |
| `CompletedPuzzleEntity` | `id`, `puzzleNumber`, `difficultyScore`, `band`, `durationMs`, `mistakes`, `hintsUsed`, `completedAt` |
| `CampaignProgressEntity` | `id` (single-row), `currentPuzzleNumber`, `highestCompletedNumber`, `updatedAt` |

Grids serializadas como `String` de 81 caracteres. Notas / stacks de undo como
JSON (kotlinx.serialization) vía `TypeConverter`.

**DAOs**: `PuzzleBufferDao`, `CurrentGameDao`, `CompletedPuzzleDao`,
`CampaignProgressDao`. Queries de agregación (`AVG`, `MIN`, `COUNT`, group by
`band`) en `CompletedPuzzleDao`; la racha se calcula en `StatsRepository` a partir
de las fechas.

**Migraciones**: migraciones Room explícitas. **No** se usa
`fallbackToDestructiveMigration`. Tests de migración instrumentados.

### 4.2 DataStore (settings)

| Setting | Default |
|---------|---------|
| `mistakeLimitEnabled` (modo límite 3 errores) | `true` |
| `highlightErrors` | `true` |
| `highlightSameNumbers` | `true` |
| `autoRemoveNotes` (borrar notas al colocar número) | `true` |
| `themeMode` (`SYSTEM` / `LIGHT` / `DARK`) | `SYSTEM` |

### 4.3 Repositorios

| Repo | Notas |
|------|-------|
| `PuzzleRepository` | `nextPuzzle(number)`: toma del buffer un puzzle cuyo score matchee la ventana del target; si el buffer está por debajo del umbral (`MIN_BUFFER`, p.ej. 5) dispara `refill()` en un `CoroutineScope` de aplicación (`SupervisorJob` + `Dispatchers.Default`). Si el buffer no tiene match y no hay tiempo, genera on-demand. Refill genera puzzles distribuidos alrededor de los próximos targets de la campaña |
| `GameRepository` | `observeCurrentGame()`, `save(snapshot)` con **debounce ~1s** (colapsa ráfagas de inputs), `clear()` |
| `StatsRepository` | `observeStats()` → `StatsUiModel` (mejor tiempo, promedio, racha actual, racha máxima, total completados, distribución por banda) |
| `SettingsRepository` | `observeSettings()` / setters, sobre DataStore |
| `CampaignRepository` | `observeProgress()`, `advance()` |

Mappers dedicados entity ↔ domain en `data/mapper`.

## 5. Capa `ui`

### 5.1 Navegación (Navigation Compose)

Destinos: `home`, `game`, `stats`, `settings`. `home` es el start destination.
`game` se abre desde `home` (Continuar / Jugar puzzle N). `stats` y `settings`
desde `home`. Back estándar; salir de `game` no descarta la partida (queda
persistida como `IN_PROGRESS`).

### 5.2 Pantalla Home / Campaña

- Estado: `HomeUiState { currentPuzzleNumber, currentBand, hasResumableGame, statsSummary, loading }`.
- Contenido: título, tarjeta del puzzle actual (nº + banda + score aproximado como
  estrellas/etiqueta), botón primario **Continuar** (si hay partida) o **Jugar
  puzzle N**, mini-resumen de stats, botones a Estadísticas y Ajustes.
- Al pulsar jugar: si no hay partida, `StartGame` con el puzzle de
  `GetNextCampaignPuzzle`; navega a `game`.

### 5.3 Pantalla Juego

- Estado: `GameUiState { board, selectedCell, notesMode, mistakes, mistakeLimitEnabled, elapsedText, hintsUsed, status, highlightErrors, highlightSameNumbers, numberPadCounts }`.
- Layout: tablero 9×9 arriba, fila de estado (timer, contador `errores 0/3`),
  barra de herramientas (deshacer, rehacer, notas [toggle], borrar, pista),
  teclado numérico 1–9 (con contador de cuántos de cada dígito faltan; dígitos
  completos se atenúan).
- Interacción del tablero:
  - Tap en celda → selección. Resalta fila/columna/caja de la celda y todas las
    celdas con el mismo valor (si `highlightSameNumbers`).
  - Tap en número del teclado → `ApplyMove(SetValue)` o `SetNote` según `notesMode`.
  - Validación **contra la solución** al escribir. Si es incorrecto y
    `highlightErrors` → celda en rojo. Si `mistakeLimitEnabled` → incrementa el
    contador; al llegar a 3 → `status = FAILED`, diálogo "Perdiste" con opción
    *Reintentar este puzzle* (nuevo `StartGame` con el mismo `Puzzle`) o *Volver*.
  - Si `mistakeLimitEnabled == false` (modo relajado): el error se marca pero no
    cuenta ni termina la partida.
  - `autoRemoveNotes`: al colocar un valor, se quitan las notas de ese dígito en
    fila/columna/caja.
- Al completar (tablero == solución): `status = COMPLETED` → hoja de resultado
  (tiempo, errores, pistas usadas) → `CompletePuzzle` → botón *Siguiente puzzle*
  (vuelve a `game` con el nº siguiente) o *Volver a Home*.
- Ciclo de vida: `elapsedMs` corre con un `LaunchedEffect` mientras
  `status == IN_PROGRESS` y la pantalla está resumida; se pausa en background. La
  verdad del tiempo se acumula y se persiste con el snapshot.

### 5.4 Pantalla Estadísticas

Lista/tarjetas de solo lectura: mejor tiempo, tiempo promedio, racha actual, racha
máxima, puzzles completados, barra de distribución por banda. Sin acciones.

### 5.5 Pantalla Ajustes

Toggles para los settings de §4.2 + selector de tema. Cambios se aplican en
caliente (los observa el `ViewModel` correspondiente).

### 5.6 Componentes reutilizables

`SudokuBoard` (dibuja grilla, líneas de caja gruesas, estados de celda),
`SudokuCellView`, `NumberPad`, `GameToolbar`, `MistakeCounter`, `TimerText`,
`ResultSheet`, `StatCard`.

## 6. Flujo de datos (partida)

```
UI event ─▶ GameViewModel.fn() ─▶ UseCase (dominio puro sobre GameSnapshot)
                                        │
                        nuevo GameSnapshot
                                        │
             ┌──────────────────────────┴───────────────┐
             ▼                                           ▼
   _uiState.update { map(snapshot) }         GameRepository.save(snapshot)  (debounce 1s)
             │                                           │
             ▼                                           ▼
        Compose recompone                        Room CurrentGameEntity
```

Al entrar a `game`: el VM llama `ResumeGame` si hay `CurrentGameEntity` con el
`puzzleNumber` pedido; si no, `GetNextCampaignPuzzle` + `StartGame`.
`SavedStateHandle` guarda transitorios de UI (celda seleccionada, notesMode) para
sobrevivir recreación de proceso; la verdad del juego siempre re-lee de Room.

## 7. Manejo de errores y casos borde

| Caso | Comportamiento |
|------|----------------|
| Generador no alcanza el score objetivo exacto | Acepta el puzzle más cercano dentro de `toleranceWindow(attempt)`, que se ensancha con los intentos; tope de intentos → acepta el mejor generado y loguea |
| Buffer vacío al pedir puzzle (1ª ejecución / refill lento) | `GetNextCampaignPuzzle` genera on-demand; UI muestra estado de carga en Home ("Preparando puzzle…") |
| Partida guardada corrupta / no deserializa | Se descarta `CurrentGameEntity`, se arranca partida nueva, se loguea |
| Muerte de proceso / rotación | UI transitoria en `SavedStateHandle`; verdad en Room; timer reanuda desde `elapsedMs` persistido |
| App cerrada a mitad de puzzle | Al volver, Home ofrece **Continuar** |
| Usuario pide pista sin celda seleccionada | El solver elige la "mejor" celda siguiente (single más obvio) y la revela |
| Reintento tras `FAILED` | Nuevo `GameSnapshot` desde el mismo `Puzzle`; no cuenta como completado; no avanza campaña |
| Migración de esquema Room | Migraciones explícitas; test instrumentado por versión |

## 8. Estrategia de testing

### 8.1 JVM unit (JUnit4 + MockK + Turbine + kotlinx-coroutines-test)

- **Solver**: resuelve un set de puzzles conocidos (fácil→experto) a la solución correcta.
- **`UniquenessChecker`**: detecta unicidad y multiplicidad en fixtures conocidos.
- **`ClueRemover`**: el resultado siempre tiene solución única (property-style sobre N semillas).
- **`GridBuilder`**: salida siempre es grilla válida y completa; determinista con semilla fija.
- **`DifficultyRater`**: monotonicidad sobre set curado (puzzle etiquetado "difícil" puntúa ≥ que uno "fácil"); score cae en el rango de su banda.
- **`CampaignCurve`**: `targetScore` monótona no decreciente; dentro de rango; ventana crece con `attempt`.
- **Use cases**: `ApplyMove` cuenta errores y transiciona estados; undo/redo determinista; `RequestHint` incrementa contador y coloca valor correcto; `CompletePuzzle` avanza campaña una sola vez.
- **ViewModels**: emisiones de estado correctas ante secuencias de eventos (Turbine); mapping settings→UI.
- **Mappers**: round-trip entity ↔ domain.

### 8.2 Instrumented (androidTest)

- DAOs: CRUD + queries de agregación de `CompletedPuzzleDao`.
- Tests de migración Room.
- `SettingsRepository` sobre DataStore real.

### 8.3 Compose UI test

- Colocar un número correcto lo fija; uno incorrecto lo marca y sube el contador.
- 3 errores en modo límite → diálogo de derrota.
- Deshacer/rehacer revierte y reaplica.
- Salir y volver a `game` retoma el mismo tablero.
- Completar el tablero muestra el `ResultSheet`.

## 9. Plan de entrega (alto nivel — se detalla en el plan de implementación)

1. Scaffold: proyecto Gradle, version catalog, Hilt, Room, navegación, tema, CI base.
2. Dominio: modelo + `GridBuilder` + `UniquenessChecker` + `ClueRemover` (con tests).
3. Dominio: `LogicalSolver` + `Technique`s + `DifficultyRater` (con tests y fixtures curados).
4. Dominio: `CampaignCurve` + `PuzzleFactory` + use cases.
5. Data: Room (entities, DAOs, DB, converters, migración v1), DataStore, repos, mappers.
6. UI: tema + navegación + Home.
7. UI: pantalla Juego (tablero, teclado, herramientas, timer, errores, resultado).
8. UI: Estadísticas + Ajustes.
9. Integración: refill de buffer en background, on-demand fallback, pulido de ciclo de vida.
10. Pasada de tests E2E/UI, accesibilidad básica (contentDescription, tamaños de toque ≥ 48dp, contraste), edge-to-edge.

## 10. Decisiones abiertas para el plan

- `applicationId` / nombre de paquete definitivo (placeholder `com.example.sudoku` hasta que se defina).
- Nombre visible de la app.
- Valores concretos de: `MIN_BUFFER`, tope de intentos de generación, pesos del `DifficultyRater`, rangos de cada `DifficultyBand`, forma exacta de `CampaignCurve.targetScore`. Se calibran durante la implementación del dominio (pasos 3–4) contra los fixtures.
