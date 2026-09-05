# Quitar la banda PRINCIPIANTE — Implementation Plan

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` (recommended) or `mobiai-mobile-executing-plans` to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Colapsar la taxonomía de dificultad de 6 bandas a 5 quitando `PRINCIPIANTE`; `FACIL` pasa a ser la banda de entrada con `lowerBound = 0.0`.

**Architecture:** `PRINCIPIANTE [0,9)` se fusiona en `FACIL`, que pasa a cubrir `[0,23)`. Los demás cortes (MEDIO 23, DIFÍCIL 40, EXPERTO 49, MAESTRO 80) no se mueven. No se recalibran los pesos del rater. `StatsScreen` ya itera `DifficultyBand.entries`, así que pasa a mostrar 5 filas sin tocarlo. Deserialización defensiva para strings `"PRINCIPIANTE"` guardados de antes.

**Tech Stack:** Kotlin, Jetpack Compose, MVVM, Room (columna `band` como String — sin cambio de schema), JUnit4 + Truth + MockK, Compose UI Test.

**Platform:** Android

**Spec:** `docs/designs/2026-09-05-drop-principiante-band-design.md`

---

### Task 1: Colapsar el enum `DifficultyBand` y arreglar todas las referencias de compilación

**Files:**
- Modify: `app/src/main/java/com/freesudoku/app/domain/model/DifficultyBand.kt`
- Modify: `app/src/main/java/com/freesudoku/app/ui/common/BandLabels.kt`
- Modify: `app/src/main/java/com/freesudoku/app/domain/campaign/CampaignCurve.kt` (solo comentario)
- Test: `app/src/test/java/com/freesudoku/app/domain/model/DifficultyBandTest.kt`
- Test: `app/src/test/java/com/freesudoku/app/ui/home/HomeViewModelTest.kt`
- Test: `app/src/androidTest/java/com/freesudoku/app/ui/HomeContentTest.kt`
- Test: `app/src/test/java/com/freesudoku/app/domain/solver/RaterCalibrationTest.kt` (referencias al símbolo + `targetBand` + lista de cobertura; la búsqueda con `DoubleArray(5)` es Task 2)

> Nota: quitar el constante del enum rompe la compilación en todos los sitios que lo nombran. Este task es atómico: no compila hasta que estén todos.

- [ ] **Step 1: Actualizar `DifficultyBandTest` para expresar el nuevo contrato**

```kotlin
class DifficultyBandTest {

    @Test fun `fromScore maps each band to its calibrated range`() {
        assertThat(DifficultyBand.fromScore(0.0)).isEqualTo(DifficultyBand.FACIL)
        assertThat(DifficultyBand.fromScore(-5.0)).isEqualTo(DifficultyBand.FACIL) // piso, no lanza
        for (band in DifficultyBand.entries) {
            assertThat(DifficultyBand.fromScore(band.lowerBound)).isEqualTo(band)
            assertThat(DifficultyBand.fromScore(band.lowerBound + 0.01)).isEqualTo(band)
            if (band.ordinal > 0) {
                assertThat(DifficultyBand.fromScore(band.lowerBound - 0.01))
                    .isEqualTo(DifficultyBand.entries[band.ordinal - 1])
            }
        }
        assertThat(DifficultyBand.fromScore(1000.0)).isEqualTo(DifficultyBand.MAESTRO)
    }

    @Test fun `bands are ordered by lowerBound`() {
        val bounds = DifficultyBand.entries.map { it.lowerBound }
        assertThat(bounds).isInOrder()
    }

    @Test fun `there are five bands and none is called PRINCIPIANTE`() {
        assertThat(DifficultyBand.entries).hasSize(5)
        assertThat(DifficultyBand.entries.map { it.name }).containsNoneOf("PRINCIPIANTE", "")
    }
}
```

- [ ] **Step 2: Run — falla a compilar**

Run: `./gradlew :app:compileDebugUnitTestKotlin`
Expected: FAIL — `DifficultyBand.PRINCIPIANTE` sigue existiendo pero el test nuevo espera 5 entries / `fromScore(0.0) == FACIL`. (Además otros archivos aún nombran el símbolo.)

- [ ] **Step 3: Colapsar el enum**

`DifficultyBand.kt`:

```kotlin
enum class DifficultyBand(val lowerBound: Double) {
    // Cut points calibrated against the sudoku-exchange puzzle bank (see RaterCalibrationTest).
    // FACIL is the floor band: it absorbs everything below the MEDIO cut, so fromScore never
    // runs out of entries for a low/negative score.
    FACIL(0.0),
    MEDIO(23.0),
    DIFICIL(40.0),
    EXPERTO(49.0),
    MAESTRO(80.0);

