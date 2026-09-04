# FreeSudoku — Plan 3: UI Layer

> **For agentic workers:** `mobiai-mobile-executing-plans`. Checkbox steps track progress.

**Goal:** A playable app: Home (campaign card + continue), Game (board, number pad, tools, timer, 3-mistake limit, result), Stats, Settings — all MVVM + UDF over the Plan 2 use cases.

**Architecture:** Jetpack Compose + Material 3, Navigation Compose, `hilt-navigation-compose` ViewModels exposing `StateFlow<UiState>`. Game truth = `GameSnapshot` in the VM, persisted debounced via `SaveGame`. Transient UI (selected cell, notes mode) in `SavedStateHandle`.

**Platform:** Android. **Depends on:** Plans 1–2.

---

## Spec reference

Implements §5 (all screens + components), §6 (data flow), the §7 rows that need UI
(loading state, resume, retry-after-fail), §8.3 Compose UI tests, §8.1 ViewModel tests.

---

## File map

```
app/src/main/java/com/freesudoku/app/
├── MainActivity.kt                     (rewrite: NavHost + theme)
├── ui/
│   ├── theme/{Theme,Color,Type}.kt     (Theme: honor ThemeMode)
│   ├── AppViewModel.kt                 (themeMode stream)
│   ├── navigation/
│   │   ├── Destinations.kt
│   │   └── FreeSudokuNavHost.kt
│   ├── home/{HomeScreen,HomeViewModel,HomeUiState}.kt
│   ├── game/{GameScreen,GameViewModel,GameUiState}.kt
│   ├── stats/{StatsScreen,StatsViewModel,StatsUiState}.kt
│   ├── settings/{SettingsScreen,SettingsViewModel}.kt
│   └── components/
│       ├── SudokuBoard.kt
│       ├── NumberPad.kt
│       ├── GameToolbar.kt
│       ├── GameTopStatus.kt            (timer + mistakes 0/3)
│       └── ResultSheet.kt
└── ui/format/TimeFormat.kt             (ms -> "mm:ss")

tests:
app/src/test/java/com/freesudoku/app/ui/game/GameViewModelTest.kt
app/src/test/java/com/freesudoku/app/ui/home/HomeViewModelTest.kt
app/src/test/java/com/freesudoku/app/ui/stats/StatsViewModelTest.kt
app/src/test/java/com/freesudoku/app/ui/format/TimeFormatTest.kt
app/src/androidTest/java/com/freesudoku/app/ui/GameScreenTest.kt
app/src/androidTest/java/com/freesudoku/app/ui/HomeScreenTest.kt
```

---

## Task 1: TimeFormat + theme wiring + AppViewModel

- [ ] `ui/format/TimeFormat.kt`: `fun formatDuration(ms: Long): String` -> `H:MM:SS` if ≥1h else `M:SS`. Test: 0 -> "0:00", 65_000 -> "1:05", 3_725_000 -> "1:02:05".
- [ ] `AppViewModel` (`@HiltViewModel`): `val themeMode: StateFlow<ThemeMode>` from `SettingsRepository.settings.map { it.themeMode }`, `stateIn(SYSTEM)`.
- [ ] `ui/theme/Theme.kt`: add `themeMode: ThemeMode` param; `darkTheme = when(themeMode){LIGHT->false; DARK->true; SYSTEM->isSystemInDarkTheme()}`.
- [ ] commit `feat(ui): time formatting + theme mode wiring`

---

## Task 2: Navigation

- [ ] `ui/navigation/Destinations.kt`
```kotlin
package com.freesudoku.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val GAME = "game"
    const val STATS = "stats"
    const val SETTINGS = "settings"
}
```
- [ ] `ui/navigation/FreeSudokuNavHost.kt` — `NavHost(startDestination = Routes.HOME)`:
  - `composable(HOME)` -> `HomeScreen(onPlay = { navController.navigate(GAME) }, onStats = {...}, onSettings = {...})`
  - `composable(GAME)` -> `GameScreen(onExit = { navController.popBackStack() })`
  - `composable(STATS)` / `composable(SETTINGS)` -> screen + back.
