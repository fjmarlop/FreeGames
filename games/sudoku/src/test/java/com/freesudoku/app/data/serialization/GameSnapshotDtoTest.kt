package com.freesudoku.app.data.serialization

import com.freesudoku.app.domain.game.GameEngine
import com.freesudoku.app.domain.model.Board
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GameSnapshot
import com.freesudoku.app.domain.model.GameStatus
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameSnapshotDtoTest {

    private val puzzle = Puzzle(
        id = "x",
        number = 3,
        givens = GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079"),
        solution = GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        difficultyScore = 18.0,
        band = DifficultyBand.FACIL,
    )

    private fun snapshot() = GameSnapshot(
        puzzle = puzzle,
        board = Board.fromGivens(puzzle.givens),
        undoStack = emptyList(),
        redoStack = emptyList(),
        elapsedMs = 0,
        mistakes = 0,
        hintsUsed = 0,
        status = GameStatus.IN_PROGRESS,
        mistakeLimitEnabled = true,
    )

    @Test fun `round trips a played game through json`() {
        val engine = GameEngine()
        var s = engine.setValue(snapshot(), 0, 2, 4)
        s = engine.toggleNote(s, 0, 3, 1)
        s = engine.setValue(s, 0, 3, 9) // a mistake
        s = engine.tick(s, 5_000)

        val restored = GameSnapshotDto.fromJson(GameSnapshotDto.toJson(s))

        assertThat(restored.board.cells()).isEqualTo(s.board.cells())
        assertThat(restored.mistakes).isEqualTo(s.mistakes)
        assertThat(restored.elapsedMs).isEqualTo(5_000)
        assertThat(restored.undoStack).isEqualTo(s.undoStack)
        assertThat(restored.puzzle).isEqualTo(s.puzzle)
        assertThat(restored.status).isEqualTo(s.status)
        assertThat(restored.mistakeLimitEnabled).isTrue()
    }

    @Test fun `fromJsonOrNull returns null on garbage`() {
        assertThat(GameSnapshotDto.fromJsonOrNull("not json")).isNull()
        assertThat(GameSnapshotDto.fromJsonOrNull("{}")).isNull()
    }

    @Test fun `a snapshot persisted with a since-removed band deserializes as FACIL`() {
        val json = GameSnapshotDto.toJson(snapshot()).replace("\"FACIL\"", "\"PRINCIPIANTE\"")
        val restored = GameSnapshotDto.fromJson(json)
        assertThat(restored.puzzle.band).isEqualTo(DifficultyBand.FACIL)
    }
}