    companion object {
        fun fromScore(score: Double): DifficultyBand = entries.last { score >= it.lowerBound }
    }
}
```

- [ ] **Step 4: Arreglar las referencias que rompen compilación**

`BandLabels.kt` — quitar la primera rama:

```kotlin
fun DifficultyBand.label(): String = when (this) {
    DifficultyBand.FACIL -> "Fácil"
    DifficultyBand.MEDIO -> "Medio"
    DifficultyBand.DIFICIL -> "Difícil"
    DifficultyBand.EXPERTO -> "Experto"
    DifficultyBand.MAESTRO -> "Maestro"
}
```

`CampaignCurve.kt` — el comentario del companion (línea ~44), reemplazar `PRINCIPIANTE` por `FACIL`:

```kotlin
        // Calibrated to the score range the generator actually produces (see RaterCalibrationTest
        // and the generator's bimodal carve distribution): a gentle ramp through FACIL -> MEDIO
        // over the first ~40 puzzles, then up into DIFICIL/EXPERTO, saturating near 56.
```

`HomeViewModelTest.kt:39` — `DifficultyBand.PRINCIPIANTE` → `DifficultyBand.FACIL`.

`HomeContentTest.kt:27` — `currentBand = DifficultyBand.PRINCIPIANTE` → `currentBand = DifficultyBand.FACIL`.

`RaterCalibrationTest.kt`:
- `targetBand()` (líneas ~65-72) — quitar la rama `rating < 1.4`:

```kotlin
    private fun targetBand(rating: Double): DifficultyBand = when {
        rating < 2.4 -> DifficultyBand.FACIL
        rating < 3.3 -> DifficultyBand.MEDIO
        rating < 4.8 -> DifficultyBand.DIFICIL
        rating < 7.0 -> DifficultyBand.EXPERTO
        else -> DifficultyBand.MAESTRO
    }
