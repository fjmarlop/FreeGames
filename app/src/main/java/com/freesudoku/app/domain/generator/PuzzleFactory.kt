package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.campaign.CampaignCurve
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.Puzzle
import com.freesudoku.app.domain.solver.DifficultyRater
import kotlin.math.abs
import kotlin.random.Random

/**
 * Generates puzzles aimed at a target difficulty score: build a full grid, carve it, rate it,
 * keep the closest match within the (widening) tolerance window, capped at [maxAttempts].
 */
class PuzzleFactory(
    private val random: Random = Random.Default,
    private val curve: CampaignCurve = CampaignCurve(),
    private val fullGridGenerator: FullGridGenerator = FullGridGenerator(random),
    private val carver: PuzzleCarver = PuzzleCarver(random),
    private val rater: DifficultyRater = DifficultyRater(),
    private val maxAttempts: Int = CARVE_MAX_ATTEMPTS,
) {
    fun generateForCampaign(number: Int): Puzzle =
        generateForTarget(curve.targetScore(number)).copy(number = number)

    fun generateForTarget(targetScore: Double): Puzzle {
        var best: Puzzle? = null
        var bestGap = Double.MAX_VALUE

        for (attempt in 0 until maxAttempts) {
            val solution = fullGridGenerator.generate()
            val carved = carver.carve(solution)
            val rating = rater.rate(carved.givens)
            val gap = abs(rating.score - targetScore)

            if (gap < bestGap) {
                bestGap = gap
                best = Puzzle(
                    id = newId(),
                    number = null,
                    givens = carved.givens,
                    solution = carved.solution,
                    difficultyScore = rating.score,
                    band = DifficultyBand.fromScore(rating.score),
                )
            }
            if (gap <= curve.toleranceWindow(attempt)) return best!!
        }
        return best!!
    }

    private fun newId(): String = buildString {
        repeat(16) { append(ID_ALPHABET[random.nextInt(ID_ALPHABET.length)]) }
    }

    companion object {
        const val CARVE_MAX_ATTEMPTS = 40
        private const val ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    }
}
