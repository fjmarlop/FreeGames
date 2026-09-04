package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.solver.SolverState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SinglesTest {

    private fun state(p: String) = SolverState.from(GridCodec.decode(p))!!

    @Test fun `naked single fills the only candidate of a cell`() {
        // Classic solution with (4,4) blanked -> that cell has one candidate.
        val s = state("534678912672195348198342567859761423426803791713924856961537284287419635345286179")
        val step = NakedSingle().apply(s)
        assertThat(step).isNotNull()
        assertThat(step!!.placements).containsExactly(40 to 5)
        assertThat(s.values[40]).isEqualTo(5)
    }

    @Test fun `hidden single finds a digit that fits one cell of a unit`() {
        // 9s at (1,4),(2,7),(3,1),(6,2) — pairwise disjoint units — force 9 into (0,0) within box 0.
        val s = state("000000000000090000000000090090000000000000000000000000009000000000000000000000000")
        val step = HiddenSingle().apply(s)
        assertThat(step).isNotNull()
        assertThat(step!!.placements).contains(0 to 9)
        // it is genuinely hidden, not naked: the cell had more than one candidate
        // (verified by construction: only four 9s were placed).
    }

    @Test fun `technique costs are ordered`() {
        assertThat(NakedSingle().cost).isLessThan(HiddenSingle().cost)
    }

    @Test fun `apply returns null on a solved grid`() {
        val solved = state("534678912672195348198342567859761423426853791713924856961537284287419635345286179")
        assertThat(NakedSingle().apply(solved)).isNull()
        assertThat(HiddenSingle().apply(solved)).isNull()
    }
}