```

- `band cut points give every band some coverage across the sample` (líneas ~54-59) — quitar `DifficultyBand.PRINCIPIANTE,` de la lista:

```kotlin
        for (b in listOf(
            DifficultyBand.FACIL, DifficultyBand.MEDIO,
            DifficultyBand.DIFICIL, DifficultyBand.EXPERTO,
        )) {
```

- [ ] **Step 5: Run — suite unitaria completa**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS. Vigilar en particular:
- `DifficultyBandTest` (nuevo contrato)
- `PuzzleFactoryTest > the first campaign puzzle is dense and lands in a beginner band` (línea 39: `puzzle.band.ordinal isAtMost FACIL.ordinal` — ahora `FACIL.ordinal == 0`, o sea exige exactamente FÁCIL). Si falla de forma flaky por un puzzle que puntúa MEDIO, aflojar a `isAtMost(DifficultyBand.MEDIO.ordinal)` y renombrar el test a `... lands in the easiest band`. Documentar el desvío.
- `RaterCalibrationTest` guards (`rho >= 0.80`, cobertura) — no deberían moverse: colapsar el corte inferior no cambia los pesos.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/model/DifficultyBand.kt \
        app/src/main/java/com/freesudoku/app/ui/common/BandLabels.kt \
        app/src/main/java/com/freesudoku/app/domain/campaign/CampaignCurve.kt \
        app/src/test/java/com/freesudoku/app/domain/model/DifficultyBandTest.kt \
        app/src/test/java/com/freesudoku/app/ui/home/HomeViewModelTest.kt \
        app/src/androidTest/java/com/freesudoku/app/ui/HomeContentTest.kt \
        app/src/test/java/com/freesudoku/app/domain/solver/RaterCalibrationTest.kt
git commit -m "refactor(domain): drop the PRINCIPIANTE band, FACIL becomes the floor"
```
(Terminar el cuerpo con `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`.)

---

### Task 2: Ajustar la búsqueda de calibración a 5 bandas

**Files:**
- Modify: `app/src/test/java/com/freesudoku/app/domain/solver/RaterCalibrationTest.kt`

Contexto: tras Task 1 el test compila y los guards pasan, pero `search prints the best weights and band cut points it can find` sigue optimizando **5** cortes (`DoubleArray(5)`) cuando ahora hay 5 bandas → 4 cortes. El 5º corte queda muerto (`bandOf` lo colapsa con `coerceAtMost`). Limpiarlo para que la búsqueda siga siendo sana si se re-corre.

- [ ] **Step 1: Reducir a 4 cortes**

En `search prints the best weights and band cut points it can find`:

```kotlin
            // ascending band cut points: MEDIO, DIFICIL, EXPERTO, MAESTRO (FACIL is the 0.0 floor)
            val raw = DoubleArray(4) { rng.nextDouble(2.0, 95.0) }.also { it.sort() }
```

Y el `println` del bloque final:

```kotlin
            band lowerBounds: FACIL=0.0 MEDIO=${f(cuts[0])} DIFICIL=${f(cuts[1])} EXPERTO=${f(cuts[2])} MAESTRO=${f(cuts[3])}
```

`bandOf(score, cuts)` no cambia (ya es genérico sobre `cuts.size`).

- [ ] **Step 2: Run**

Run: `./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.domain.solver.RaterCalibrationTest"`
Expected: PASS — los 4 guards verdes y `search` corre sin error (`assertThat(bestScore).isGreaterThan(0.0)`).

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/freesudoku/app/domain/solver/RaterCalibrationTest.kt
git commit -m "test(domain): calibration search uses 4 band cuts after PRINCIPIANTE removal"
```

---

### Task 3: Deserialización defensiva para `band = "PRINCIPIANTE"` guardado

**Files:**
- Modify: `app/src/main/java/com/freesudoku/app/data/mapper/PuzzleMappers.kt`
- Modify: `app/src/main/java/com/freesudoku/app/data/serialization/GameSnapshotDto.kt`
- Test: `app/src/test/java/com/freesudoku/app/data/serialization/GameSnapshotDtoTest.kt`
- Test: crear `app/src/test/java/com/freesudoku/app/data/mapper/PuzzleMappersTest.kt` si no existe (si existe, agregar el caso)

Contexto: `DifficultyBand.valueOf("PRINCIPIANTE")` lanza `IllegalArgumentException`. Una fila vieja del `puzzle_buffer` o una partida guardada con ese string crashea al leerse. `StatsRepository` ya es null-safe; estos dos puntos no.

- [ ] **Step 1: Test — `GameSnapshotDto` tolera un band desconocido**

En `GameSnapshotDtoTest.kt`, agregar:

```kotlin
    @Test fun `a snapshot persisted with a since-removed band deserializes as FACIL`() {
        val json = GameSnapshotDto.toJson(sampleSnapshot()) // helper existente
            .replace("\"FACIL\"", "\"PRINCIPIANTE\"")
        val restored = GameSnapshotDto.fromJson(json)
        assertThat(restored.puzzle.band).isEqualTo(DifficultyBand.FACIL)
    }
```

(Ajustar a los nombres reales de los helpers del test / API del DTO — verificar antes de escribir.)

- [ ] **Step 2: Run — falla**

Run: `./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.data.serialization.GameSnapshotDtoTest"`
Expected: FAIL — `IllegalArgumentException: No enum constant ... PRINCIPIANTE`.

- [ ] **Step 3: Implementar el fallback**

Añadir un helper en `DifficultyBand`:

```kotlin
        /** Tolerant parse for persisted strings — an unknown/removed name falls back to the floor band. */
        fun parseOrFloor(name: String): DifficultyBand =
            entries.firstOrNull { it.name == name } ?: FACIL
```

`GameSnapshotDto.kt:110` — `band = DifficultyBand.valueOf(band)` → `band = DifficultyBand.parseOrFloor(band)`.

`PuzzleMappers.kt:14` — `band = DifficultyBand.valueOf(band)` → `band = DifficultyBand.parseOrFloor(band)`.

- [ ] **Step 4: Run — pasa**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (suite completa).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/freesudoku/app/domain/model/DifficultyBand.kt \
        app/src/main/java/com/freesudoku/app/data/serialization/GameSnapshotDto.kt \
        app/src/main/java/com/freesudoku/app/data/mapper/PuzzleMappers.kt \
        app/src/test/java/com/freesudoku/app/data/serialization/GameSnapshotDtoTest.kt
git commit -m "fix(data): tolerate a persisted PRINCIPIANTE band string, fall back to FACIL"
```

---

### Task 4: Verificación end-to-end, docs y cierre

**Files:**
- Modify: `docs/PROGRESS.md`
- Modify: `docs/designs/2026-09-04-sudoku-mvp-design.md` (tablas de banda)

- [ ] **Step 1: Suite completa + lint**

Run: `./gradlew :app:testDebugUnitTest :app:lintDebug`
Expected: unit verdes; lint sin hallazgos nuevos (el `EmptySuperCall` preexistente en `GameViewModel.onCleared` no cuenta).

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: 11 instrumentados verdes. (Emulador `Medium_Phone_API_35`; si hay flakiness recién booteado, desinstalar APKs stale y reintentar.)

- [ ] **Step 2: Verificación manual en el emulador**

1. `./gradlew :app:installDebug`
2. Abrir la app → pestaña **Estadísticas** → "Por dificultad" muestra **5 filas**: Fácil, Medio, Difícil, Experto, Maestro. No "Principiante".
3. Jugar y completar el puzzle #1 (spam de pista) → vuelve a Home → Estadísticas: **Fácil = 1 completado**.
4. Pantalla de juego: el footer "Dificultad: Fácil" y el chip de banda muestran "Fácil".

- [ ] **Step 3: Docs**

`docs/designs/2026-09-04-sudoku-mvp-design.md`:
- Línea ~97: `enum: FACIL, MEDIO, DIFICIL, EXPERTO, MAESTRO ...` (quitar PRINCIPIANTE).
- Línea ~372: `Rangos de banda (score) | DifficultyBand | FACIL 0, MEDIO 23, DIFICIL 40, EXPERTO 49, MAESTRO 80`.
- Línea ~383: reformular la frase de la distribución bimodal sin PRINCIPIANTE.

`docs/PROGRESS.md` — nueva sección al final (antes de `## Estado`):

```markdown
## Banda PRINCIPIANTE eliminada (2026-09-05)

Investigando por qué el puzzle #1 se registraba como "Fácil" y no "Principiante":
la curva de campaña (`CampaignCurve`, `CURVE_BASE = 9.0` = umbral de FÁCIL) nunca
apunta a PRINCIPIANTE `[0,9)` — la banda existía pero era una fila permanente en
cero en Estadísticas. Decisión del usuario: **eliminarla**. FÁCIL pasa a ser la
banda de entrada con `lowerBound = 0.0` (cubre `[0,23)`); el resto de los cortes
no se mueven y no se recalibran los pesos del rater. Deserialización tolerante
(`DifficultyBand.parseOrFloor`) para strings `"PRINCIPIANTE"` guardados de antes.

Spec/plan: `docs/designs/2026-09-05-drop-principiante-band-design.md`,
`docs/plans/2026-09-05-drop-principiante-band.md`.
```

Y actualizar la línea de `## Estado`.

- [ ] **Step 4: Commit**

```bash
git add docs/
git commit -m "docs: record dropping the PRINCIPIANTE band"
```

- [ ] **Step 5: Actualizar la memoria**

En `C:\Users\fjmar\.claude\projects\D--proyectos-Sudoku\memory\freesudoku-project.md`:
- La línea de bandas: `band bounds 0/9/23/40/49/80` → `band bounds (5 bandas, sin PRINCIPIANTE) FACIL 0 / MEDIO 23 / DIFICIL 40 / EXPERTO 49 / MAESTRO 80`.

- [ ] **Step 6: Finalizar**

Usar `mobiai-mobile-finishing-branch` (directo sobre `master`, repo local sin remoto).

---

## Self-review

- **Cobertura del spec:** enum colapsado (Task 1), `BandLabels`/comentario (Task 1), `StatsScreen` sin tocar por diseño (verificado: itera `entries`), calibración (Task 1 + 2), datos viejos (Task 3), docs+memoria (Task 4).
- **Sin placeholders:** todo el código está escrito salvo el helper del test de DTO en Task 3 Step 1 que dice explícitamente "verificar los nombres reales antes de escribir" (la API del DTO no está en contexto).
- **Orden / colisiones:** Task 1 es atómico y bloquea todo (no compila hasta terminarlo). Task 2 y 3 son independientes entre sí pero ambos posteriores a Task 1. Task 3 vuelve a tocar `DifficultyBand.kt` (agrega `parseOrFloor`) — no en paralelo con nada. Ejecutar 1 → 2 → 3 → 4 en orden.
- **Tipos:** `FACIL.ordinal` pasa de 1 a 0; `MEDIO` de 2 a 1, etc. Revisadas las aserciones `.ordinal isAtMost/isAtLeast` en `PuzzleFactoryTest`, `DifficultyRaterTest`, `RaterCalibrationTest` — siguen teniendo sentido (easy → FÁCIL, diabolical → DIFÍCIL+).
