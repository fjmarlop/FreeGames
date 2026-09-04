# FreeSudoku — progreso (autónomo, 2026-09-04)

Rama: `feat/plan-1-scaffold-domain` · todo commiteado.
**MVP funcional end-to-end verificado en emulador (API 35).**

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

## Bordes conocidos (no bloqueantes)

- Primera generación on-demand bloquea ~1–3 s con spinner (el buffer se llena en background después).
- Ícono de launcher es un placeholder de color plano.
- Falta afinar los pesos del `DifficultyRater` contra un set de puzzles etiquetados por humanos (solo mueve constantes).
- `MigrationTest` diferido hasta que exista un esquema v2.

## Siguiente paso

Integrar la rama (merge a `main`). Ver `docs/plans/*` para los detalles de cada plan y sus desvíos.