- [ ] `MainActivity` rewrite: `enableEdgeToEdge()`, collect `appViewModel.themeMode`, wrap `FreeSudokuTheme(themeMode) { FreeSudokuNavHost() }`.
- [ ] Build check `:app:assembleDebug`. commit `feat(ui): navigation graph + themed NavHost`.

---

## Task 3: Home

- [ ] `HomeUiState`
```kotlin
data class HomeUiState(
    val loading: Boolean = true,
    val currentNumber: Int = 1,
    val currentBand: DifficultyBand? = null,
    val hasResumableGame: Boolean = false,
    val stats: PlayerStats = PlayerStats.EMPTY,
    val preparingPuzzle: Boolean = false,
)
```
- [ ] `HomeViewModel` (`@HiltViewModel`): combine `ObserveCampaignProgress()`, `ObserveCurrentGame()` (→ `hasResumableGame` + band from its puzzle), `ObservePlayerStats()`. `fun onPlayOrContinue(onReady: () -> Unit)`: if resumable → just `onReady()`; else set `preparingPuzzle=true`, `viewModelScope.launch { val p = getNextCampaignPuzzle(); startGame(p, settingsRepository.settings.first()); preparingPuzzle=false; onReady() }`.
- [ ] `HomeScreen`: `Scaffold`; big card with `#N` + band label + a `Continuar`/`Jugar puzzle N` button (shows spinner when `preparingPuzzle`); a stats strip (best time, current streak, total); icon buttons to Stats & Settings. `contentDescription`s; touch targets ≥48dp.
- [ ] Test `HomeViewModelTest`: resumable game → `onPlayOrContinue` calls `onReady` without `GetNextCampaignPuzzle`; no game → calls `GetNextCampaignPuzzle` + `StartGame` then `onReady`.
- [ ] commit `feat(ui): home screen`

---

## Task 4: Game — ViewModel

- [ ] `GameUiState`
```kotlin
data class GameUiState(
    val loading: Boolean = true,
    val cells: List<CellUi> = emptyList(),      // 81, row-major
    val selected: Int? = null,
    val notesMode: Boolean = false,
    val mistakes: Int = 0,
    val mistakeLimitEnabled: Boolean = true,
    val elapsedText: String = "0:00",
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val remainingPerDigit: Map<Int, Int> = emptyMap(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val puzzleNumber: Int = 0,
    val hintsUsed: Int = 0,
)
data class CellUi(
    val value: Int, val given: Boolean, val notes: Set<Int>,
    val error: Boolean,            // highlightErrors && wrong vs solution
    val inSelectionScope: Boolean, // same row/col/box as selection
    val sameValueAsSelection: Boolean,
)
```
- [ ] `GameViewModel` (`@HiltViewModel`, injects `ObserveCurrentGame`, `ResumeGame`, `GetNextCampaignPuzzle`, `StartGame`, `SaveGame`, `CompletePuzzle`, `AbandonGame`, `SettingsRepository`, `SavedStateHandle`).
  - `private var snapshot: GameSnapshot`. `private val engine: GameEngine` built from `settings.autoRemoveNotes`.
  - **init**: `viewModelScope.launch { val s = resumeGame() ?: startGame(getNextCampaignPuzzle(), settings.first()); bind(s) }`.
  - `bind(s)`: set `snapshot`, recompute `_uiState` via `render(s, settings, selected, notesMode)`.
  - **render** derives `CellUi` list (error = `settings.highlightErrors && value!=0 && value != solution`), `remainingPerDigit` (9 minus count on board of each digit), `canUndo/redo`.
  - Events: `onCellTap(i)`, `onNumberInput(d)` (→ `engine.setValue` or `engine.toggleNote` depending on `notesMode`; if `selected==null` ignore), `onErase()`, `onToggleNotesMode()`, `onUndo()`, `onRedo()`, `onHint()` (`engine.hint(snapshot, r, c)` if a cell selected & empty, else `engine.autoHint(snapshot)`).
  - After every mutating event: `snapshot = new; persistDebounced(); render(); if (status==COMPLETED) onCompleted()`.
  - **timer**: `viewModelScope.launch { while(isActive){ delay(1000); if(snapshot.status==IN_PROGRESS && screenResumed){ snapshot = engine.tick(snapshot,1000); render() } } }` — `screenResumed` toggled by `onResume()/onPause()` from the screen's lifecycle. Persist elapsed on pause.
  - **persistDebounced**: `saveJob?.cancel(); saveJob = viewModelScope.launch { delay(1000); saveGame(snapshot) }`. Also force-save (no delay) on `onPause()` and in `onCleared()` via a non-cancellable `@ApplicationScope` write — inject `@ApplicationScope CoroutineScope` for the final flush.
  - `onCompleted()`: `_uiState.update { it.copy(status=COMPLETED) }` — the screen shows `ResultSheet`; its "Siguiente" button calls `vm.onAdvance(onReady)` = `completePuzzle(snapshot); startGame(getNextCampaignPuzzle(), settings.first())`.
  - `onFailed` (status==FAILED): screen shows a dialog; "Reintentar" -> `vm.onRetry()` = `startGame(snapshot.puzzle.copy(number=…), settings.first())` (same puzzle, fresh snapshot); "Salir" -> `abandonGame()` + `onExit()`.
  - `onExitRequested()`: force-save; `onExit()` (does NOT abandon — game stays resumable).
