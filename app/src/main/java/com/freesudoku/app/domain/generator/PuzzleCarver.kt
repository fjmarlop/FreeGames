package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.model.Grid
import com.freesudoku.app.domain.solver.SolutionCounter
import kotlin.random.Random

data class CarvedPuzzle(val givens: Grid, val solution: Grid)

/** Removes clues from a complete grid, greedily, keeping the solution unique. */
class PuzzleCarver(
    private val random: Random = Random.Default,
    private val counter: SolutionCounter = SolutionCounter(),
) {
    fun carve(solution: Grid): CarvedPuzzle {
        require(solution.isComplete) { "carve() needs a complete grid" }
        var current = solution
        for (idx in (0 until 81).shuffled(random)) {
            val row = idx / 9
            val col = idx % 9
            if (current.valueAt(row, col) == 0) continue
            val candidate = current.withValue(row, col, 0)
            if (counter.countUpTo(candidate, limit = 2) == 1) {
                current = candidate
            }
        }
        return CarvedPuzzle(givens = current, solution = solution)
    }
}
