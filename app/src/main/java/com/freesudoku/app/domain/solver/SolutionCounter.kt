package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.Grid

/** Counts solutions up to a bound and can return the unique completion of a puzzle. */
class SolutionCounter {

    /** Returns min([limit], number of solutions). 0 means the givens are already contradictory. */
    fun countUpTo(grid: Grid, limit: Int): Int {
        val state = SolverState.from(grid) ?: return 0
        return search(state, limit, IntArray(1), null)
    }

    /** Returns the unique completion, or null if there is none or the puzzle is contradictory. */
    fun solve(grid: Grid): Grid? {
        val state = SolverState.from(grid) ?: return null
        val capture = arrayOfNulls<Grid>(1)
        search(state, limit = 1, count = IntArray(1), capture = capture)
        return capture[0]
    }

    private fun search(state: SolverState, limit: Int, count: IntArray, capture: Array<Grid?>?): Int {
        if (state.isSolved()) {
            count[0]++
            if (capture != null && capture[0] == null) capture[0] = state.toGrid()
            return count[0]
        }
        var best = -1
        var bestCount = 10
        for (i in 0 until 81) {
            if (state.values[i] == 0) {
                val c = state.candidateCount(i)
                if (c < bestCount) {
                    bestCount = c
                    best = i
                    if (c <= 1) break
                }
            }
        }
        if (best == -1 || bestCount == 0) return count[0]
        for (d in state.candidateList(best)) {
            val branch = state.copy()
            if (branch.assign(best, d)) {
                search(branch, limit, count, capture)
                if (count[0] >= limit) return count[0]
            }
        }
        return count[0]
    }
}
