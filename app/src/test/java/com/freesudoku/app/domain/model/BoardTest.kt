package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BoardTest {

    private val givens = GridCodec.decode(
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    )
    private val solution = GridCodec.decode(
        "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    )

    @Test fun `board starts from givens with given flags`() {
        val b = Board.fromGivens(givens)
        assertThat(b.cell(0, 0).value).isEqualTo(5)
        assertThat(b.cell(0, 0).isGiven).isTrue()
        assertThat(b.cell(0, 2).value).isEqualTo(0)
        assertThat(b.cell(0, 2).isGiven).isFalse()
    }

    @Test fun `setValue on given cell is rejected`() {
        val b = Board.fromGivens(givens)
        assertThat(b.withValue(0, 0, 1)).isSameInstanceAs(b)
    }

    @Test fun `setValue then clear on non-given cell`() {
        val b = Board.fromGivens(givens).withValue(0, 2, 4)
        assertThat(b.cell(0, 2).value).isEqualTo(4)
        assertThat(b.withValue(0, 2, 0).cell(0, 2).value).isEqualTo(0)
    }

    @Test fun `notes toggle`() {
        val b = Board.fromGivens(givens).withNoteToggled(0, 2, 4).withNoteToggled(0, 2, 7)
        assertThat(b.cell(0, 2).notes).containsExactly(4, 7)
        assertThat(b.withNoteToggled(0, 2, 4).cell(0, 2).notes).containsExactly(7)
    }

    @Test fun `fresh board does not match solution`() {
        assertThat(Board.fromGivens(givens).toGrid()).isNotEqualTo(solution)
        assertThat(Board.fromGivens(givens).isComplete).isFalse()
    }

    @Test fun `restore round trips cells`() {
        val b = Board.fromGivens(givens).withValue(0, 2, 4)
        assertThat(Board.restore(b.cells()).cell(0, 2).value).isEqualTo(4)
    }
}
