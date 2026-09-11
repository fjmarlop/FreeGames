# Modo "Partida rápida" — Design

**Fecha:** 2026-09-11
**Plataforma:** Android
**Alcance:** nuevo modo de juego que no sigue la campaña: elegir dificultad, jugar un puzzle suelto.

---

## Contexto

Hoy el único modo es la campaña: `HomeViewModel.onPlayOrContinue()` siempre resuelve
"continuar la partida en curso, o generar el siguiente puzzle de campaña". No hay forma
de jugar un puzzle aislado a una dificultad elegida sin afectar el progreso de campaña.

## Pedido del usuario

Un modo "Partida rápida": elegir la dificultad, jugar **un** puzzle, sin necesidad de
seguir la carrera de campaña.

## Decisiones

1. **Niveles ofrecidos: Fácil, Medio, Difícil, Experto.** Se excluye Maestro (`score ≥ 80`)
   porque el generador no puede producirlo de forma fiable — el solver llega hasta X-Wing
   y `CampaignCurve.CURVE_MAX = 56` ya refleja ese techo (limitación documentada,
   spec §10.2 del diseño original). Ofrecerlo sería engañoso. Decisión del usuario.

2. **Una sola partida activa a la vez.** Se reutiliza el slot `current_game` (Room, fila
   única) tanto para campaña como para partida rápida — sin tabla ni migración nueva.
   Si hay una campaña en curso y el jugador arranca una rápida (o viceversa), se pide
   confirmación de que se pierde el progreso del intento actual (mismo patrón que ya
   tiene "Reiniciar").

3. **El dominio ya soporta puzzles sin número de campaña**: `Puzzle.number: Int?` — el
   buffer de puzzles pre-generados ya crea puzzles con `number = null`
   (`PuzzleFactory.generateForTarget()`). Una partida rápida es exactamente eso: un
   `Puzzle` con `number = null`, servido directo (sin pasar por el buffer, que está
   pensado para consumo continuo de campaña, no para una acción puntual).

4. **Cuenta en Estadísticas, no en la campaña.** Completar un puzzle rápido inserta una
   fila en `completed_puzzle` (total completados, mejor tiempo, desglose por banda) pero
   **no** llama a `campaignRepository.advanceAfterCompleting`. `GameRepository` ya usa el
   centinela `-1` para "sin número de campaña" en `current_game`
   (`puzzleNumber = snapshot.puzzle.number ?: -1`); se reutiliza el mismo centinela en
   `completed_puzzle` en vez de agregar una columna nullable (evita una migración de Room
   para un campo que ninguna query filtra hoy).

5. **Selector de dificultad: bottom sheet desde Home**, no una pantalla nueva — mismo
   patrón que `ResultSheet` (`ModalBottomSheet`). Un botón nuevo, secundario, debajo de la
   tarjeta "Continuar/Jugar".

## Diseño

### Puntuación objetivo por banda (generación)

Un score representativo dentro de cada banda, lejos de los bordes (los mismos que usan
`RaterCalibrationTest`/`CampaignCurve` como referencia):

| Banda | Rango | Score objetivo |
|---|---|---|
| FACIL | [0, 23) | 14.0 |
| MEDIO | [23, 40) | 31.0 |
| DIFICIL | [40, 49) | 44.0 |
| EXPERTO | [49, ∞) — techo práctico ~56 | 52.0 |

`PuzzleFactory.generateForTarget(target)` ya hace "generar, tallar, puntuar, quedarse con
el más cercano al objetivo dentro de la tolerancia" — igual que para campaña. El score
real puede caer en la banda vecina (igual que ya pasa en campaña); no es un contrato duro.

### Cambios de dominio/datos

- `DifficultyBand.kt`: constante `QUICK_PLAY_SELECTABLE = listOf(FACIL, MEDIO, DIFICIL, EXPERTO)`
  con comentario explicando la exclusión de MAESTRO.
- `PuzzleRepository.kt`: `suspend fun puzzleForBand(band: DifficultyBand): Puzzle` — mapa
  privado banda→score objetivo, delega en `factory.generateForTarget(target)`. Sin buffer.
- `GameUseCases.kt`: nuevo `GetPuzzleForBand(puzzleRepository)` — wrapper fino, mismo
  patrón que `GetNextCampaignPuzzle`.
- `CompletePuzzle`: cuando `snapshot.puzzle.number == null`, inserta con
  `puzzleNumber = QUICK_PLAY_SENTINEL (-1)` y **no** llama a
  `campaignRepository.advanceAfterCompleting`.

### `GameViewModel` / pantalla de juego

- `GameUiState.puzzleNumber` pasa de `Int` a `Int?` (null = partida rápida). `render()`
  ya no coacciona con `?: 0`.
- `GameTopStatus`: si `puzzleNumber == null` el chip dice "Partida rápida"; si no,
  "Puzzle #$puzzleNumber" (como hoy).
- `onRestart()` y `onAdvance()` (usados por el botón "Reiniciar" y por "Siguiente" del
  `ResultSheet`) pasan a elegir la fuente del próximo puzzle según el modo:
  - `snapshot.puzzle.number != null` → `getNextCampaignPuzzle()` (como hoy).
  - `snapshot.puzzle.number == null` → `getPuzzleForBand(snapshot.puzzle.band)` (mismo
    nivel, puzzle nuevo).
- `onRetry()` (diálogo de derrota) no cambia: siempre repite `snapshot.puzzle` tal cual,
  funciona igual en los dos modos.
- `ResultSheet`: el texto del botón "Siguiente puzzle" pasa a depender del modo:
  "Siguiente puzzle" en campaña, "Otro puzzle rápido" en partida rápida.

### Home

- `HomeUiState.currentNumber: Int` pasa a `Int?` (null = hay una partida rápida en curso
  guardada). `ContinuePuzzleCard` evita mostrar "#N" engañoso: si `currentNumber == null`
  muestra el chip "Partida rápida" en vez de "PUZZLE / #N", y el botón dice "Continuar"
  igual que hoy.
- Nuevo botón secundario "Partida rápida" (fuera de la tarjeta de campaña) que abre el
  bottom sheet de selección de banda. Deshabilitado mientras `preparingPuzzle` (mismo
  patrón que el botón de campaña).
- Selección de banda → si hay una partida en curso (`hasResumableGame`) se pide
  confirmación (perder el intento actual) antes de generar y arrancar; si no, arranca
  directo. Al terminar, navega a `Routes.GAME` (mismo mecanismo que
  `HomeViewModel.onPlayOrContinue`: pre-generar y guardar el snapshot **antes** de
  navegar, `GameViewModel.init` lo resume sin cambios).

### Fuera de alcance

- Elegir dificultad *dentro* de la campaña (la campaña sigue siendo su propia curva).
- Historial separado de "partidas rápidas" (se mezclan con el resto en Estadísticas).
- Un límite al número de partidas rápidas seguidas.
- Tocar el generador/solver para intentar producir MAESTRO.
