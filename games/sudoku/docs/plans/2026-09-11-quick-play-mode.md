# Modo "Partida rápida" — Implementation Plan

**Goal:** Nuevo modo desde Home: elegir dificultad (Fácil/Medio/Difícil/Experto), jugar un puzzle suelto sin afectar la campaña.

**Spec:** `docs/designs/2026-09-11-quick-play-mode-design.md`

---

### Task 1 — Dominio/datos: generar y completar puzzles sin número de campaña

**Files:** `domain/model/DifficultyBand.kt`, `data/repository/PuzzleRepository.kt`, `domain/usecase/GameUseCases.kt`, tests correspondientes.

1. `DifficultyBand`: agregar `QUICK_PLAY_SELECTABLE = listOf(FACIL, MEDIO, DIFICIL, EXPERTO)` (comentario: MAESTRO excluido, techo del generador).
2. `PuzzleRepository.puzzleForBand(band)`: mapa banda→score objetivo (14/31/44/52), `factory.generateForTarget(target)`. Test: puzzle resultante tiene `number == null` y `band` razonable.
3. `GetPuzzleForBand` use case (wrapper). Test análogo a `GetNextCampaignPuzzle`.
4. `CompletePuzzle`: si `snapshot.puzzle.number == null` → insertar con `puzzleNumber = -1` (mismo centinela que `GameRepository`), NO llamar `advanceAfterCompleting`. Test: verifica `campaignRepo.advanceAfterCompleting` nunca invocado para un snapshot con `number = null`, y que sí se inserta en el DAO.

### Task 2 — `GameViewModel` / pantalla de juego: modo dual

**Files:** `ui/game/GameUiState.kt`, `ui/game/GameViewModel.kt`, `ui/components/GameTopStatus.kt`, `ui/components/ResultSheet.kt`, `ui/game/GameScreen.kt`, tests.

1. `GameUiState.puzzleNumber: Int` → `Int?`.
2. `GameViewModel`: inyectar `GetPuzzleForBand`; helper `private suspend fun nextPuzzleForCurrentMode(): Puzzle` que elige `getNextCampaignPuzzle()` o `getPuzzleForBand(snapshot.puzzle.band)` según `snapshot.puzzle.number`. `onAdvance`/`onRestart` lo usan en vez de llamar `getNextCampaignPuzzle()` directo.
3. `GameTopStatus`: `puzzleNumber: Int?`; chip "Partida rápida" si null.
4. `ResultSheet`: nuevo parámetro `isQuickPlay: Boolean`, texto del botón condicional.
5. Tests: `GameViewModelTest` — nuevo caso "completar una partida rápida no avanza campaña y Siguiente da otro puzzle de la misma banda"; `GameContentTest`/`GameTopStatus` — chip "Partida rápida" cuando `puzzleNumber == null`.

### Task 3 — Entrada desde Home: selector de dificultad

**Files:** `ui/home/HomeUiState.kt` (o inline en `HomeViewModel`), `ui/home/HomeViewModel.kt`, `ui/home/HomeScreen.kt`, nuevo composable de selector (bottom sheet), tests.

1. `HomeUiState.currentNumber: Int` → `Int?` (null = hay partida rápida guardada). `ContinuePuzzleCard` branchea el chip.
2. `HomeViewModel`: nueva función `onStartQuickPlay(band, onReady)` — genera vía `GetPuzzleForBand`, `startGame`, luego `onReady()` (mismo patrón que `onPlayOrContinue`).
3. Botón "Partida rápida" en `HomeContent` que abre un bottom sheet con los 4 chips de banda (`DifficultyBand.QUICK_PLAY_SELECTABLE`). Si `hasResumableGame`, confirmar antes de reemplazar.
4. Tests: `HomeViewModelTest` (nuevo caso), `HomeContentTest` (sheet se abre, elegir banda dispara `onStartQuickPlay`, confirmación si hay partida en curso).

### Task 4 — Verificación + docs

1. `./gradlew :games:sudoku:testDebugUnitTest :games:sudoku:lintDebug :games:sudoku:verifyDebugPermissions`.
2. `./gradlew :games:sudoku:connectedDebugAndroidTest`.
3. Manual en emulador: Home → Partida rápida → Difícil → jugar → completar → Estadísticas refleja el completado, campaña sin avanzar; "Reiniciar" y "Otro puzzle rápido" dan puzzles nuevos de la misma banda.
4. `docs/PROGRESS.md` + memoria (`freegames-monorepo.md`).
