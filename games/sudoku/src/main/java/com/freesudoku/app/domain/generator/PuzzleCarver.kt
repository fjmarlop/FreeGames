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
    /**
     * Carves down to (at most) [minGivens] clues — fewer removals than the default means a
     * denser, gentler puzzle. Used to keep early campaign puzzles from being over-carved: a
     * grid full-carved to its practical minimum can need only naked/hidden singles and still
     * feel intimidating to a first-time player just from how empty it looks. Defaults to
     * [MIN_GIVENS_UNIQUE], the classic minimum, i.e. carve as far as uniqueness allows.
     */
    fun carve(solution: Grid, minGivens: Int = MIN_GIVENS_UNIQUE): CarvedPuzzle {
        require(solution.isComplete) { "carve() needs a complete grid" }
        require(minGivens in MIN_GIVENS_UNIQUE..80) { "minGivens must be in $MIN_GIVENS_UNIQUE..80" }
        var current = solution
        for (idx in (0 until 81).shuffled(random)) {
            if (current.filledCount <= minGivens) break
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

    companion object {
        /** The lowest clue count any 9x9 Sudoku can have while keeping a unique solution. */
        const val MIN_GIVENS_UNIQUE = 17
    }
}
