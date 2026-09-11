package com.freesudoku.app.data.serialization

import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.BoardCell
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Move
import com.freesudoku.app.domain.model.Puzzle
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
private data class CellDto(val value: Int, val given: Boolean, val notes: List<Int>)

@Serializable
private data class MoveDto(
    val type: String,
    val row: Int,
    val col: Int,
    val value: Int = 0,
    val prevValue: Int = 0,
    val prevNotes: List<Int> = emptyList(),
    val digit: Int = 0,
    val added: Boolean = false,
)

@Serializable
private data class SnapshotJson(
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

/** JSON codec for the whole in-progress game, stored as one column in `current_game`. */
object GameSnapshotDto {

    private val json = Json { ignoreUnknownKeys = true }

    fun toJson(snapshot: GameSnapshot): String = json.encodeToString(
        SnapshotJson(
            puzzleId = snapshot.puzzle.id,
            puzzleNumber = snapshot.puzzle.number,
            givens = GridCodec.encode(snapshot.puzzle.givens),
            solution = GridCodec.encode(snapshot.puzzle.solution),
            difficultyScore = snapshot.puzzle.difficultyScore,
            band = snapshot.puzzle.band.name,
            cells = snapshot.board.cells().map { CellDto(it.value, it.isGiven, it.notes.sorted()) },
            undo = snapshot.undoStack.map { it.toDto() },
            redo = snapshot.redoStack.map { it.toDto() },
            elapsedMs = snapshot.elapsedMs,
            mistakes = snapshot.mistakes,
            hintsUsed = snapshot.hintsUsed,
            status = snapshot.status.name,
            mistakeLimitEnabled = snapshot.mistakeLimitEnabled,
        )
    )

    fun fromJson(text: String): GameSnapshot = json.decodeFromString<SnapshotJson>(text).toDomain()

    fun fromJsonOrNull(text: String): GameSnapshot? = try {
        fromJson(text)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun Move.toDto(): MoveDto = when (this) {
        is Move.SetValue -> MoveDto(
            type = "set", row = row, col = col,
            value = value, prevValue = previousValue, prevNotes = previousNotes.sorted(),
        )
        is Move.ClearCell -> MoveDto(
            type = "clear", row = row, col = col,
            prevValue = previousValue, prevNotes = previousNotes.sorted(),
        )
        is Move.ToggleNote -> MoveDto(
            type = "note", row = row, col = col, digit = digit, added = added,
        )
    }

    private fun MoveDto.toDomain(): Move = when (type) {
        "set" -> Move.SetValue(row, col, value, prevValue, prevNotes.toSortedSet())
        "clear" -> Move.ClearCell(row, col, prevValue, prevNotes.toSortedSet())
        "note" -> Move.ToggleNote(row, col, digit, added)
        else -> throw IllegalArgumentException("unknown move type '$type'")
    }

    private fun SnapshotJson.toDomain(): GameSnapshot {
        val puzzle = Puzzle(
            id = puzzleId,
            number = puzzleNumber,
            givens = GridCodec.decode(givens),
            solution = GridCodec.decode(solution),
            difficultyScore = difficultyScore,
            band = DifficultyBand.parseOrFloor(band),
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
