package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.Grid
import com.freesudoku.app.domain.solver.techniques.HiddenSingle
import com.freesudoku.app.domain.solver.techniques.HiddenSubset
import com.freesudoku.app.domain.solver.techniques.LockedCandidates
import com.freesudoku.app.domain.solver.techniques.NakedSingle
import com.freesudoku.app.domain.solver.techniques.NakedSubset
import com.freesudoku.app.domain.solver.techniques.XWing

data class SolveResult(
    val solved: Boolean,
    val finalGrid: Grid,
    val steps: List<TechniqueStep>,
) {
    val techniqueCounts: Map<TechniqueId, Int> = steps.groupingBy { it.technique }.eachCount()

    val hardestTechnique: TechniqueId? =
        steps.maxByOrNull { TECHNIQUE_COST.getValue(it.technique) }?.technique
}

/** Solves a grid using only human-style techniques, cheapest first, recording every step. */
class LogicalSolver(
    private val techniques: List<Technique> = defaultTechniques(),
) {
    fun solve(grid: Grid): SolveResult {
        val state = SolverState.from(grid) ?: return SolveResult(false, grid, emptyList())
        val steps = mutableListOf<TechniqueStep>()
        var guard = 0
        while (!state.isSolved() && guard++ < GUARD_LIMIT) {
            if (state.hasContradiction()) break
            val step = techniques.firstNotNullOfOrNull { it.apply(state) }
            if (step == null || !step.isProgress) break
            steps += step
        }
        return SolveResult(state.isSolved() && !state.hasContradiction(), state.toGrid(), steps)
    }

    companion object {
        private const val GUARD_LIMIT = 400

        fun defaultTechniques(): List<Technique> = listOf(
            NakedSingle(),
            HiddenSingle(),
            LockedCandidates(),
            NakedSubset(),
            HiddenSubset(),
            XWing(),
        ).sortedBy { it.cost }
    }
}
