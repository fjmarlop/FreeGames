package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GridTest {

    @Test fun `empty grid has 81 zero cells`() {
        val g = Grid.empty()
        assertThat(g.cells.size).isEqualTo(81)
        assertThat(g.cells.all { it == 0 }).isTrue()
        assertThat(g.isFull).isFalse()
    }

    @Test fun `value at row col round trips`() {
        val g = Grid.empty().withValue(row = 3, col = 5, value = 7)
        assertThat(g.valueAt(3, 5)).isEqualTo(7)
        assertThat(g.valueAt(0, 0)).isEqualTo(0)
        assertThat(g.filledCount).isEqualTo(1)
    }

    @Test fun `boxIndex maps 3x3 blocks`() {
        assertThat(Grid.boxIndex(0, 0)).isEqualTo(0)
        assertThat(Grid.boxIndex(4, 4)).isEqualTo(4)
        assertThat(Grid.boxIndex(8, 8)).isEqualTo(8)
        assertThat(Grid.boxIndex(2, 5)).isEqualTo(1)
    }

    @Test fun `isValidPlacement rejects row column and box conflicts`() {
        val g = Grid.empty().withValue(0, 0, 5)
        assertThat(g.isValidPlacement(0, 8, 5)).isFalse() // same row
        assertThat(g.isValidPlacement(8, 0, 5)).isFalse() // same column
        assertThat(g.isValidPlacement(1, 1, 5)).isFalse() // same box
        assertThat(g.isValidPlacement(4, 4, 5)).isTrue()
    }

    @Test fun `equality is structural`() {
        assertThat(Grid.empty().withValue(1, 1, 9))
            .isEqualTo(Grid.empty().withValue(1, 1, 9))
    }

    @Test fun `isComplete true only when full and consistent`() {
        val solved = GridCodec.decode(SOLVED_81)
        assertThat(solved.isComplete).isTrue()
        assertThat(Grid.empty().isComplete).isFalse()
    }

    private companion object {
        const val SOLVED_81 =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    }
}
