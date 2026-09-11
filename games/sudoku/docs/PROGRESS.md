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

## Ícono real (2026-09-04)

Reemplazado el placeholder de color plano por un diseño real: grilla blanca 3x3
con una celda destacada en dorado sobre un degradé azul diagonal. Ícono
adaptativo (fondo + foreground + capa monocroma para Android 13+) más los PNG
legacy (pre-API 26) generados con Pillow desde el mismo diseño
(`scripts/render_launcher_icon.py`). Verificado en el emulador: se ve limpio y
reconocible en el cajón de apps. Lint quedó sin advertencias de ícono.

## Verificación del usuario

El usuario probó el **modo con límite de 3 errores** en su propio dispositivo y
confirmó que funciona correctamente (celda marcada, contador, fin de partida al
tercer error).

## Rediseño visual "Apex Precision" (2026-09-04)

El usuario compartió mockups (Stitch) con un sistema de diseño oscuro/industrial
("Apex Precision": Space Grotesk / Geist / JetBrains Mono, paleta acero-azul +
dorado, sin glow, bordes achaflanados). Los mockups mezclaban look nuevo +
funciones nuevas (XP, rangos, temporadas, logros, desafío diario, contrarreloj,
selección de dificultad, puntos, sonido/háptica, tutorial, reset de stats, PRO).
**Decisión del usuario: solo el reskin visual**, sin ninguna de esas funciones
nuevas.

Aplicado a las 4 pantallas existentes sin tocar la lógica de negocio:
- Tema nuevo (fuentes variables Space Grotesk/Geist/JetBrains Mono bundleadas,
  paletas oscura+clara, formas "Soft Technical").
- Nav inferior (Inicio/Estadísticas/Ajustes) reemplaza los botones de texto.
- Juego: contador de errores reskineado como 3 escudos ("vidas"), footer de
  dificultad + % completado (derivados de datos ya existentes).
- Estadísticas: desglose por dificultad con las 6 bandas (dato ya existente).
- Ajustes: toggles como filas con ícono, selector de tema como tarjetas.

113 tests unitarios + 9 instrumentados en verde, lint limpio. Verificado en el
emulador en claro y oscuro, las 4 pantallas.

## Botón "Reiniciar" en partida (2026-09-05)

Se agregó un botón "Reiniciar" en la barra superior de la pantalla de juego
(visible sólo con la partida en curso). Con diálogo de confirmación.

Primera versión: reiniciaba el **mismo** puzzle. Segunda petición del usuario:
que genere un **puzzle nuevo del mismo nivel**. `GameViewModel.onRestart()` toma
el puzzle de `getNextCampaignPuzzle()` (el puntero de campaña no avanzó, así que
devuelve una instancia nueva para el número actual) en vez de `snapshot.puzzle`.
La campaña no avanza y no se registra nada. El diálogo de derrota "Reintentar"
queda igual (mismo puzzle) — decisión del usuario.

Spec/plan: `docs/designs/2026-09-05-restart-generates-new-puzzle-design.md`,
`docs/plans/2026-09-05-restart-generates-new-puzzle.md`.

115 tests unitarios + 11 instrumentados en verde, lint sin hallazgos nuevos.
Verificado end-to-end en el emulador (API 35): el reinicio genera givens
distintos, mantiene el número de puzzle, resetea timer y errores.

## Migración al monorepo FreeGames (2026-09-06)

El proyecto se movió a `D:\proyectos\FreeGames\games\sudoku` (monorepo de juegos,
`git mv` con historia preservada). `applicationId` cambió a
`es.fjmarlop.freegames.sudoku` (el `namespace`/paquete de código sigue siendo
`com.freesudoku.app`). SDK, firma, build type release y el guard de "sin
`INTERNET`" ahora vienen del convention plugin `freegames.android.game`
compartido con el resto de los juegos. Detalles en el README del monorepo.

## Banda PRINCIPIANTE eliminada (2026-09-11)

Investigando por qué el puzzle #1 se registraba como "Fácil" y no "Principiante":
la curva de campaña (`CampaignCurve`, `CURVE_BASE = 9.0` = umbral de FÁCIL) nunca
apunta a PRINCIPIANTE `[0,9)` — la banda existía pero era una fila permanente en
cero en Estadísticas. Decisión del usuario: **eliminarla**. FÁCIL pasa a ser la
banda de entrada con `lowerBound = 0.0` (cubre `[0,23)`); el resto de los cortes
no se mueven y no se recalibran los pesos del rater. `DifficultyBand.fromScore`
además ya no puede lanzar `NoSuchElementException` (cae a la banda más baja en
vez de exigir un match). Deserialización tolerante (`DifficultyBand.parseOrFloor`)
para strings `"PRINCIPIANTE"` guardados de antes (buffer de puzzles / partida
guardada).

Spec/plan: `docs/designs/2026-09-05-drop-principiante-band-design.md`,
`docs/plans/2026-09-05-drop-principiante-band.md`.

## Modo "Partida rápida" (2026-09-11)

Nuevo modo de juego: desde Home, botón "Partida rápida" abre un selector de
dificultad (Fácil/Medio/Difícil/Experto — Maestro excluido, el generador no
puede producirlo de forma fiable) y arranca **un** puzzle suelto, sin afectar
la campaña.

El dominio ya soportaba esto: `Puzzle.number: Int?` — una partida rápida es un
puzzle con `number = null`, igual que ya hacían los puzzles del buffer.
`PuzzleRepository.puzzleForBand(band)` genera directo (sin buffer) apuntando a
un score representativo por banda. Se reutiliza el slot único `current_game`
(sin tabla/migración nueva); si hay una campaña en curso, empezar una partida
rápida pide confirmación. Completar una partida rápida cuenta en Estadísticas
(total, mejor tiempo, desglose por banda) pero no avanza la campaña — mismo
centinela `-1` que `GameRepository` ya usaba para "sin número de campaña".

En pantalla de juego: chip "Partida rápida" en vez de "Puzzle #N"; "Reiniciar"
y "Siguiente" ("Otro puzzle rápido") generan otro puzzle de la misma banda en
vez de tirar de la campaña.

Verificado end-to-end en el emulador: 9 puzzles Fácil completados vía "Otro
puzzle rápido" sin que la campaña se moviera de PUZZLE #1; desglose por banda
correcto en Estadísticas; "Reiniciar" en modo rápido genera un puzzle distinto
manteniendo la banda. 128 tests unitarios + 16 instrumentados en verde, lint y
guard de permisos limpios.

Spec/plan: `docs/designs/2026-09-11-quick-play-mode-design.md`,
`docs/plans/2026-09-11-quick-play-mode.md`.

## Estado

Todo mergeado a `master`. MVP funcional + rater calibrado + bug de finalización
corregido + límite de errores confirmado por el usuario + reskin visual
aplicado + botón "Reiniciar" (genera puzzle nuevo) + migrado al monorepo
FreeGames + banda PRINCIPIANTE eliminada + modo "Partida rápida". Ver
`docs/plans/*` y `docs/designs/*` para detalles y desvíos.
