# FreeSudoku — progreso (autónomo, 2026-09-04)

Rama: `master` · todo commiteado.
**MVP funcional end-to-end verificado en emulador (API 35) y por el usuario en dispositivo real.**

## Estado: los 3 planes completos ✅

### Plan 1 — Scaffold + motor de dominio
Gradle 9.6 + AGP 9.3.1 (Kotlin built-in) + Hilt 2.60.1 + Compose BOM 2026.08, `compileSdk 37 / minSdk 24`.
- `domain/model`, `domain/generator` (backtracking + carver + factory apuntando a score),
  `domain/solver` (candidatos bitmask, técnicas humanas, rater continuo), `domain/campaign` (curva),
  `domain/game` (GameEngine puro: jugadas, undo/redo, notas, pistas, límite de 3 errores, modo relajado, timer).

### Plan 2 — Capa de datos
- Room v1 (4 tablas + DAOs + schema exportado), `GameSnapshotDto` JSON, `SettingsRepository` (DataStore).
- Repos: `PuzzleRepository` (buffer + refill background), `GameRepository`, `CampaignRepository`, `StatsRepository` (rachas por día).
- Use cases + Hilt (`DatabaseModule`, `SettingsModule`, `DomainModule`, `DispatchersModule`, `UiModule`).

### Plan 3 — UI
- Navigation Compose + tema (claro/oscuro/sistema + dynamic color).
- Pantallas: Home (card de campaña + Continuar/Jugar), Juego (tablero, teclado con contador, toolbar, timer, errores 0/3, hoja de resultado, diálogo de derrota), Estadísticas, Ajustes.
- `GameViewModel`: máquina de estados sobre `GameSnapshot`, guardado con debounce, `SavedStateHandle` para UI transitoria, timer inyectable (`Ticker`).
- Componentes: `SudokuBoard` (grilla dibujada, resaltados, notas 3x3), `NumberPad`, `GameToolbar`, `GameTopStatus`, `ResultSheet`.

## Verificación

- **93 tests unitarios JVM** + **9 tests instrumentados** — todos en verde.
- `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` — verdes (lint 0 errores).
- Smoke manual en emulador: generar puzzle on-demand → jugar → seleccionar celda → escribir número → resaltado de iguales → contador de errores → **force-stop + reabrir → "Continuar" restaura tablero, entradas y timer**.

## Calibración del rater (hecha, mergeada)

`DifficultyRater` ajustado contra **510 puzzles reales rankeados** (sudoku-exchange
puzzle bank). **Spearman ρ = 0.86** vs rating humano; banda predicha de cada puzzle
a ≤1 de su objetivo. Pesos, bandas y `CampaignCurve` re-anclados al rango de score
que el generador realmente produce. Guards + búsqueda reproducible en
`RaterCalibrationTest`. Detalle en spec §10.

## Bordes conocidos (no bloqueantes)

- Primera generación on-demand bloquea ~1–3 s con spinner (el buffer se llena en background después).
- Ícono de launcher es un placeholder de color plano.
- **Hueco de dificultad media-alta**: el solver del MVP llega hasta X-Wing → distribución bimodal
  de dificultad, poca cobertura en el rango DIFICIL. Fix real = agregar técnicas al solver
  (XY-Wing, colouring, Y-Wing). Spec §10.2. **Decisión del usuario: diferido.**
- `MigrationTest` diferido hasta que exista un esquema v2.

## Bug encontrado y corregido jugando en el emulador (2026-09-04)

Al completar un puzzle y volver a Home, la partida completada quedaba "resucitada"
como partida en curso — Home seguía ofreciendo "Continuar" sobre el mismo puzzle
en vez de avanzar. Causa raíz **determinista**: salir de la pantalla de juego
dispara `ON_PAUSE` sobre el mismo `GameViewModel` antes de destruirlo, lo que
volvía a guardar la partida justo después de que `CompletePuzzle` la había
limpiado. Arreglado con un flag `canPersist` + mutex que bloquea cualquier
escritura mientras corre una acción terminal (avanzar/reintentar/abandonar/
terminar), documentado y con tests de regresión en `GameViewModelTest`.
Verificado en el emulador leyendo la base Room directamente y jugando dos
puzzles completos por los dos caminos ("Siguiente puzzle" y "Volver a Home").

## Nivel fácil más accesible (2026-09-04)

El usuario probando la app pidió bajar la dificultad del nivel fácil para
"enganchar" mejor a nuevos jugadores. Causa: el puzzle #1 ya se tallaba a su
mínimo práctico (~25 pistas). Se agregó un piso de pistas (`minGivens`) que
empieza en 40 para el puzzle #1 y decae hasta el tallado normal (24) hacia el
puzzle #15. Verificado en dispositivo: puzzle #1 ahora tiene 40 pistas y banda
**Principiante** (antes: ~25 pistas, banda Fácil).

## Verificación del usuario

El usuario probó el **modo con límite de 3 errores** en su propio dispositivo y
confirmó que funciona correctamente (celda marcada, contador, fin de partida al
tercer error).

## Estado

Todo mergeado a `master`. MVP funcional + rater calibrado + bug de finalización
corregido + límite de errores confirmado por el usuario. Ver `docs/plans/*` y
`docs/designs/*` para detalles y desvíos.
