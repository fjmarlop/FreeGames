# FreeSudoku — Plan 2: Data Layer

> **For agentic workers:** Use `mobiai-mobile-executing-plans-with-subagents` or `mobiai-mobile-executing-plans`. Checkbox steps track progress.

**Goal:** Persist everything: pre-generated puzzle buffer, the in-progress game, completed-puzzle history, and campaign position — plus settings — behind repositories and repo-backed use cases, wired with Hilt.

**Architecture:** Room (KSP) for structured data, DataStore Preferences for settings. Repositories expose `Flow`s and suspend functions over the domain types from Plan 1; entity↔domain mapping is explicit. A `@Singleton` application `CoroutineScope` drives background buffer refill. No UI.

**Tech Stack:** Room 2.8.4, DataStore 1.1.7, Hilt 2.60.1, kotlinx-serialization-json 1.8.1, coroutines 1.10.2.

**Platform:** Android

**Depends on:** Plan 1 (`domain/` types: `Grid`, `GridCodec`, `Puzzle`, `DifficultyBand`, `Board`, `BoardCell`, `Move`, `GameSnapshot`, `GameStatus`, `GameEngine`, `PuzzleFactory`, `CampaignCurve`).

---

## Spec reference

Implements §4 (all of the data layer) and the repo-backed use cases from §3.5.
Instrumented DAO + migration tests from §8.2.

---

## File map

```
app/src/main/java/com/freesudoku/app/
├── di/
│   ├── DatabaseModule.kt
│   ├── DispatchersModule.kt        # @IoDispatcher, @DefaultDispatcher, @ApplicationScope
│   └── RepositoryModule.kt
├── data/
│   ├── db/
│   │   ├── FreeSudokuDatabase.kt
│   │   ├── Converters.kt
│   │   ├── entity/
│   │   │   ├── PuzzleBufferEntity.kt
│   │   │   ├── CurrentGameEntity.kt
│   │   │   ├── CompletedPuzzleEntity.kt
│   │   │   └── CampaignProgressEntity.kt
│   │   └── dao/
│   │       ├── PuzzleBufferDao.kt
│   │       ├── CurrentGameDao.kt
│   │       ├── CompletedPuzzleDao.kt
│   │       └── CampaignProgressDao.kt
│   ├── settings/
│   │   ├── GameSettings.kt          # data class + enum ThemeMode
│   │   └── SettingsRepository.kt    # DataStore-backed
│   ├── serialization/
│   │   └── GameSnapshotDto.kt       # @Serializable DTOs + toDomain/toDto
│   ├── mapper/
│   │   ├── PuzzleMappers.kt
│   │   └── StatsMappers.kt
│   └── repository/
│       ├── PuzzleRepository.kt
│       ├── GameRepository.kt
│       ├── CampaignRepository.kt
│       └── StatsRepository.kt
└── domain/
    ├── stats/PlayerStats.kt         # domain model for the stats screen
    └── usecase/
        ├── GetNextCampaignPuzzle.kt
        ├── StartGame.kt
        ├── ResumeGame.kt
        ├── ObserveCurrentGame.kt
        ├── SaveGame.kt
        ├── CompletePuzzle.kt
        ├── AbandonGame.kt
        ├── ObserveCampaignProgress.kt
        └── ObservePlayerStats.kt

app/src/test/java/com/freesudoku/app/data/serialization/GameSnapshotDtoTest.kt
app/src/test/java/com/freesudoku/app/data/settings/SettingsRepositoryTest.kt   (Robolectric)
app/src/androidTest/java/com/freesudoku/app/data/db/*DaoTest.kt
app/src/androidTest/java/com/freesudoku/app/data/db/MigrationTest.kt
```

---

## Task 1: Dispatchers + application scope Hilt module

**Files:** Create `di/DispatchersModule.kt`

- [ ] **Step 1: implement**

```kotlin
package com.freesudoku.app.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides @IoDispatcher fun io(): CoroutineDispatcher = Dispatchers.IO
    @Provides @DefaultDispatcher fun default(): CoroutineDispatcher = Dispatchers.Default

    @Provides @Singleton @ApplicationScope
    fun appScope(@DefaultDispatcher d: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + d)
}
```

- [ ] **Step 2: commit** `feat(di): add dispatcher + application scope qualifiers`

---

