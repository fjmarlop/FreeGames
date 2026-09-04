package com.freesudoku.app.domain.game

import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameEngineTest {

    private val engine = GameEngine()

    private val puzzle = Puzzle(
        id = "t",
        number = 1,
        givens = GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079"),
        solution = GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        difficultyScore = 15.0,
        band = DifficultyBand.FACIL,
    )

    private fun fresh(limit: Boolean = true) = GameSnapshot(
        puzzle = puzzle,
        board = Board.fromGivens(puzzle.givens),
        undoStack = emptyList(),
        redoStack = emptyList(),
        elapsedMs = 0,
        mistakes = 0,
        hintsUsed = 0,
        status = GameStatus.IN_PROGRESS,
        mistakeLimitEnabled = limit,
    )

    @Test fun `correct value is placed and is not a mistake`() {
        val s = engine.setValue(fresh(), 0, 2, 4)
        assertThat(s.board.cell(0, 2).value).isEqualTo(4)
        assertThat(s.mistakes).isEqualTo(0)
        assertThat(s.undoStack).hasSize(1)
    }

    @Test fun `wrong value increments mistakes`() {
        assertThat(engine.setValue(fresh(), 0, 2, 9).mistakes).isEqualTo(1)
    }

    @Test fun `three mistakes fail the game when the limit is on`() {
        var s = fresh()
        s = engine.setValue(s, 0, 2, 9)
        s = engine.setValue(s, 0, 3, 9)
        s = engine.setValue(s, 0, 5, 9)
        assertThat(s.status).isEqualTo(GameStatus.FAILED)
    }

    @Test fun `mistakes never fail the game in relaxed mode`() {
        var s = fresh(limit = false)
        repeat(5) { s = engine.setValue(s, 0, 2, 9); s = engine.setValue(s, 0, 2, 0) }
        assertThat(s.status).isEqualTo(GameStatus.IN_PROGRESS)
    }

    @Test fun `given cells cannot be edited`() {
        val s = engine.setValue(fresh(), 0, 0, 1)
        assertThat(s.board.cell(0, 0).value).isEqualTo(5)
        assertThat(s.undoStack).isEmpty()
    }

    @Test fun `undo then redo restores state`() {
        val s1 = engine.setValue(fresh(), 0, 2, 4)
        val s2 = engine.undo(s1)
        assertThat(s2.board.cell(0, 2).value).isEqualTo(0)
        assertThat(engine.redo(s2).board.cell(0, 2).value).isEqualTo(4)
    }

    @Test fun `toggle note adds then removes`() {
        var s = engine.toggleNote(fresh(), 0, 2, 4)
        assertThat(s.board.cell(0, 2).notes).containsExactly(4)
        s = engine.toggleNote(s, 0, 2, 4)
        assertThat(s.board.cell(0, 2).notes).isEmpty()
    }

    @Test fun `hint reveals the correct value and counts`() {
        val s = engine.hint(fresh(), 0, 2)
        assertThat(s.board.cell(0, 2).value).isEqualTo(4)
        assertThat(s.hintsUsed).isEqualTo(1)
    }

    @Test fun `autoHint fills a correct value`() {
        val s = engine.autoHint(fresh())
        assertThat(s.hintsUsed).isEqualTo(1)
        val filled = s.board.emptyCells().size
        assertThat(filled).isEqualTo(fresh().board.emptyCells().size - 1)
    }

    @Test fun `completing the board sets COMPLETED`() {
        var s = fresh()
        val sol = puzzle.solution
        for (r in 0 until 9) for (c in 0 until 9) {
            if (s.board.cell(r, c).value == 0) s = engine.setValue(s, r, c, sol.valueAt(r, c))
        }
        assertThat(s.status).isEqualTo(GameStatus.COMPLETED)
    }

    @Test fun `placing a value auto-removes matching notes from peers`() {
        var s = engine.toggleNote(fresh(), 0, 3, 4)
        s = engine.setValue(s, 0, 2, 4)
        assertThat(s.board.cell(0, 3).notes).doesNotContain(4)
    }

    @Test fun `tick only advances time while in progress`() {
        assertThat(engine.tick(fresh(), 1000).elapsedMs).isEqualTo(1000)
        val failed = fresh().copy(status = GameStatus.FAILED)
        assertThat(engine.tick(failed, 1000).elapsedMs).isEqualTo(0)
    }
}
