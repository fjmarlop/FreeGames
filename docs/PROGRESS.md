# FreeSudoku — progreso (autónomo, 2026-09-04)

Rama: `feat/plan-1-scaffold-domain` · todo commiteado.

## Hecho

### Plan 1 — Scaffold + motor de dominio ✅ (65 tests JVM verdes, `assembleDebug` + `lintDebug` verdes)
- Proyecto Gradle: **Gradle 9.6 + AGP 9.3.1 (Kotlin built-in) + Hilt 2.60.1 + Compose BOM 2026.08**, `compileSdk 37 / minSdk 24`.
  (El toolchain del equipo forzó AGP 9 en vez del 8.13 que decía el plan; detalle en el doc del plan.)
- `domain/model`: `Grid` + codec, `DifficultyBand`, `Puzzle`, `Board`, `Move`, `GameSnapshot`.
- `domain/generator`: `FullGridGenerator` (backtracking), `PuzzleCarver` (quita pistas manteniendo solución única), `PuzzleFactory` (genera apuntando a un score objetivo).
- `domain/solver`: `SolverState` (candidatos por bitmask), `SolutionCounter`, técnicas humanas (naked/hidden single, locked candidates, naked/hidden subsets, X-Wing), `LogicalSolver`, `DifficultyRater` (score continuo).
- `domain/campaign`: `CampaignCurve` (curva creciente saturante + ruido determinista).
- `domain/game`: `GameEngine` puro (jugadas, deshacer/rehacer, notas, pistas, límite de 3 errores, modo relajado, auto-borrado de notas, timer, detección de completado).

### Plan 2 — Capa de datos ✅ (79 tests JVM verdes; instrumentados compilan, corriendo en emulador)
- Room v1: `puzzle_buffer`, `current_game`, `completed_puzzle`, `campaign_progress` + DAOs + schema exportado.
- `GameSnapshotDto`: serialización JSON de la partida en curso (round-trip testeado).
- `SettingsRepository` sobre DataStore (5 settings, defaults).
- Repos: `PuzzleRepository` (buffer + refill en background), `GameRepository`, `CampaignRepository`, `StatsRepository` (rachas por día).
- Use cases: `GetNextCampaignPuzzle`, `StartGame`, `ResumeGame`, `ObserveCurrentGame`, `SaveGame`, `CompletePuzzle`, `AbandonGame`, observers.
- Hilt: `DatabaseModule`, `SettingsModule`, `DomainModule`, `DispatchersModule`.
- Tests instrumentados de DAOs (corriendo en emulador ahora).

## Pendiente

### Plan 3 — UI (en preparación)
Tema + Navigation Compose + pantallas Home / Juego / Estadísticas / Ajustes + ViewModels + componentes (`SudokuBoard`, `NumberPad`, toolbar, timer, contador de errores, hoja de resultado) + ciclo de vida/timer + generación on-demand con estado de carga + accesibilidad + edge-to-edge + tests de UI y de ViewModel.

Al terminar Plan 3: verificación completa y merge de la rama.
