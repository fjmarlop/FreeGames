package com.freesudoku.app.domain.campaign

import kotlin.math.ln
import kotlin.math.min

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

    private fun deterministicNoise(n: Int): Double {
        var h = n * 2654435761L
        h = h xor (h ushr 13)
        val unit = ((h and 0xFFFF).toDouble() / 0xFFFF) * 2.0 - 1.0
        return unit * CURVE_NOISE
    }

    companion object {
        const val CURVE_BASE = 4.0
        const val CURVE_GROWTH = 12.0
        const val CURVE_MAX = 88.0
        const val CURVE_NOISE = 3.0
        const val CARVE_TOLERANCE_BASE = 6.0
        const val CARVE_TOLERANCE_STEP = 1.5
        const val CARVE_TOLERANCE_MAX = 20.0
    }
}
