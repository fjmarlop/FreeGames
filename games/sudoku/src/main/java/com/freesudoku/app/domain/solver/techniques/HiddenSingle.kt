package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

/** A digit that can go in only one cell of some row, column or box. */
class HiddenSingle : Technique {
    override val id = TechniqueId.HIDDEN_SINGLE
    override val cost = 15

    override fun apply(state: SolverState): TechniqueStep? {
        for (unit in SolverState.UNITS) {
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                val slots = unit.filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                if (slots.size == 1) {
                    val idx = slots[0]
                    state.place(idx, d)
                    return TechniqueStep(id, placements = listOf(idx to d))
                }
            }
        }
        return null
    }
}
