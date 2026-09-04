package com.freesudoku.app.domain.game

import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.Move

/**
 * Pure Sudoku game rules over a [GameSnapshot]. No Android, no coroutines, no persistence.
 * Every method returns a new snapshot (or the same instance when the move is a no-op).
 */
class GameEngine(private val autoRemoveNotes: Boolean = true) {

    fun setValue(snapshot: GameSnapshot, row: Int, col: Int, value: Int): GameSnapshot {
        if (snapshot.status != GameStatus.IN_PROGRESS) return snapshot
        val cell = snapshot.board.cell(row, col)
        if (cell.isGiven || cell.value == value) return snapshot

        var board = snapshot.board.withValue(row, col, value)
        if (autoRemoveNotes && value != 0) board = clearPeerNotes(board, row, col, value)

        val correct = value != 0 && snapshot.puzzle.solution.valueAt(row, col) == value
        val isMistake = value != 0 && !correct
        val mistakes = snapshot.mistakes + if (isMistake) 1 else 0

        val move = Move.SetValue(row, col, value, cell.value, cell.notes)
        val status = resolveStatus(snapshot, board, mistakes)

        return snapshot.copy(
            board = board,
            undoStack = snapshot.undoStack + move,
            redoStack = emptyList(),
            mistakes = mistakes,
            status = status,
        )
    }

    fun clear(snapshot: GameSnapshot, row: Int, col: Int): GameSnapshot {
        if (snapshot.status != GameStatus.IN_PROGRESS) return snapshot
        val cell = snapshot.board.cell(row, col)
        if (cell.isGiven || (cell.value == 0 && cell.notes.isEmpty())) return snapshot
        val move = Move.ClearCell(row, col, cell.value, cell.notes)
        return snapshot.copy(
            board = snapshot.board.withValue(row, col, 0).withNotes(row, col, emptySet()),
            undoStack = snapshot.undoStack + move,
            redoStack = emptyList(),
        )
    }

    fun toggleNote(snapshot: GameSnapshot, row: Int, col: Int, digit: Int): GameSnapshot {
        if (snapshot.status != GameStatus.IN_PROGRESS) return snapshot
        val cell = snapshot.board.cell(row, col)
        if (cell.isGiven || cell.value != 0) return snapshot
        val added = digit !in cell.notes
        val move = Move.ToggleNote(row, col, digit, added)
        return snapshot.copy(
            board = snapshot.board.withNoteToggled(row, col, digit),
            undoStack = snapshot.undoStack + move,
            redoStack = emptyList(),
        )
    }

    fun hint(snapshot: GameSnapshot, row: Int, col: Int): GameSnapshot {
        if (snapshot.status != GameStatus.IN_PROGRESS) return snapshot
        val cell = snapshot.board.cell(row, col)
        if (cell.isGiven || cell.value != 0) return snapshot
        val answer = snapshot.puzzle.solution.valueAt(row, col)
        var board = snapshot.board.withValue(row, col, answer)
        if (autoRemoveNotes) board = clearPeerNotes(board, row, col, answer)
        val move = Move.SetValue(row, col, answer, cell.value, cell.notes)
        return snapshot.copy(
            board = board,
            undoStack = snapshot.undoStack + move,
            redoStack = emptyList(),
            hintsUsed = snapshot.hintsUsed + 1,
            status = resolveStatus(snapshot, board, snapshot.mistakes),
        )
    }

    /** Reveals the empty cell that currently has the fewest legal candidates. */
    fun autoHint(snapshot: GameSnapshot): GameSnapshot {
        val grid = snapshot.board.toGrid()
        val target = snapshot.board.emptyCells().minByOrNull { (r, c) ->
            (1..9).count { grid.isValidPlacement(r, c, it) }
        } ?: return snapshot
        return hint(snapshot, target.first, target.second)
    }

    fun undo(snapshot: GameSnapshot): GameSnapshot {
        val move = snapshot.undoStack.lastOrNull() ?: return snapshot
        return snapshot.copy(
            board = revert(snapshot.board, move),
            undoStack = snapshot.undoStack.dropLast(1),
            redoStack = snapshot.redoStack + move,
            status = if (snapshot.status == GameStatus.COMPLETED) GameStatus.IN_PROGRESS else snapshot.status,
        )
    }

    fun redo(snapshot: GameSnapshot): GameSnapshot {
        val move = snapshot.redoStack.lastOrNull() ?: return snapshot
        val board = reapply(snapshot.board, move)
        return snapshot.copy(
            board = board,
            redoStack = snapshot.redoStack.dropLast(1),
            undoStack = snapshot.undoStack + move,
            status = resolveStatus(snapshot, board, snapshot.mistakes),
        )
    }

    fun tick(snapshot: GameSnapshot, deltaMs: Long): GameSnapshot =
        if (snapshot.status == GameStatus.IN_PROGRESS) {
            snapshot.copy(elapsedMs = snapshot.elapsedMs + deltaMs)
        } else {
            snapshot
        }

    private fun resolveStatus(snapshot: GameSnapshot, board: Board, mistakes: Int): GameStatus = when {
        snapshot.mistakeLimitEnabled && mistakes >= GameSnapshot.MISTAKE_LIMIT -> GameStatus.FAILED
        board.isComplete && board.toGrid() == snapshot.puzzle.solution -> GameStatus.COMPLETED
        else -> GameStatus.IN_PROGRESS
    }

    private fun revert(board: Board, move: Move): Board = when (move) {
        is Move.SetValue -> board.withValue(move.row, move.col, move.previousValue)
            .withNotes(move.row, move.col, move.previousNotes)
        is Move.ClearCell -> board.withValue(move.row, move.col, move.previousValue)
            .withNotes(move.row, move.col, move.previousNotes)
        is Move.ToggleNote -> board.withNoteToggled(move.row, move.col, move.digit)
    }

    private fun reapply(board: Board, move: Move): Board = when (move) {
        is Move.SetValue -> board.withValue(move.row, move.col, move.value)
        is Move.ClearCell -> board.withValue(move.row, move.col, 0).withNotes(move.row, move.col, emptySet())
        is Move.ToggleNote -> board.withNoteToggled(move.row, move.col, move.digit)
    }

    private fun clearPeerNotes(board: Board, row: Int, col: Int, digit: Int): Board {
        var b = board
        for (c in 0 until 9) if (digit in b.cell(row, c).notes) b = b.withNoteToggled(row, c, digit)
        for (r in 0 until 9) if (digit in b.cell(r, col).notes) b = b.withNoteToggled(r, col, digit)
        val br = (row / 3) * 3
        val bc = (col / 3) * 3
        for (r in br until br + 3) for (c in bc until bc + 3) {
            if (digit in b.cell(r, c).notes) b = b.withNoteToggled(r, c, digit)
        }
        return b
    }
}
