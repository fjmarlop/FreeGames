# Quitar la banda PRINCIPIANTE — Design

**Fecha:** 2026-09-05
**Plataforma:** Android
**Alcance:** colapsar el taxonomía de dificultad de 6 bandas a 5. Sin cambios de gameplay ni de generación.

---

## Contexto (investigación previa)

`CampaignCurve.targetScore(n) = 9.0 + 7.0·ln(1+n) + ruido(±3)`. Con `CURVE_BASE = 9.0`
= `DifficultyBand.FACIL.lowerBound`, la curva **arranca en el umbral de FÁCIL** y solo
sube:

| Puzzle | target | banda |
|--------|--------|-------|
| #1 | ~14 | Fácil |
| #5 | ~24 | Medio |
| #40 | ~37 | Medio |
| #100 | ~44 | Difícil |

Ningún número de campaña apunta jamás a PRINCIPIANTE `[0, 9)`. La banda existe en el
enum y ocupa una fila permanentemente en cero en Estadísticas. El comentario de
`CampaignCurve` todavía dice *"a gentle ramp through PRINCIPIANTE → MEDIO"* — falso
desde el commit `174c43e` (calibración) que subió `CURVE_BASE` de 4.0 → 9.0.

## Decisión del usuario

**Opción C:** eliminar PRINCIPIANTE. FÁCIL pasa a ser la banda de entrada.

## Diseño

FÁCIL absorbe el rango que ocupaba PRINCIPIANTE: `FACIL.lowerBound` pasa de `9.0` a
`0.0`, así FÁCIL cubre `[0, 23)`. El resto de los cortes no se mueven
(MEDIO 23, DIFÍCIL 40, EXPERTO 49, MAESTRO 80). **No se recalibran los pesos del
rater** — solo se colapsa el corte inferior. Todo lo que antes puntuaba/caía como
PRINCIPIANTE ahora cae como FÁCIL (que es lo que el usuario ya ve: puzzle #1 → Fácil).

Efecto secundario positivo: `DifficultyBand.fromScore()` hoy puede lanzar
`NoSuchElementException` si algún día le llega un score < 0; con `FACIL(0.0)` como
piso eso desaparece.

### Cambios

| Archivo | Cambio |
|---------|--------|
| `domain/model/DifficultyBand.kt` | quitar `PRINCIPIANTE(0.0)`; `FACIL(9.0)` → `FACIL(0.0)` |
| `ui/common/BandLabels.kt` | quitar la rama `PRINCIPIANTE -> "Principiante"` |
| `domain/campaign/CampaignCurve.kt` | corregir el comentario (PRINCIPIANTE → FÁCIL) |
| `domain/model/DifficultyBandTest.kt` | `fromScore(0.0)` ahora es `FACIL`; quitar el caso especial `band != PRINCIPIANTE` |
| `domain/solver/RaterCalibrationTest.kt` | `targetBand()` sin la rama `< 1.4`; lista de cobertura sin PRINCIPIANTE; búsqueda: `DoubleArray(5)` → `DoubleArray(4)` cortes (4 cortes para 5 bandas) + `println` |
| `ui/home/HomeViewModelTest.kt`, `ui/HomeContentTest.kt` | fixtures `PRINCIPIANTE` → `FACIL` |
| `data/mapper/PuzzleMappers.kt`, `data/serialization/GameSnapshotDto.kt` | `DifficultyBand.valueOf(band)` → fallback a `FACIL` si el string guardado es `"PRINCIPIANTE"` (filas viejas del buffer / partida guardada) |
| `ui/stats/StatsScreen.kt` | ninguno — ya itera `DifficultyBand.entries`, pasa a 5 filas solo |
| `data/repository/StatsRepository.kt` | ninguno — ya usa `runCatching { valueOf }` |
| docs + memoria | tablas de banda, `docs/PROGRESS.md`, `freesudoku-project.md` |

### Datos existentes (sin migración de Room)

`band` es una columna `String`, no hay cambio de schema. Filas `completed_puzzle`
con `band = "PRINCIPIANTE"` (de una instalación previa):
- `StatsRepository` ya las descarta con `runCatching` → dejan de aparecer en el
  desglose por banda, pero **siguen contando** en `totalCompleted` (COUNT(*) aparte).
- Aceptable para el MVP (el usuario está en instalación limpia). No se hace
  normalización one-shot.

## Fuera de alcance

- Recalibrar los pesos del rater.
- La saturación superior de la curva (EXPERTO casi inalcanzable, MAESTRO imposible con
  `CURVE_MAX = 56` < `80`) — es el límite del solver ya documentado (spec §10.2).
- Cambiar `CampaignCurve` para que la campaña arranque más suave (era la opción A,
  descartada).