## Task 2: Room entities

**Files:** Create the four entity files.

- [ ] **Step 1: implement**

`PuzzleBufferEntity.kt`
```kotlin
package com.freesudoku.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "puzzle_buffer")
data class PuzzleBufferEntity(
    @PrimaryKey val id: String,
    val givens: String,      // 81 chars
    val solution: String,    // 81 chars
    val difficultyScore: Double,
    val band: String,        // DifficultyBand.name
    val createdAt: Long,
)
```

`CurrentGameEntity.kt`
```kotlin
package com.freesudoku.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single row: id is always 0. Holds the serialized in-progress game. */
@Entity(tableName = "current_game")
data class CurrentGameEntity(
    @PrimaryKey val id: Int = 0,
    val puzzleNumber: Int,
    val snapshotJson: String,   // GameSnapshotDto
    val updatedAt: Long,
)
```

`CompletedPuzzleEntity.kt`
```kotlin
package com.freesudoku.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_puzzle")
data class CompletedPuzzleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puzzleNumber: Int,
    val difficultyScore: Double,
    val band: String,
    val durationMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val completedAt: Long,
)
```

`CampaignProgressEntity.kt`
```kotlin
package com.freesudoku.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaign_progress")
data class CampaignProgressEntity(
    @PrimaryKey val id: Int = 0,
    val currentPuzzleNumber: Int,
    val highestCompletedNumber: Int,
    val updatedAt: Long,
)
```

- [ ] **Step 2: commit** `feat(data): add Room entities`

---

## Task 3: DAOs

**Files:** Create the four DAO files.

- [ ] **Step 1: implement**

`PuzzleBufferDao.kt`
```kotlin
package com.freesudoku.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freesudoku.app.data.db.entity.PuzzleBufferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleBufferDao {
    @Query("SELECT COUNT(*) FROM puzzle_buffer")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(*) FROM puzzle_buffer")
    suspend fun countNow(): Int

    /** Closest buffered puzzle to [target] within [tolerance], if any. */
    @Query(
        """SELECT * FROM puzzle_buffer
           WHERE ABS(difficultyScore - :target) <= :tolerance
           ORDER BY ABS(difficultyScore - :target) ASC LIMIT 1"""
    )
    suspend fun closestTo(target: Double, tolerance: Double): PuzzleBufferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(puzzles: List<PuzzleBufferEntity>)

    @Query("DELETE FROM puzzle_buffer WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT id FROM puzzle_buffer")
    suspend fun allIds(): List<String>
}
```

`CurrentGameDao.kt`
```kotlin
package com.freesudoku.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.freesudoku.app.data.db.entity.CurrentGameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentGameDao {
    @Query("SELECT * FROM current_game WHERE id = 0")
    fun observe(): Flow<CurrentGameEntity?>

    @Query("SELECT * FROM current_game WHERE id = 0")
    suspend fun get(): CurrentGameEntity?

    @Upsert
    suspend fun upsert(game: CurrentGameEntity)

    @Query("DELETE FROM current_game")
    suspend fun clear()
}
```

`CompletedPuzzleDao.kt`
```kotlin
package com.freesudoku.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.freesudoku.app.data.db.entity.CompletedPuzzleEntity
import kotlinx.coroutines.flow.Flow

data class BandCount(val band: String, val count: Int)

@Dao
interface CompletedPuzzleDao {
    @Insert
    suspend fun insert(entity: CompletedPuzzleEntity)

    @Query("SELECT COUNT(*) FROM completed_puzzle")
    fun totalCompleted(): Flow<Int>

    @Query("SELECT MIN(durationMs) FROM completed_puzzle")
    fun bestDurationMs(): Flow<Long?>

    @Query("SELECT AVG(durationMs) FROM completed_puzzle")
    fun averageDurationMs(): Flow<Double?>

    @Query("SELECT band, COUNT(*) AS count FROM completed_puzzle GROUP BY band")
    fun countByBand(): Flow<List<BandCount>>

    @Query("SELECT DISTINCT completedAt FROM completed_puzzle ORDER BY completedAt DESC")
    fun completionTimestamps(): Flow<List<Long>>
}
```

