package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

/**
 * A digit whose candidates in two rows sit in exactly the same two columns (or vice versa)
 * is eliminated from the rest of those columns (rows).
 */
class XWing : Technique {
    override val id = TechniqueId.X_WING
    override val cost = 65

    override fun apply(state: SolverState): TechniqueStep? {
        for (d in 1..9) {
            val bit = 1 shl (d - 1)

            val rowSlots = Array(9) { r ->
                (0 until 9).filter { c -> state.values[r * 9 + c] == 0 && state.candidates[r * 9 + c] and bit != 0 }
            }
            for (r1 in 0 until 9) for (r2 in r1 + 1 until 9) {
                if (rowSlots[r1].size == 2 && rowSlots[r1] == rowSlots[r2]) {
                    val (c1, c2) = rowSlots[r1]
                    val elim = (0 until 9).filter { it != r1 && it != r2 }
                        .flatMap { r -> listOf(r * 9 + c1, r * 9 + c2) }
                        .filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) return eliminate(state, elim, d)
                }
            }

            val colSlots = Array(9) { c ->
                (0 until 9).filter { r -> state.values[r * 9 + c] == 0 && state.candidates[r * 9 + c] and bit != 0 }
            }
            for (c1 in 0 until 9) for (c2 in c1 + 1 until 9) {
                if (colSlots[c1].size == 2 && colSlots[c1] == colSlots[c2]) {
                    val (r1, r2) = colSlots[c1]
                    val elim = (0 until 9).filter { it != c1 && it != c2 }
                        .flatMap { c -> listOf(r1 * 9 + c, r2 * 9 + c) }
                        .filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) return eliminate(state, elim, d)
                }
            }
        }
        return null
    }

    private fun eliminate(state: SolverState, cells: List<Int>, d: Int): TechniqueStep {
        cells.forEach { state.removeCandidate(it, d) }
        return TechniqueStep(id, eliminations = cells.map { it to d })
    }
}