- [ ] `GameViewModelTest` (Turbine, `SavedStateHandle()` real, use-cases mocked, `GameEngine` real via a fake `SettingsRepository`):
  - resumes an existing snapshot without generating.
  - correct number input updates the cell, no mistake.
  - wrong input in limit mode: 3rd → `status == FAILED`.
  - `onToggleNotesMode` then number input writes a note.
  - completing every cell → `status == COMPLETED` and `CompletePuzzle` NOT called until `onAdvance`.
  - `onUndo` reverts.
- [ ] commit `feat(ui): game view model`

---

## Task 5: Game — Compose

- [ ] `components/SudokuBoard.kt` — `Canvas`/`Box` grid: 9×9, thick 3px lines on box borders, 1px elsewhere; cell background by state (selected > sameValue > inScope > default), text red when `error`, given cells bold. `Modifier.aspectRatio(1f)`. `onCellClick(index)`. Pencil notes: 3×3 mini-grid of digits when `value==0 && notes.isNotEmpty()`. `testTag("cell_$i")`, `contentDescription`.
- [ ] `components/NumberPad.kt` — row(s) of 1–9 buttons; each shows `remaining` count small; disabled/dim when `remaining==0`. `testTag("pad_$d")`. Height ≥48dp.
- [ ] `components/GameToolbar.kt` — Undo, Redo, Erase, Notes (toggle, shows on/off), Hint. Icons + labels, `contentDescription`.
- [ ] `components/GameTopStatus.kt` — `TimerText` + `Errores n/3` (hidden if `!mistakeLimitEnabled`, show plain "Errores: n" instead).
- [ ] `components/ResultSheet.kt` — `ModalBottomSheet`: "¡Completado!", time, mistakes, hints; buttons *Siguiente puzzle* / *Volver a Home*.
- [ ] `GameScreen.kt` — `Scaffold` topBar (back = `onExitRequested`), column: `GameTopStatus`, `SudokuBoard`, `GameToolbar`, `NumberPad`. Wire lifecycle: `LifecycleEventEffect(ON_RESUME/ON_PAUSE)` -> `vm.onResume()/onPause()`. `if (status==COMPLETED) ResultSheet(...)`. `if (status==FAILED) AlertDialog(...)`. Loading spinner while `loading`.
- [ ] commit `feat(ui): game screen + board components`

---

## Task 6: Stats + Settings

- [ ] `StatsViewModel`: `ObservePlayerStats()` -> `StateFlow<PlayerStats>`. `StatsScreen`: cards — total, best time, avg time, current streak, longest streak; a simple bar row of `completedByBand`. Read-only. Back arrow.
- [ ] `SettingsViewModel`: `SettingsRepository.settings` -> state; setter functions delegate to repo. `SettingsScreen`: `Switch` rows for the 4 booleans + a 3-option theme selector (`SYSTEM/LIGHT/DARK`). Changes apply live.
- [ ] `StatsViewModelTest`: emits mapped stats from a fake flow.
- [ ] commit `feat(ui): stats + settings screens`

---

## Task 7: Compose UI tests (emulator)

