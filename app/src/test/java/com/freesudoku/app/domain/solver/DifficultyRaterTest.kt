package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.DifficultyBand
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DifficultyRaterTest {

    private val rater = DifficultyRater()

    @Test fun `score is finite, non-negative, and band-consistent`() {
        for (case in SolverCorpus.generate(count = 15)) {
            val r = rater.rate(case.givens)
            assertThat(r.score).isAtLeast(0.0)
            assertThat(r.score.isFinite()).isTrue()
            assertThat(r.band).isEqualTo(DifficultyBand.fromScore(r.score))
        }
    }

    @Test fun `an easy singles-only puzzle lands in a low band`() {
        val r = rater.rate(SolverCorpus.EASY_GIVENS)
        assertThat(r.band.ordinal).isAtMost(DifficultyBand.MEDIO.ordinal)
        assertThat(r.solvedByLogic).isTrue()
    }

    @Test fun `puzzles needing advanced techniques score higher on average than singles-only ones`() {
        val solver = LogicalSolver()
        val advancedIds = setOf(
            TechniqueId.LOCKED_CANDIDATES, TechniqueId.NAKED_SUBSET,
            TechniqueId.HIDDEN_SUBSET, TechniqueId.X_WING,
        )
        val singlesOnly = mutableListOf<Double>()
        val advanced = mutableListOf<Double>()

        for (case in SolverCorpus.generate(count = 60)) {
            val result = solver.solve(case.givens)
            val score = rater.rate(case.givens).score
            if (result.steps.any { it.technique in advancedIds }) advanced += score else singlesOnly += score
        }

        assertThat(singlesOnly).isNotEmpty()
        assertThat(advanced).isNotEmpty()
        assertThat(advanced.average()).isGreaterThan(singlesOnly.average())
    }
}
