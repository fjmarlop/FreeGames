package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.GridCodec
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SolutionCounterTest {

    private val uniquePuzzle =
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079"

    // The classic solution with a 6/7 unique rectangle (rows 0 & 3, cols 3 & 4, boxes 1 & 4)
    // blanked: exactly two solutions (the 6/7 swap).
    private val twoSolutionGrid =
        "534008912672195348198342567859001423426853791713924856961537284287419635345286179"

    @Test fun `unique puzzle counts exactly one`() {
        assertThat(SolutionCounter().countUpTo(GridCodec.decode(uniquePuzzle), limit = 2)).isEqualTo(1)
    }

    @Test fun `empty grid has many solutions and stops at the limit`() {
        assertThat(SolutionCounter().countUpTo(GridCodec.decode("0".repeat(81)), limit = 2)).isEqualTo(2)
    }

    @Test fun `deadly-rectangle grid counts exactly two`() {
        assertThat(SolutionCounter().countUpTo(GridCodec.decode(twoSolutionGrid), limit = 2)).isEqualTo(2)
    }

    @Test fun `contradictory grid counts zero`() {
        val bad = "1".repeat(2) + "0".repeat(79) // two 1s in row 0
        assertThat(SolutionCounter().countUpTo(GridCodec.decode(bad), limit = 2)).isEqualTo(0)
    }

    @Test fun `solve returns the unique completion`() {
        val solved = SolutionCounter().solve(GridCodec.decode(uniquePuzzle))!!
        assertThat(solved.isComplete).isTrue()
        assertThat(solved.valueAt(0, 2)).isEqualTo(4)
    }
}
