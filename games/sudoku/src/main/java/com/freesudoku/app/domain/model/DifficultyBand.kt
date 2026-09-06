package com.freesudoku.app.domain.model

/**
 * Labelled difficulty tiers laid over the continuous difficulty score produced by the rater.
 * [lowerBound] is inclusive; a band runs up to the next band's lower bound.
 */
enum class DifficultyBand(val lowerBound: Double) {
    // Cut points calibrated against the sudoku-exchange puzzle bank (see RaterCalibrationTest).
    PRINCIPIANTE(0.0),
    FACIL(9.0),
    MEDIO(23.0),
    DIFICIL(40.0),
    EXPERTO(49.0),
    MAESTRO(80.0);

    companion object {
        fun fromScore(score: Double): DifficultyBand = entries.last { score >= it.lowerBound }
    }
}
