# "Reiniciar" genera un puzzle nuevo — Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Que la acción "Reiniciar" de la pantalla de juego descarte el intento y arranque un puzzle NUEVO del mismo nivel de campaña, en vez de repetir el puzzle actual.

**Architecture:** Se agrega `GameViewModel.onRestart()` (gemela de `onRetry()` pero tomando el puzzle de `getNextCampaignPuzzle()` en vez de `snapshot.puzzle`). `onRetry()` queda sólo para el diálogo de derrota. La UI cablea un nuevo callback `GameCallbacks.onRestart` y el diálogo de confirmación de reinicio lo invoca.

**Tech Stack:** Kotlin, Jetpack Compose, MVVM, Hilt, Coroutines/Flow, JUnit4 + MockK + Truth (unit), Compose UI Test (instrumented).

**Platform:** Android

**Spec:** `docs/designs/2026-09-05-restart-generates-new-puzzle-design.md`

---

### Task 1: `GameViewModel.onRestart()`

**Files:**
- Modify: `app/src/main/java/com/freesudoku/app/ui/game/GameViewModel.kt` (add `onRestart()` after `onRetry()`, ~line 175; update `onRetry()` KDoc)
- Test: `app/src/test/java/com/freesudoku/app/ui/game/GameViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

En `GameViewModelTest.kt`, añadir un segundo puzzle de campaña y el test de `onRestart`. Justo debajo de la propiedad `puzzle` (después de la línea 44):

```kotlin
    private val puzzle2 = Puzzle(
        "p2", 5,
        GridCodec.decode("000000000000000000000000000000000000000000000000000000000000000000000000000000000"),
        GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        14.5, DifficultyBand.FACIL,
    )