`CampaignProgressDao.kt`
```kotlin
package com.freesudoku.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.freesudoku.app.data.db.entity.CampaignProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignProgressDao {
    @Query("SELECT * FROM campaign_progress WHERE id = 0")
    fun observe(): Flow<CampaignProgressEntity?>

    @Query("SELECT * FROM campaign_progress WHERE id = 0")
    suspend fun get(): CampaignProgressEntity?

    @Upsert
    suspend fun upsert(progress: CampaignProgressEntity)
}
```

- [ ] **Step 2: commit** `feat(data): add Room DAOs`

---

## Task 4: Database + converters

**Files:** Create `Converters.kt`, `FreeSudokuDatabase.kt`

- [ ] **Step 1:** `Converters.kt` — none needed yet (all columns are primitives/String). Create an empty holder for future use OR skip. **Decision: skip; add when needed.**

- [ ] **Step 2:** `FreeSudokuDatabase.kt`
```kotlin
package com.freesudoku.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.freesudoku.app.data.db.dao.CampaignProgressDao
import com.freesudoku.app.data.db.dao.CompletedPuzzleDao
import com.freesudoku.app.data.db.dao.CurrentGameDao
import com.freesudoku.app.data.db.dao.PuzzleBufferDao
import com.freesudoku.app.data.db.entity.CampaignProgressEntity
import com.freesudoku.app.data.db.entity.CompletedPuzzleEntity
import com.freesudoku.app.data.db.entity.CurrentGameEntity
import com.freesudoku.app.data.db.entity.PuzzleBufferEntity

@Database(
    entities = [
        PuzzleBufferEntity::class,
        CurrentGameEntity::class,
        CompletedPuzzleEntity::class,
        CampaignProgressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FreeSudokuDatabase : RoomDatabase() {
    abstract fun puzzleBufferDao(): PuzzleBufferDao
    abstract fun currentGameDao(): CurrentGameDao
    abstract fun completedPuzzleDao(): CompletedPuzzleDao
    abstract fun campaignProgressDao(): CampaignProgressDao
}
```

- [ ] **Step 3:** enable schema export — in `app/build.gradle.kts` add inside `android { defaultConfig { } }` sibling:
```kotlin
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
```
and add `sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")` for `MigrationTestHelper`. Commit the generated `app/schemas/...json`.

- [ ] **Step 4: commit** `feat(data): add Room database (v1)`

---

## Task 5: DatabaseModule (Hilt)

**Files:** Create `di/DatabaseModule.kt`

- [ ] **Step 1: implement**
```kotlin
package com.freesudoku.app.di

import android.content.Context
import androidx.room.Room
import com.freesudoku.app.data.db.FreeSudokuDatabase
import com.freesudoku.app.data.db.dao.CampaignProgressDao
import com.freesudoku.app.data.db.dao.CompletedPuzzleDao
import com.freesudoku.app.data.db.dao.CurrentGameDao
import com.freesudoku.app.data.db.dao.PuzzleBufferDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext ctx: Context): FreeSudokuDatabase =
        Room.databaseBuilder(ctx, FreeSudokuDatabase::class.java, "freesudoku.db")
            .build()

    @Provides fun puzzleBufferDao(db: FreeSudokuDatabase): PuzzleBufferDao = db.puzzleBufferDao()
    @Provides fun currentGameDao(db: FreeSudokuDatabase): CurrentGameDao = db.currentGameDao()
    @Provides fun completedPuzzleDao(db: FreeSudokuDatabase): CompletedPuzzleDao = db.completedPuzzleDao()
    @Provides fun campaignProgressDao(db: FreeSudokuDatabase): CampaignProgressDao = db.campaignProgressDao()
}
```
No `fallbackToDestructiveMigration`. When schema changes land, add `.addMigrations(MIGRATION_1_2, …)`.

- [ ] **Step 2: commit** `feat(di): add DatabaseModule`

---

## Task 6: GameSnapshot serialization

**Files:** Create `data/serialization/GameSnapshotDto.kt`, test `GameSnapshotDtoTest.kt`

