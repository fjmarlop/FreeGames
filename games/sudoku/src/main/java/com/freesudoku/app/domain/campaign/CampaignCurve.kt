package com.freesudoku.app.domain.campaign

import kotlin.math.ln
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Maps a campaign puzzle number to a target difficulty score: monotonically rising, saturating
 * below [CURVE_MAX], with a small deterministic wobble so consecutive puzzles are not identical.
 */
class CampaignCurve {

    fun targetScore(puzzleNumber: Int): Double {
        require(puzzleNumber >= 1) { "puzzle number is 1-based" }
        val base = CURVE_BASE + CURVE_GROWTH * ln(1.0 + puzzleNumber)
        return (base + deterministicNoise(puzzleNumber)).coerceIn(0.0, CURVE_MAX)
    }

    fun toleranceWindow(attempt: Int): Double =
        min(CARVE_TOLERANCE_MAX, CARVE_TOLERANCE_BASE + CARVE_TOLERANCE_STEP * attempt)

    /**
     * Minimum clue count for the carver at this puzzle number: a dense, gentle grid for the
     * first puzzles (mostly filled in, only a handful of obvious deductions) that thins out to
     * a normal full carve by ~puzzle 15. Without this, even puzzle #1 was carved to its practical
     * minimum (~24-30 clues) — technically singles-only, but a wall of empty cells is a rough
     * first impression for a brand-new player.
     */
    fun minGivensFor(puzzleNumber: Int): Int {
        require(puzzleNumber >= 1) { "puzzle number is 1-based" }
        val value = GIVENS_START - GIVENS_DECAY * ln(puzzleNumber.toDouble())
        return value.roundToInt().coerceIn(GIVENS_FLOOR, GIVENS_START)
    }

    private fun deterministicNoise(n: Int): Double {
        var h = n * 2654435761L
        h = h xor (h ushr 13)
        val unit = ((h and 0xFFFF).toDouble() / 0xFFFF) * 2.0 - 1.0
        return unit * CURVE_NOISE
    }

    companion object {
        // Calibrated to the score range the generator actually produces (see RaterCalibrationTest
        // and the generator's bimodal carve distribution): a gentle ramp through FACIL ->
        // MEDIO over the first ~40 puzzles, then up into DIFICIL/EXPERTO, saturating near 56.
        const val CURVE_BASE = 9.0
        const val CURVE_GROWTH = 7.0
        const val CURVE_MAX = 56.0
        const val CURVE_NOISE = 3.0
        const val CARVE_TOLERANCE_BASE = 6.0
        const val CARVE_TOLERANCE_STEP = 1.5
        const val CARVE_TOLERANCE_MAX = 20.0
        const val GIVENS_START = 40
        const val GIVENS_DECAY = 6.0
        const val GIVENS_FLOOR = 24
    }
}