```

Y como test nuevo, junto al de `onRetry` (después de la línea 143):

```kotlin
    @Test fun `onRestart swaps in a brand-new campaign puzzle without advancing`() = runTest {
        val getNext = mockk<GetNextCampaignPuzzle>()
        val startGame = mockk<StartGame>()
        val complete = mockk<CompletePuzzle>(relaxed = true)
        coEvery { getNext() } returns puzzle2
        coEvery { startGame(puzzle2, any()) } returns freshSnapshot().copy(puzzle = puzzle2)
        val vm = build(getNext = getNext, startGame = startGame, completePuzzle = complete)

        vm.onCellTap(2)
        vm.onNumberInput(4) // correct
        vm.onCellTap(3)
        vm.onNumberInput(9) // wrong -> a mistake
        assertThat(vm.uiState.value.mistakes).isEqualTo(1)

        vm.onRestart()
        advanceUntilIdle()

        coVerify(exactly = 1) { getNext() }
        coVerify(exactly = 1) { startGame(puzzle2, any()) }
        coVerify(exactly = 0) { complete(any()) } // restarting is not completing; campaign stays put
        assertThat(vm.uiState.value.mistakes).isEqualTo(0)
        assertThat(vm.uiState.value.status).isEqualTo(GameStatus.IN_PROGRESS)
        assertThat(vm.uiState.value.canUndo).isFalse()
        assertThat(vm.uiState.value.cells[2].value).isEqualTo(0)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.ui.game.GameViewModelTest"`
Expected: FAIL — `onRestart` no existe (unresolved reference).

- [ ] **Step 3: Implement `onRestart()`**

En `GameViewModel.kt`, actualizar el KDoc de `onRetry()` para acotarlo al caso derrota:

```kotlin
    /**
     * Reintenta el MISMO puzzle desde cero (mismos givens/solución, tablero/timer/errores/historial
     * limpios). Usada sólo por el diálogo de derrota ("Reintentar").
     */
    fun onRetry() {
```

Y agregar, inmediatamente después del bloque de `onRetry()`:

```kotlin
    /**
     * "Reiniciar" en partida en curso: descarta el intento y arranca un puzzle NUEVO del mismo
     * nivel de campaña (mismo target de dificultad). No avanza la campaña ni registra nada — el
     * puntero de campaña no se movió, así que [getNextCampaignPuzzle] devuelve una instancia
     * nueva para el número actual. El diálogo de confirmación vive en la UI.
     */
    fun onRestart() {
        runTerminalAction {
            _uiState.value = _uiState.value.copy(loading = true)
            snapshot = startGame(getNextCampaignPuzzle(), settings)
            canPersist = true
            selected = null
            render()
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.ui.game.GameViewModelTest"`
Expected: PASS (incluye el `onRetry restarts the same puzzle from scratch mid-game` existente, que sigue verde sin cambios).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/ui/game/GameViewModel.kt app/src/test/java/com/freesudoku/app/ui/game/GameViewModelTest.kt
git commit -m "feat(game): onRestart() starts a new puzzle at the same campaign level"
```

---

### Task 2: Cablear `onRestart` en la UI y actualizar el diálogo

**Files:**
- Modify: `app/src/main/java/com/freesudoku/app/ui/game/GameScreen.kt` (campo en `GameCallbacks`, wiring en `GameScreen`, `onClick` y texto del `AlertDialog` de reinicio)
- Test: `app/src/androidTest/java/com/freesudoku/app/ui/GameContentTest.kt`

- [ ] **Step 1: Actualizar los tests instrumentados (fallan primero)**

En `GameContentTest.kt`:

1. En `noopCallbacks(...)` agregar `onRestart = {},` a la construcción de `GameCallbacks` (junto a `onRetry = {}`).
2. Reescribir `restart_action_asks_for_confirmation_before_calling_back` para verificar `onRestart`:

```kotlin
    @Test fun restart_action_asks_for_confirmation_before_calling_back() {
        var restarted = false
        composeRule.setContent {
            FreeSudokuTheme {
                GameContent(
                    state = GameUiState(loading = false, cells = cells(), puzzleNumber = 3),
                    callbacks = noopCallbacks().copy(onRestart = { restarted = true }),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Reiniciar puzzle").performClick()
        composeRule.onNodeWithText("Reiniciar puzzle").assertIsDisplayed()
        assertThat(restarted).isFalse() // el diálogo no actúa hasta confirmar

        composeRule.onNodeWithText("Cancelar").performClick()
        assertThat(restarted).isFalse()

        composeRule.onNodeWithContentDescription("Reiniciar puzzle").performClick()
        composeRule.onNodeWithText("Reiniciar").performClick()
        assertThat(restarted).isTrue()
    }
```

`restart_action_is_hidden_once_the_puzzle_is_over` y `failed_status_shows_the_dialog` quedan sin cambios.

- [ ] **Step 2: Verificar que no compila / falla**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: FAIL — `onRestart` no es parámetro de `GameCallbacks` / `copy`.

- [ ] **Step 3: Implementar en `GameScreen.kt`**

1. En `data class GameCallbacks`, agregar el campo (junto a `onRetry`):

```kotlin
    val onRetry: () -> Unit,
    val onRestart: () -> Unit,
```

2. En `GameScreen(...)`, dentro de `GameCallbacks(...)`:

```kotlin
            onRetry = viewModel::onRetry,
            onRestart = viewModel::onRestart,
```

3. En el `AlertDialog` de `showRestartConfirm`, cambiar el `confirmButton` para llamar a `onRestart` y actualizar el texto:

```kotlin
        AlertDialog(
            onDismissRequest = { showRestartConfirm = false },
            title = { Text("Reiniciar puzzle") },
            text = {
                Text(
                    "Se genera un puzzle nuevo del mismo nivel. El tiempo y los errores " +
                        "vuelven a cero y pierdes lo resuelto hasta ahora.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestartConfirm = false
                        callbacks.onRestart()
                    },
                ) { Text("Reiniciar") }
            },
            dismissButton = {
                TextButton(onClick = { showRestartConfirm = false }) { Text("Cancelar") }
            },
        )
```

- [ ] **Step 4: Compilar y correr los tests instrumentados**

Run: `./gradlew :app:testDebugUnitTest` (regresión completa unit — el cambio en `GameCallbacks` no debe romper nada)
Run: `./gradlew :app:connectedDebugAndroidTest --tests "com.freesudoku.app.ui.GameContentTest"`
Expected: PASS ambos. (Requiere emulador `Medium_Phone_API_35` corriendo.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/ui/game/GameScreen.kt app/src/androidTest/java/com/freesudoku/app/ui/GameContentTest.kt
git commit -m "feat(ui): wire Reiniciar to onRestart and reword its confirm dialog"
```

---

### Task 3: Verificación end-to-end, docs y cierre

**Files:**
- Modify: `docs/PROGRESS.md` (nueva sección)

- [ ] **Step 1: Suite completa + lint**

Run: `./gradlew :app:testDebugUnitTest :app:lintDebug`
Expected: unit tests verdes, lint sin issues nuevos.

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: instrumentados verdes.

- [ ] **Step 2: Verificación manual en el emulador**

1. Instalar: `./gradlew :app:installDebug`
2. Abrir la app, entrar a una partida en curso, resolver 2–3 celdas, dejar correr el timer.
3. Tocar "Reiniciar" (barra superior) → confirmar en el diálogo (texto nuevo, sin "#N").
4. Verificar: tablero limpio, timer 0, errores 0, **givens distintos** a los de antes (puzzle nuevo), sigue siendo el mismo número de puzzle en el header.
5. Volver a Home → "Continuar" retoma esta partida nueva (persistió). El número de campaña no cambió.
6. Forzar derrota (3 errores) en un puzzle → el diálogo "Perdiste" → "Reintentar" → **mismo puzzle** (givens iguales), timer/errores a cero.

- [ ] **Step 3: Actualizar `docs/PROGRESS.md`**

Agregar al final:

```markdown
## "Reiniciar" genera un puzzle nuevo (2026-09-05)

Antes, la acción "Reiniciar" (barra superior, partida en curso) repetía el mismo
puzzle. Ahora genera uno nuevo del mismo nivel de campaña: `GameViewModel.onRestart()`
toma el puzzle de `getNextCampaignPuzzle()` (el puntero de campaña no avanzó) en
vez de `snapshot.puzzle`. La campaña no avanza y nada se registra. El diálogo de
derrota "Reintentar" queda igual (mismo puzzle) — decisión del usuario.

Spec: `docs/designs/2026-09-05-restart-generates-new-puzzle-design.md`.
```

- [ ] **Step 4: Commit**

```bash
git add docs/PROGRESS.md
git commit -m "docs: record Reiniciar generating a fresh puzzle"
```

- [ ] **Step 5: Finalizar la rama**

Usar `mobiai-mobile-finishing-branch`: verificar tests, presentar opciones de integración, ejecutar la elección.

---

## Self-review

- **Cobertura del spec:** decisión 1 (mismo número de campaña) → `getNextCampaignPuzzle()` en Task 1. Decisión 2 ("Reintentar" sin cambios) → `onRetry()` intacto + test de derrota preservado. Decisión 3 (campaña no avanza) → `coVerify(exactly = 0) { complete(any()) }` en Task 1. `loading` → `_uiState.copy(loading = true)` en Task 1. Texto del diálogo → Task 2 Step 3.
- **Sin placeholders:** todo el código está escrito.
- **Consistencia de tipos:** `onRestart: () -> Unit` sigue la forma de los demás callbacks de `GameCallbacks`; `onRestart()` en el VM no toma args, igual que `onRetry()`.
- **Riesgo de colisión entre tareas:** Task 1 y Task 2 tocan archivos distintos salvo que ambas dependen del orden (Task 2 usa `viewModel::onRestart` de Task 1). Ejecutar en orden, no en paralelo.
