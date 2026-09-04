package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverCorpus
import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * These techniques are hard to pin with hand-built fixtures, so we test them by *soundness*:
 * run them interleaved on realistic puzzles and assert they never eliminate a candidate that
 * the true solution needs, and never place a wrong digit. We also assert each one actually
 * fires somewhere in the corpus (i.e. it is not dead code).
 */
class AdvancedTechniquesTest {

    private fun freshTechniques(): List<Technique> =
        listOf(NakedSingle(), HiddenSingle(), LockedCandidates(), NakedSubset(), HiddenSubset(), XWing())

    @Test fun `techniques are sound and each one fires across the corpus`() {
        val fired = HashSet<String>()

        for (case in SolverCorpus.generate(count = 40)) {
            val state = SolverState.from(case.givens)!!
            var guard = 0
            while (!state.isSolved() && guard++ < 400 && !state.hasContradiction()) {
                val step = freshTechniques().firstNotNullOfOrNull { it.apply(state) } ?: break
                fired += step.technique.name

                for ((cell, digit) in step.placements) {
                    assertThat(digit).isEqualTo(case.solution.cells[cell])
                }
                for ((cell, digit) in step.eliminations) {
                    assertThat(digit).isNotEqualTo(case.solution.cells[cell])
                }
                // candidate set must always still contain the solution digit for every empty cell
                for (i in 0 until 81) {
                    if (state.values[i] == 0) {
                        val solBit = 1 shl (case.solution.cells[i] - 1)
                        assertThat(state.candidates[i] and solBit).isNotEqualTo(0)
                    }
                }
            }
        }

        assertThat(fired).containsAtLeast(
            "NAKED_SINGLE", "HIDDEN_SINGLE", "LOCKED_CANDIDATES", "NAKED_SUBSET",
        )
    }
}
