package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

/** A cell with exactly one remaining candidate. */
class NakedSingle : Technique {
    override val id = TechniqueId.NAKED_SINGLE
    override val cost = 10

    override fun apply(state: SolverState): TechniqueStep? {
        for (i in 0 until 81) {
            if (state.values[i] == 0 && state.candidateCount(i) == 1) {
                val d = state.candidateList(i).first()
                state.place(i, d)
                return TechniqueStep(id, placements = listOf(i to d))
            }
        }
        return null
    }
}