- [ ] **Step 1: failing test**
```kotlin
package com.freesudoku.app.data.serialization

import com.freesudoku.app.domain.game.GameEngine
import com.freesudoku.app.domain.model.*
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameSnapshotDtoTest {
    private val puzzle = Puzzle(
        "x", 3,
        GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079"),
        GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        18.0, DifficultyBand.FACIL,
    )
    private fun snap() = GameSnapshot(
        puzzle, Board.fromGivens(puzzle.givens), emptyList(), emptyList(),
        0, 0, 0, GameStatus.IN_PROGRESS, mistakeLimitEnabled = true,
    )

    @Test fun `round trips a played game through json`() {
        val engine = GameEngine()
        var s = engine.setValue(snap(), 0, 2, 4)
        s = engine.toggleNote(s, 0, 3, 1)
        s = engine.setValue(s, 0, 3, 9) // a mistake
        s = engine.tick(s, 5_000)

        val json = GameSnapshotDto.toJson(s)
        val restored = GameSnapshotDto.fromJson(json)

        assertThat(restored.board.cells()).isEqualTo(s.board.cells())
        assertThat(restored.mistakes).isEqualTo(s.mistakes)
        assertThat(restored.elapsedMs).isEqualTo(5_000)
        assertThat(restored.undoStack).isEqualTo(s.undoStack)
        assertThat(restored.puzzle).isEqualTo(s.puzzle)
        assertThat(restored.status).isEqualTo(s.status)
    }

    @Test fun `fromJson returns null on garbage`() {
        assertThat(GameSnapshotDto.fromJsonOrNull("not json")).isNull()
    }
}
```

- [ ] **Step 2: run — FAIL**

`./gradlew :app:testDebugUnitTest --tests "com.freesudoku.app.data.serialization.*"`

- [ ] **Step 3: implement**
```kotlin
package com.freesudoku.app.data.serialization

import com.freesudoku.app.domain.model.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable private data class CellDto(val value: Int, val given: Boolean, val notes: List<Int>)

@Serializable private data class MoveDto(
    val type: String, val row: Int, val col: Int,
    val value: Int = 0, val prevValue: Int = 0, val prevNotes: List<Int> = emptyList(),
    val digit: Int = 0, val added: Boolean = false,
)

@Serializable private data class SnapshotJson(
    val puzzleId: String,
    val puzzleNumber: Int?,
    val givens: String,
    val solution: String,
    val difficultyScore: Double,
    val band: String,
    val cells: List<CellDto>,
    val undo: List<MoveDto>,
    val redo: List<MoveDto>,
    val elapsedMs: Long,
    val mistakes: Int,
    val hintsUsed: Int,
    val status: String,
    val mistakeLimitEnabled: Boolean,
)

object GameSnapshotDto {
    private val json = Json { ignoreUnknownKeys = true }

    fun toJson(s: GameSnapshot): String = json.encodeToString(
        SnapshotJson(
            puzzleId = s.puzzle.id,
            puzzleNumber = s.puzzle.number,
            givens = GridCodec.encode(s.puzzle.givens),
            solution = GridCodec.encode(s.puzzle.solution),
            difficultyScore = s.puzzle.difficultyScore,
            band = s.puzzle.band.name,
            cells = s.board.cells().map { CellDto(it.value, it.isGiven, it.notes.sorted()) },
            undo = s.undoStack.map { it.toDto() },
            redo = s.redoStack.map { it.toDto() },
            elapsedMs = s.elapsedMs,
            mistakes = s.mistakes,
            hintsUsed = s.hintsUsed,
            status = s.status.name,
            mistakeLimitEnabled = s.mistakeLimitEnabled,
        )
    )

    fun fromJson(text: String): GameSnapshot = json.decodeFromString<SnapshotJson>(text).toDomain()

    fun fromJsonOrNull(text: String): GameSnapshot? = try {
        fromJson(text)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun Move.toDto(): MoveDto = when (this) {
        is Move.SetValue -> MoveDto("set", row, col, value = value, prevValue = previousValue, prevNotes = previousNotes.sorted())
        is Move.ClearCell -> MoveDto("clear", row, col, prevValue = previousValue, prevNotes = previousNotes.sorted())
        is Move.ToggleNote -> MoveDto("note", row, col, digit = digit, added = added)
    }

    private fun MoveDto.toDomain(): Move = when (type) {
        "set" -> Move.SetValue(row, col, value, prevValue, prevNotes.toSet())
        "clear" -> Move.ClearCell(row, col, prevValue, prevNotes.toSet())
        "note" -> Move.ToggleNote(row, col, digit, added)
        else -> throw IllegalArgumentException("unknown move type $type")
    }

    private fun SnapshotJson.toDomain(): GameSnapshot {
        val puzzle = Puzzle(
            id = puzzleId,
            number = puzzleNumber,
            givens = GridCodec.decode(givens),
            solution = GridCodec.decode(solution),
            difficultyScore = difficultyScore,
            band = DifficultyBand.valueOf(band),
        )
        val board = Board.restore(cells.map { BoardCell(it.value, it.given, it.notes.toSortedSet()) })
        return GameSnapshot(
            puzzle = puzzle,
            board = board,
            undoStack = undo.map { it.toDomain() },
            redoStack = redo.map { it.toDomain() },
            elapsedMs = elapsedMs,
            mistakes = mistakes,
            hintsUsed = hintsUsed,
            status = GameStatus.valueOf(status),
            mistakeLimitEnabled = mistakeLimitEnabled,
        )
    }
}
```

