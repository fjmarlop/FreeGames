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

/** The solve features a score is derived from. Cheap to extract once, then re-score for any weights. */
data class RatingFeatures(
    val hardestCost: Int,
    val weightedApplications: Double,
    val givenCount: Int,
    val solvedByLogic: Boolean,
    val hardestTechnique: TechniqueId?,
)

/** Tunable coefficients — calibrated against the sudoku-exchange puzzle bank (see RaterCalibrationTest). */
data class RatingWeights(
    val wHardest: Double,
    val wFrequency: Double,
    val wClues: Double,
    val cluePivot: Int,
    val unsolvedPenalty: Double,
) {
    companion object {
        /**
         * Calibrated 2026-09-04 against 510 rated puzzles from the sudoku-exchange puzzle bank
         * (easy/medium/hard/diabolical, human ratings 1.2..9.2). Random search over weights +
         * band cut points; result: Spearman rho 0.86 vs human ratings, and every puzzle's
         * predicted band is within one of its rating-derived target. See RaterCalibrationTest.
         */
        val DEFAULT = RatingWeights(
            wHardest = 0.53,
            wFrequency = 0.05,
            wClues = 0.66,
            cluePivot = 34,
            unsolvedPenalty = 39.0,
        )
    }
}

/**
 * Continuous difficulty score (~0..100) from simulating a human solve:
 * weight of the hardest technique needed + how often techniques were used + how sparse the givens are,
 * plus a penalty when the MVP technique set cannot finish the puzzle.
 */
class DifficultyRater(
    private val solver: LogicalSolver = LogicalSolver(),
    private val weights: RatingWeights = RatingWeights.DEFAULT,
) {

    fun features(givens: Grid): RatingFeatures {
        val result = solver.solve(givens)
        return RatingFeatures(
            hardestCost = result.hardestTechnique?.let { TECHNIQUE_COST.getValue(it) } ?: 0,
            weightedApplications = result.techniqueCounts.entries.sumOf { (id, n) ->
                n * (TECHNIQUE_COST.getValue(id) / 100.0)
            },
            givenCount = givens.filledCount,
            solvedByLogic = result.solved,
            hardestTechnique = result.hardestTechnique,
        )
    }

    fun scoreOf(features: RatingFeatures, w: RatingWeights = weights): Double {
        val clueComponent = max(0.0, (w.cluePivot - features.givenCount).toDouble())
        var score = w.wHardest * features.hardestCost +
            w.wFrequency * features.weightedApplications +
            w.wClues * clueComponent
        if (!features.solvedByLogic) score += w.unsolvedPenalty
        return score
    }

    fun rate(givens: Grid): Rating {
        val f = features(givens)
        val score = scoreOf(f)
        return Rating(
            score = score,
            band = DifficultyBand.fromScore(score),
            solvedByLogic = f.solvedByLogic,
            hardestTechnique = f.hardestTechnique,
        )
    }
}
