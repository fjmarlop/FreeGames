package com.freesudoku.app.domain.model

/**
 * A generated puzzle. [number] is the campaign index once the puzzle is assigned to the
 * campaign; it is null while the puzzle sits unassigned in the pre-generated buffer.
 */
data class Puzzle(
    val id: String,
    val number: Int?,
    val givens: Grid,
    val solution: Grid,
    val difficultyScore: Double,
    val band: DifficultyBand,
) {
    init {
        require(solution.isComplete) { "solution must be a complete, valid grid" }
        require(givens.filledCount in 17..80) { "givens count ${givens.filledCount} out of sane range" }
    }
}
