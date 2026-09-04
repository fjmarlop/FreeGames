package com.freesudoku.app.domain.solver

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LogicalSolverTest {

    @Test fun `solves an easy puzzle to the correct solution`() {
        val result = LogicalSolver().solve(SolverCorpus.EASY_GIVENS)
        assertThat(result.solved).isTrue()
        assertThat(result.finalGrid).isEqualTo(SolverCorpus.EASY_SOLUTION)
        assertThat(result.hardestTechnique).isNotNull()
    }

    @Test fun `never contradicts the real solution on a generated corpus`() {
        for (case in SolverCorpus.generate(count = 12)) {
            val result = LogicalSolver().solve(case.givens)
            for (i in 0 until 81) {
                val v = result.finalGrid.cells[i]
                if (v != 0) {
                    assertThat(v).isEqualTo(case.solution.cells[i])
                }
            }
        }
    }

    @Test fun `solves the large majority of a carved corpus`() {
        val cases = SolverCorpus.generate(count = 20)
        val solved = cases.count { LogicalSolver().solve(it.givens).solved }
        assertThat(solved.toDouble() / cases.size).isAtLeast(0.5)
    }

    @Test fun `steps are recorded cheapest-first when multiple techniques could fire`() {
        val result = LogicalSolver().solve(SolverCorpus.EASY_GIVENS)
        // an easy puzzle should be carried mostly by singles
        val singleShare = result.steps.count {
            it.technique == TechniqueId.NAKED_SINGLE || it.technique == TechniqueId.HIDDEN_SINGLE
        }.toDouble() / result.steps.size
        assertThat(singleShare).isAtLeast(0.8)
    }
}