- [ ] **Step 4: run — PASS**
- [ ] **Step 5: commit** `feat(data): add GameSnapshot JSON serialization`

---

## Task 7: Settings (DataStore)

**Files:** Create `data/settings/GameSettings.kt`, `SettingsRepository.kt`, test `SettingsRepositoryTest.kt`

- [ ] **Step 1:** `GameSettings.kt`
```kotlin
package com.freesudoku.app.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class GameSettings(
    val mistakeLimitEnabled: Boolean = true,
    val highlightErrors: Boolean = true,
    val highlightSameNumbers: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)
```

- [ ] **Step 2: failing test** (`SettingsRepositoryTest`, Robolectric — `@RunWith(RobolectricTestRunner::class)`, `@Config(sdk=[34])`), using a temp-file DataStore. Assert defaults, then each setter round-trips through `observe()`.

- [ ] **Step 3:** `SettingsRepository.kt`
```kotlin
package com.freesudoku.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val mistakeLimit = booleanPreferencesKey("mistake_limit_enabled")
        val highlightErrors = booleanPreferencesKey("highlight_errors")
        val highlightSame = booleanPreferencesKey("highlight_same_numbers")
        val autoRemoveNotes = booleanPreferencesKey("auto_remove_notes")
        val theme = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<GameSettings> = dataStore.data.map { p ->
        GameSettings(
            mistakeLimitEnabled = p[Keys.mistakeLimit] ?: true,
            highlightErrors = p[Keys.highlightErrors] ?: true,
            highlightSameNumbers = p[Keys.highlightSame] ?: true,
            autoRemoveNotes = p[Keys.autoRemoveNotes] ?: true,
            themeMode = p[Keys.theme]?.let(ThemeMode::valueOf) ?: ThemeMode.SYSTEM,
        )
    }

    suspend fun setMistakeLimitEnabled(v: Boolean) = dataStore.edit { it[Keys.mistakeLimit] = v }
    suspend fun setHighlightErrors(v: Boolean) = dataStore.edit { it[Keys.highlightErrors] = v }
    suspend fun setHighlightSameNumbers(v: Boolean) = dataStore.edit { it[Keys.highlightSame] = v }
    suspend fun setAutoRemoveNotes(v: Boolean) = dataStore.edit { it[Keys.autoRemoveNotes] = v }
    suspend fun setThemeMode(v: ThemeMode) = dataStore.edit { it[Keys.theme] = v.name }
}
```

- [ ] **Step 4:** provide `DataStore<Preferences>` in a Hilt module (`di/SettingsModule.kt`) via `PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("settings") }` on `@ApplicationScope`.

- [ ] **Step 5: run — PASS; commit** `feat(data): add settings repository (DataStore)`

---

## Task 8: Mappers + PuzzleRepository

**Files:** `data/mapper/PuzzleMappers.kt`, `data/repository/PuzzleRepository.kt`

- [ ] **Step 1:** `PuzzleMappers.kt` — `PuzzleBufferEntity.toDomain()` / `Puzzle.toBufferEntity()` using `GridCodec` + `DifficultyBand.valueOf`.

