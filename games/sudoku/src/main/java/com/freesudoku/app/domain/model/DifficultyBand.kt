package com.freesudoku.app.domain.model

/**
 * Labelled difficulty tiers laid over the continuous difficulty score produced by the rater.
 * [lowerBound] is inclusive; a band runs up to the next band's lower bound.
 */
enum class DifficultyBand(val lowerBound: Double) {
    // Cut points calibrated against the sudoku-exchange puzzle bank (see RaterCalibrationTest).
    // FACIL is the floor band: it absorbs everything below the MEDIO cut, so fromScore never
    // runs out of entries for a low/negative score.
    FACIL(0.0),
    MEDIO(23.0),
    DIFICIL(40.0),
    EXPERTO(49.0),
    MAESTRO(80.0);

    companion object {
        fun fromScore(score: Double): DifficultyBand =
            entries.lastOrNull { score >= it.lowerBound } ?: entries.first()

        /** Tolerant parse for persisted strings — an unknown/removed name falls back to the floor band. */
        fun parseOrFloor(name: String): DifficultyBand =
            entries.firstOrNull { it.name == name } ?: FACIL

        /**
         * Bands a player can pick for "Partida rápida". MAESTRO is excluded: the solver only
         * reaches X-Wing, so the generator cannot reliably produce a score in `[80, ∞)`
         * (`CampaignCurve.CURVE_MAX = 56` already reflects the same ceiling) — offering it would
         * be misleading.
         */
        val QUICK_PLAY_SELECTABLE: List<DifficultyBand> = listOf(FACIL, MEDIO, DIFICIL, EXPERTO)
    }
}
