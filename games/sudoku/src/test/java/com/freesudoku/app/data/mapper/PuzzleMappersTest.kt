package com.freesudoku.app.data.mapper

import com.freesudoku.app.data.db.entity.PuzzleBufferEntity
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PuzzleMappersTest {

    private val givens =
        GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079")
    private val solution =
        GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179")

    @Test fun `round trips a puzzle through the buffer entity`() {
        val puzzle = Puzzle(
            id = "abc",
            number = null,
            givens = givens,
            solution = solution,
            difficultyScore = 20.0,
            band = DifficultyBand.MEDIO,
        )
        val restored = puzzle.toBufferEntity(createdAt = 1L).toDomain()
        assertThat(restored).isEqualTo(puzzle)
    }

    @Test fun `a buffered row with a since-removed band deserializes as FACIL`() {
        val entity = PuzzleBufferEntity(
            id = "abc",
            givens = GridCodec.encode(givens),
            solution = GridCodec.encode(solution),
            difficultyScore = 5.0,
            band = "PRINCIPIANTE",
            createdAt = 1L,
        )
        assertThat(entity.toDomain().band).isEqualTo(DifficultyBand.FACIL)
    }
}