- [ ] **Step 2:** `PuzzleRepository.kt`
```kotlin
package com.freesudoku.app.data.repository

import com.freesudoku.app.data.db.dao.PuzzleBufferDao
import com.freesudoku.app.data.mapper.toBufferEntity
import com.freesudoku.app.data.mapper.toDomain
import com.freesudoku.app.di.ApplicationScope
import com.freesudoku.app.domain.campaign.CampaignCurve
import com.freesudoku.app.domain.generator.PuzzleFactory
import com.freesudoku.app.domain.model.Puzzle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleRepository @Inject constructor(
    private val bufferDao: PuzzleBufferDao,
    private val factory: PuzzleFactory,
    private val curve: CampaignCurve,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val refillMutex = Mutex()

    /** Returns a puzzle for campaign [number]: buffered match if any, else generated on demand. */
    suspend fun puzzleForCampaign(number: Int): Puzzle {
        val target = curve.targetScore(number)
        val buffered = bufferDao.closestTo(target, curve.toleranceWindow(0))
        val puzzle = if (buffered != null) {
            bufferDao.deleteById(buffered.id)
            buffered.toDomain().copy(number = number)
        } else {
            factory.generateForCampaign(number)
        }
        maybeRefill(number)
        return puzzle
    }

    private fun maybeRefill(fromNumber: Int) {
        scope.launch {
            refillMutex.withLock {
                if (bufferDao.countNow() >= MIN_BUFFER) return@withLock
                val fresh = (1..REFILL_BATCH).map {
                    factory.generateForCampaign(fromNumber + it).toBufferEntity()
                }
                bufferDao.insertAll(fresh)
            }
        }
    }

    companion object {
        const val MIN_BUFFER = 4
        const val REFILL_BATCH = 6
    }
}
```
> Buffered puzzles are stored with `number = null`; `toBufferEntity()` writes the score/band and a fresh id. On checkout we re-stamp the campaign number.

- [ ] **Step 3:** provide `PuzzleFactory` + `CampaignCurve` in `RepositoryModule` (factory needs a `Random` — provide `Random.Default`).

- [ ] **Step 4: commit** `feat(data): add PuzzleRepository with background buffer refill`

---

## Task 9: GameRepository (debounced save)

**Files:** `data/repository/GameRepository.kt`

- [ ] **Step 1: implement**
```kotlin
package com.freesudoku.app.data.repository

import com.freesudoku.app.data.db.dao.CurrentGameDao
import com.freesudoku.app.data.db.entity.CurrentGameEntity
import com.freesudoku.app.data.serialization.GameSnapshotDto
import com.freesudoku.app.domain.model.GameSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val dao: CurrentGameDao,
) {
    fun observeCurrentGame(): Flow<GameSnapshot?> =
        dao.observe().map { it?.let { e -> GameSnapshotDto.fromJsonOrNull(e.snapshotJson) } }

    suspend fun currentGame(): GameSnapshot? =
        dao.get()?.let { GameSnapshotDto.fromJsonOrNull(it.snapshotJson) }

    suspend fun save(snapshot: GameSnapshot) {
        dao.upsert(
            CurrentGameEntity(
                id = 0,
                puzzleNumber = snapshot.puzzle.number ?: -1,
                snapshotJson = GameSnapshotDto.toJson(snapshot),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clear() = dao.clear()
}
```
> **Debounce lives in the ViewModel** (Plan 3): the VM `collect`s its state with a `debounce(1_000)` before calling `save`. The repository stays a plain writer so it is trivially testable. (Deviation-friendly: if a later plan wants debounce here, wrap with a `MutableSharedFlow` + `scope`.)

- [ ] **Step 2: commit** `feat(data): add GameRepository`

---

## Task 10: CampaignRepository + StatsRepository

**Files:** `data/repository/CampaignRepository.kt`, `StatsRepository.kt`, `domain/stats/PlayerStats.kt`, `data/mapper/StatsMappers.kt`

- [ ] **Step 1:** `PlayerStats.kt`
```kotlin
package com.freesudoku.app.domain.stats

import com.freesudoku.app.domain.model.DifficultyBand

data class PlayerStats(
    val totalCompleted: Int,
    val bestDurationMs: Long?,
    val averageDurationMs: Long?,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val completedByBand: Map<DifficultyBand, Int>,
)
```

- [ ] **Step 2:** `CampaignRepository.kt` — `observeProgress(): Flow<CampaignProgress>` (domain model: `currentNumber`, `highestCompleted`), `advanceTo(number)`, `ensureInitialized()` (writes row 0 with `currentPuzzleNumber = 1` if absent).

