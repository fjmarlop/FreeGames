package com.freesudoku.app.domain.solver.techniques

import com.freesudoku.app.domain.solver.SolverState
import com.freesudoku.app.domain.solver.Technique
import com.freesudoku.app.domain.solver.TechniqueId
import com.freesudoku.app.domain.solver.TechniqueStep

/**
 * Pointing: a digit confined to one row/column within a box is removed from the rest of that line.
 * Claiming: a digit confined to one box within a row/column is removed from the rest of that box.
 */
class LockedCandidates : Technique {
    override val id = TechniqueId.LOCKED_CANDIDATES
    override val cost = 25

    override fun apply(state: SolverState): TechniqueStep? {
        // Pointing: box -> line
        for (box in SolverState.BOXES) {
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                val slots = box.filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                if (slots.size < 2) continue
                if (slots.map { it / 9 }.toSet().size == 1) {
                    val row = slots[0] / 9
                    val elim = (0 until 9).map { row * 9 + it }
                        .filter { it !in slots && state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) return eliminate(state, elim, d)
                }
                if (slots.map { it % 9 }.toSet().size == 1) {
                    val col = slots[0] % 9
                    val elim = (0 until 9).map { it * 9 + col }
                        .filter { it !in slots && state.values[it] == 0 && state.candidates[it] and bit != 0 }
                    if (elim.isNotEmpty()) return eliminate(state, elim, d)
                }
            }
        }
        // Claiming: line -> box (rows are UNITS[0..8], columns UNITS[9..17])
        for (lineIndex in 0 until 18) {
            val line = SolverState.UNITS[lineIndex]
            for (d in 1..9) {
                val bit = 1 shl (d - 1)
                val slots = line.filter { state.values[it] == 0 && state.candidates[it] and bit != 0 }
                if (slots.size < 2) continue
                val boxesOf = slots.map { (it / 9 / 3) * 3 + (it % 9 / 3) }.toSet()
                if (boxesOf.size == 1) {
                    val elim = SolverState.BOXES[boxesOf.first()]
                        .filter { it !in slots && state.values[it] == 0 && state.candidates[it] and bit != 0 }
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