- [ ] `HomeScreenTest` — with a fake/hilt-test graph OR by hoisting state: assert "Jugar puzzle 1" shows when no resumable game; tapping it triggers the play callback.
- [ ] `GameScreenTest` — feed a fixed `GameUiState` to a stateless `GameContent(state, callbacks)` (split screen into stateful + stateless): tap `cell_2`, tap `pad_4` → callback `onNumberInput(4)`; notes toggle changes pad behavior; when `state.status==COMPLETED` the result sheet shows; `mistakes=3` path shows the dialog.
  > Prefer testing the **stateless** `GameContent` with hoisted state — no Hilt, fast, deterministic. Keep one small end-to-end `@HiltAndroidTest` that launches `MainActivity`, waits for the board, types the full solution of a seeded puzzle, and asserts the result sheet — gate on emulator availability.
- [ ] commit `test(ui): compose ui tests for home + game`

---

## Task 8: Verify + finish

- [ ] `./gradlew :app:testDebugUnitTest` green (all plans).
- [ ] `./gradlew :app:lintDebug` green.
- [ ] `./gradlew :app:connectedDebugAndroidTest` green (emulator) — or record which are gated.
- [ ] `./gradlew :app:installDebug` + a manual smoke (screenshot Home + Game) if an emulator is up.
- [ ] Update status + "Execution outcome" section.
- [ ] Hand to `mobiai-mobile-finishing-branch`.

---

## Self-review

- **§5 screens** — Tasks 3–6 ✅. **§5.6 components** — Task 5 ✅.
- **§6 data flow** — Task 4: VM holds `GameSnapshot`, renders `GameUiState`, debounced `SaveGame`, `SavedStateHandle` for transient UI ✅.
- **§7**: preparing-puzzle loading (Task 3), resume (Task 4 init), retry after FAIL (Task 4) ✅. Corrupt-save handling already in `GameSnapshotDto.fromJsonOrNull` → `ResumeGame` returns null → fresh game ✅.
- **§8.1 VM tests / §8.3 UI tests** — Tasks 4, 6, 7 ✅.
- **Accessibility** (§10 delivery step): contentDescription + 48dp targets called out per component; add a `testTag`s pass.
- **Placeholder scan**: screen composition details ("a card", "a bar row") are deliberately left to implementation taste — no fake values, no TODOs in logic. Board rendering + VM state machine (the only real complexity) are spelled out.

---

## Execution outcome (2026-09-04)

**STATUS: COMPLETE.** Branch `feat/plan-1-scaffold-domain`. 93 JVM tests + 9
instrumented tests green; `assembleDebug` / `lintDebug` green. Manual smoke on
API-35 emulator: on-demand generation, cell select + input, same-value
highlight, remaining-per-digit counts, mistake counter, timer, force-stop →
"Continuar" → board + timer + entries all restored.

**Deviations:**
- **Timer**: extracted behind a `Ticker` fun-interface (`RealTicker` +
  `di/UiModule` `@Binds`) instead of an in-VM `while+delay` loop — makes
  `GameViewModel` testable without virtual-clock fighting.
- **Icons**: no `material-icons-extended` dependency; top-bar actions are
  `TextButton`s ("Estadísticas"/"Ajustes", "Salir", "Atrás") — accessible and
  keeps the APK smaller.
- **HomeUiState.currentBand** is populated from the resumable game's puzzle
  (campaign progress carries only a number), shown as "—" when there is no
  in-progress game.
- Screens are split stateful (`HomeScreen`/`GameScreen`) + stateless
  (`HomeContent`/`GameContent`/`StatsContent`/`SettingsContent`); Compose tests
  drive the stateless halves — no Hilt test graph needed.
- No `@HiltAndroidTest` end-to-end test (the stateless UI tests + manual smoke
  cover it); add one if regressions appear.
- Extra files: `ui/common/BandLabels.kt`, `ui/game/Ticker.kt`, `di/UiModule.kt`,
  `util/MainDispatcherRule.kt` (test).

**Known rough edges (not blockers):**
- On-demand first puzzle generation blocks ~1–3 s with a spinner (buffer empties
  on first run; background refill fills it after). Acceptable; could pre-warm on
  first launch.
- Placeholder launcher icon (flat colour).
- Board sizing is generous; could tighten vertical rhythm on small screens.