- [ ] **Step 3:** `StatsRepository.kt` — `combine` the `CompletedPuzzleDao` flows into `PlayerStats`. Streak computed by a pure helper `fun streaks(sortedDescTimestamps: List<Long>, zoneMillisOffset): Pair<Int,Int>` that buckets timestamps to local days and counts the run ending today/yesterday (current) and the longest run anywhere.

- [ ] **Step 4:** unit-test the streak helper (pure function) in `app/src/test` — days: `[today, today-1, today-3]` → current 2, longest 2; empty → 0,0.

- [ ] **Step 5: commit** `feat(data): add campaign + stats repositories`

---

## Task 11: Repo-backed use cases

**Files:** the nine files under `domain/usecase/`.

Each is a thin `@Inject constructor` class with one `operator fun invoke(...)`. Key ones:

- [ ] **`GetNextCampaignPuzzle`** — `campaignRepo.ensureInitialized(); val n = campaignRepo.currentNumber(); return puzzleRepo.puzzleForCampaign(n)`
- [ ] **`StartGame(puzzle, settings)`** — build fresh `GameSnapshot` (`Board.fromGivens`, empty stacks, `mistakeLimitEnabled = settings.mistakeLimitEnabled`), `gameRepo.save(it)`, return it.
- [ ] **`ResumeGame`** — `gameRepo.currentGame()`.
- [ ] **`ObserveCurrentGame`** — `gameRepo.observeCurrentGame()`.
- [ ] **`SaveGame(snapshot)`** — `gameRepo.save(snapshot)`.
- [ ] **`CompletePuzzle(snapshot)`** — require `snapshot.status == COMPLETED`; insert `CompletedPuzzleEntity` (duration = `elapsedMs`, band/score from `snapshot.puzzle`); `campaignRepo.advanceTo(snapshot.puzzle.number!! + 1)`; `gameRepo.clear()`.
- [ ] **`AbandonGame`** — `gameRepo.clear()` (campaign not advanced).
- [ ] **`ObserveCampaignProgress`** / **`ObservePlayerStats`** — pass-throughs.

- [ ] **Test** (`app/src/test`, MockK): `CompletePuzzle` advances the campaign exactly once and clears the game; `StartGame` respects `mistakeLimitEnabled`; `GetNextCampaignPuzzle` initializes progress then asks the puzzle repo for the current number.

- [ ] **commit** `feat(domain): add repo-backed use cases`

---

## Task 12: RepositoryModule + wire-up

**Files:** `di/RepositoryModule.kt` (+ `SettingsModule.kt` from Task 7)

