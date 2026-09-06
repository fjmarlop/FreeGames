package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

/** N cells in a unit whose candidates union to exactly N digits -> those digits leave the other cells. */
class NakedSubset : Technique {
    override val id = TechniqueId.NAKED_SUBSET
    override val cost = 40

    override fun apply(state: SolverState): TechniqueStep? {
        for (size in 2..3) {
            for (unit in SolverState.UNITS) {
                val open = unit.filter { state.values[it] == 0 && state.candidateCount(it) in 2..size }
                for (combo in combinations(open, size)) {
                    val union = combo.fold(0) { acc, i -> acc or state.candidates[i] }
                    if (Integer.bitCount(union) != size) continue
                    val elims = buildList {
                        for (t in unit) {
                            if (t in combo || state.values[t] != 0) continue
                            for (d in 1..9) {
                                val bit = 1 shl (d - 1)
                                if (union and bit != 0 && state.candidates[t] and bit != 0) add(t to d)
                            }
                        }
                    }
                    if (elims.isNotEmpty()) {
                        elims.forEach { (c, d) -> state.removeCandidate(c, d) }
                        return TechniqueStep(id, eliminations = elims)
                    }
                }
            }
        }
        return null
    }
}
