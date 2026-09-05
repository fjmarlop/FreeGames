# "Reiniciar" genera un puzzle nuevo — Design

**Fecha:** 2026-09-05
**Plataforma:** Android (Jetpack Compose, MVVM)
**Alcance:** cambio de comportamiento de una acción existente. Sin funciones nuevas.

---

## Contexto

La acción "Reiniciar" (barra superior de la pantalla de juego, visible solo con
`GameStatus.IN_PROGRESS`, agregada en `e396607`) hoy reinicia **el mismo puzzle**:
mismos givens/solución, tablero limpio, timer/errores/historial a cero.

Reutiliza `GameViewModel.onRetry()`, que también sirve al botón "Reintentar" del
diálogo de derrota (3 errores).

## Pedido

Que "Reiniciar" **genere un puzzle nuevo** del mismo nivel de dificultad, en vez
de repetir el actual.

## Decisiones

1. **"Nivel de dificultad" = mismo número de campaña.** El juego es una campaña
   con curva de dificultad continua; cada número de campaña tiene un target de
   score. "Reiniciar" mantiene la posición de campaña y sólo cambia la instancia
   del puzzle, apuntando al mismo target.

2. **El diálogo de derrota "Reintentar" NO cambia** (decisión del usuario, opción
   B). Sigue dando el mismo puzzle: "reintentar" implica revancha sobre el puzzle
   que se falló. Sólo cambia la acción "Reiniciar" en partida en curso.

3. **La campaña no avanza.** Reiniciar no cuenta como completar; no se registra
   `CompletedPuzzle` ni se mueve el puntero. El puzzle viejo (in‑progress, sin
   terminar) simplemente se descarta.

4. **Reroll libre.** El jugador puede reiniciar tantas veces como quiera; cada vez
   obtiene un puzzle distinto del mismo nivel. Aceptable para el MVP y es
   justamente lo que se pidió.

## Diseño

### Comportamiento

- Tocar "Reiniciar" → diálogo de confirmación (ya existe) → al confirmar:
  - Se genera/toma del buffer un puzzle nuevo para el número de campaña actual.
  - Nuevo `GameSnapshot` in‑progress: tablero desde givens, timer 0, errores 0,
    undo/redo vacío. Se persiste como partida actual.
  - Mientras se genera se muestra el estado `loading` (spinner centrado). Con el
    buffer lleno es instantáneo; con el buffer vacío tarda 1–3 s.
- El diálogo de derrota y su botón "Reintentar" quedan igual.

### Implementación

**`GameViewModel`**

- Nueva función `onRestart()`, gemela de `onRetry()` salvo la fuente del puzzle:

  ```kotlin
  /**
   * "Reiniciar" en partida en curso: descarta el intento y arranca un puzzle
   * NUEVO del mismo nivel de campaña (mismo target de dificultad). No avanza la
   * campaña ni registra nada. El diálogo de confirmación lo pide en la UI.
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

  `getNextCampaignPuzzle()` lee el puntero de campaña (que no avanzó) y devuelve
  un puzzle nuevo para ese número vía `PuzzleRepository.puzzleForCampaign(...)`,
  que además mantiene el buffer lleno. Mismo patrón que `onAdvance()`, sin
  `completePuzzle`.

- `onRetry()` se mantiene tal cual, ahora usada **sólo** por el diálogo de
  derrota. Su KDoc se actualiza para reflejarlo.

**`GameScreen`**

- `GameCallbacks` gana el campo `onRestart: () -> Unit`.
- `GameScreen` lo cablea a `viewModel::onRestart`.
- El `AlertDialog` de confirmación de reinicio llama a `callbacks.onRestart()` en
  vez de `callbacks.onRetry()`.
- Texto del diálogo: se quita "es el mismo puzzle #N". Nuevo cuerpo:
  "Se genera un puzzle nuevo del mismo nivel. El tiempo y los errores vuelven a
  cero y pierdes lo resuelto hasta ahora." Título sin cambios ("Reiniciar
  puzzle").

### Tests

**Unit — `GameViewModelTest`**

- `onRetry` (caso derrota): el test existente `onRetry restarts the same puzzle
  from scratch mid-game` se mantiene — verifica `startGame(puzzle, …)` con el
  MISMO puzzle.
- Nuevo `onRestart generates a new campaign puzzle at the same level`:
  - `getNextCampaignPuzzle()` devuelve un puzzle distinto (otro id, mismo target).
  - Tras `onRestart()`: `startGame` llamado con el puzzle NUEVO, errores 0,
    status `IN_PROGRESS`, `canUndo` false.
  - `completePuzzle` **no** invocado (la campaña no avanza).

**Instrumented — `GameContentTest`**

- `restart_action_asks_for_confirmation_before_calling_back`: pasa a verificar que
  se dispara `onRestart` (no `onRetry`); Cancelar sigue siendo no‑op.
- `restart_action_is_hidden_once_the_puzzle_is_over`: sin cambios.
- `failed_status_shows_the_dialog`: sin cambios ("Reintentar" sigue ahí).

### Archivos

| Acción | Archivo |
|--------|---------|
| Modificar | `app/src/main/java/com/freesudoku/app/ui/game/GameViewModel.kt` |
| Modificar | `app/src/main/java/com/freesudoku/app/ui/game/GameScreen.kt` |
| Modificar | `app/src/test/java/com/freesudoku/app/ui/game/GameViewModelTest.kt` |
| Modificar | `app/src/androidTest/java/com/freesudoku/app/ui/GameContentTest.kt` |
| Modificar | `docs/PROGRESS.md` |

## Fuera de alcance

- Cambiar "Reintentar" del diálogo de derrota.
- Selector de dificultad libre / modo práctica.
- Límite al número de rerolls.
- Animaciones de transición al reiniciar.
