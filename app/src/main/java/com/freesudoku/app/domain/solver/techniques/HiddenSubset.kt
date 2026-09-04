package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

/** N digits confined to exactly N cells of a unit -> all other candidates leave those cells. */
class HiddenSubset : Technique {
    override val id = TechniqueId.HIDDEN_SUBSET
    override val cost = 48

    override fun apply(state: SolverState): TechniqueStep? {
        for (size in 2..3) {
            for (unit in SolverState.UNITS) {
                val digitSlots = (1..9).associateWith { d ->
                    val bit = 1 shl (d - 1)
                    unit.filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                }.filterValues { it.size in 2..size }
                val digits = digitSlots.keys.toList()
                for (combo in combinations(digits, size)) {
                    val cells = combo.flatMap { digitSlots.getValue(it) }.toSet()
                    if (cells.size != size) continue
                    val keepMask = combo.fold(0) { acc, d -> acc or (1 shl (d - 1)) }
                    val elims = buildList {
                        for (c in cells) for (d in 1..9) {
                            val bit = 1 shl (d - 1)
                            if (keepMask and bit == 0 && state.candidates[c] and bit != 0) add(c to d)
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