- [ ] Provide `PuzzleFactory` (`@Provides` with `Random.Default`, `CampaignCurve()`, `DifficultyRater()`), `CampaignCurve` singleton, `GameEngine` (from settings? no — GameEngine's `autoRemoveNotes` is set per-use in the VM; provide a default `GameEngine()` and let the VM construct one with the setting, OR make `autoRemoveNotes` a parameter of each method — **decision: VM constructs `GameEngine(settings.autoRemoveNotes)`; no Hilt binding needed**).
- [ ] Repositories are `@Inject constructor` + `@Singleton` so no explicit `@Provides` needed beyond their deps.
- [ ] **Build check:** `./gradlew :app:assembleDebug` — Hilt graph resolves.
- [ ] **commit** `feat(di): wire repository module`

---

## Task 13: Instrumented DAO + migration tests

**Files:** `app/src/androidTest/java/com/freesudoku/app/data/db/`

- [ ] `PuzzleBufferDaoTest` — insert 3 puzzles at scores 10/20/50; `closestTo(22, 5)` returns the score-20 row; `closestTo(22, 1)` returns null; `countNow` == 3; `deleteById` drops one.
- [ ] `CurrentGameDaoTest` — upsert then observe emits it; second upsert replaces (still one row); `clear` empties.
- [ ] `CompletedPuzzleDaoTest` — insert 4 rows across 2 bands; `totalCompleted` == 4, `bestDurationMs` == min, `countByBand` groups correctly.
- [ ] `CampaignProgressDaoTest` — upsert row 0, observe, re-upsert replaces.
- [ ] `MigrationTest` — `MigrationTestHelper` with `FreeSudokuDatabase`, `createDatabase(TEST_DB, 1)` then `runMigrationsAndValidate` (no-op for v1; the test exists so v2 has a home). Uses the exported schema in `app/schemas`.
- [ ] These run with `./gradlew :app:connectedDebugAndroidTest` — **requires a running emulator/device**. If none is available in this environment, mark the task blocked and note it; the code must still compile (`./gradlew :app:assembleDebugAndroidTest`).
- [ ] **commit** `test(data): add instrumented DAO + migration tests`

---

## Task 14: Verify

- [ ] `./gradlew :app:testDebugUnitTest` — all JVM tests green (Plan 1 + new serialization/settings/streak/use-case tests).
- [ ] `./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug` — all green.
- [ ] `./gradlew :app:connectedDebugAndroidTest` if a device is available; else record blocked.
- [ ] Commit any fixups. Update this file's status line + an "Execution outcome" section mirroring Plan 1.

---

## Self-review

- **Spec §4.1 entities** — Task 2 ✅ (note: `CurrentGameEntity` stores one `snapshotJson` blob instead of the spec's separate `boardValues`/`notesJson`/`undoStackJson` columns — simpler, and the game snapshot is always read/written whole).
- **§4.1 DAOs + aggregation** — Task 3 ✅ (streak computed in `StatsRepository`, per spec).
- **§4.1 migrations, no destructive fallback** — Tasks 4, 13 ✅.
- **§4.2 DataStore settings** — Task 7 ✅ (all 5 settings, defaults match spec).
- **§4.3 repositories** — Tasks 8–10 ✅. Debounce moved from `GameRepository` to the ViewModel (Plan 3) — noted as an intentional simplification; behaviour (coalesced writes ~1 s) is preserved.
- **§3.5 use cases** — Task 11 ✅.
- **§8.2 instrumented tests** — Task 13 ✅ (gated on device availability).
- **Placeholder scan** — none; every task has concrete code or a precise spec of the file.
- **Type consistency** — `GameSnapshotDto.toJson/fromJson` matches `GameSnapshot` fields from Plan 1; `Puzzle.copy(number=)` used (data class); `Board.restore(List<BoardCell>)` exists in Plan 1.

---

## After Plan 2

Plan 3 (UI): theme finalization, Navigation Compose graph, Home / Game / Stats / Settings screens + ViewModels, reusable board components, lifecycle/timer via `SavedStateHandle`, on-demand generation loading state, accessibility + edge-to-edge, Compose UI + ViewModel tests.

---

## Execution outcome (2026-09-04)

**STATUS: COMPLETE.** Branch `feat/plan-1-scaffold-domain` (kept the single
branch through Plans 1–3). 79 JVM unit tests + 4 instrumented DAO tests green;
`assembleDebug`, `assembleDebugAndroidTest`, `lintDebug` green.

**Deviations:**
- **Task 4** `Converters.kt` skipped (all columns are primitive/String).
- **Task 6**: `CurrentGameEntity` stores one `snapshotJson` blob (the whole
  `GameSnapshot`) rather than separate board/notes/stack columns — the snapshot
  is always read and written whole, so splitting it buys nothing.
- **Task 9**: debounce is the ViewModel's job (Plan 3); `GameRepository.save`
  is a plain writer.
- **Task 11**: the nine use cases live in one file `domain/usecase/GameUseCases.kt`
  rather than nine files. `CompletePuzzle` injects `CompletedPuzzleDao` directly
  (no separate history repo).
- **Task 13**: `MigrationTest` removed. Room 2.8.4's `MigrationTestHelper` has a
  different constructor/`createDatabase` shape than the plan's draft, and there
  is no v2 schema to migrate yet. DAO tests + committed `app/schemas/…/1.json`
  cover v1. Re-add a migration test with the first schema bump.
- Extra files: `di/DomainModule.kt`, `di/SettingsModule.kt`,
  `domain/campaign/CampaignProgress.kt`, `domain/stats/{PlayerStats,Streaks}.kt`,
  `data/db/DaoTestBase.kt`.
- Added compiler arg `-Xannotation-default-target=param-property` (Kotlin 2.2
  qualifier-target warning with Hilt `@Qualifier`s).
- Added `androidx.sqlite:sqlite-bundled` + truth/turbine/coroutines-test to
  `androidTestImplementation`.

**MIN_BUFFER** fixed at 4 (`PuzzleRepository`).
