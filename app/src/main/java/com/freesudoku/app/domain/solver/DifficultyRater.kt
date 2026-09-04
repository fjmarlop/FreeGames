package com.freesudoku.app.domain.solver

import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.Grid
import kotlin.math.max

data class Rating(
    val score: Double,
    val band: DifficultyBand,
    val solvedByLogic: Boolean,
    val hardestTechnique: TechniqueId?,
)

/**
 * Continuous difficulty score (~0..100) from simulating a human solve:
 * weight of the hardest technique needed + how often techniques were used + how sparse the givens are.
 */
class DifficultyRater(private val solver: LogicalSolver = LogicalSolver()) {

    fun rate(givens: Grid): Rating {
        val result = solver.solve(givens)
        val hardestCost = result.hardestTechnique?.let { TECHNIQUE_COST.getValue(it) } ?: 0
        val freqComponent = result.techniqueCounts.entries.sumOf { (id, n) ->
            n * (TECHNIQUE_COST.getValue(id) / 100.0)
        }
        val clueComponent = max(0.0, (RATER_CLUE_PIVOT - givens.filledCount).toDouble())

        var score = W_HARDEST * hardestCost + W_FREQ * freqComponent + W_CLUES * clueComponent
        if (!result.solved) score += UNSOLVED_PENALTY

        return Rating(
            score = score,
            band = DifficultyBand.fromScore(score),
            solvedByLogic = result.solved,
            hardestTechnique = result.hardestTechnique,
        )
    }

    companion object {
        const val W_HARDEST = 0.60
        const val W_FREQ = 0.10
        const val W_CLUES = 0.80
        const val RATER_CLUE_PIVOT = 32
        const val UNSOLVED_PENALTY = 25.0
    }
}
