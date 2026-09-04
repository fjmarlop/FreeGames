package com.freesudoku.app.domain.model

/**
 * Labelled difficulty tiers laid over the continuous difficulty score produced by the rater.
 * [lowerBound] is inclusive; a band runs up to the next band's lower bound.
 */
enum class DifficultyBand(val lowerBound: Double) {
    PRINCIPIANTE(0.0),
    FACIL(12.0),
    MEDIO(25.0),
    DIFICIL(40.0),
    EXPERTO(58.0),
    MAESTRO(78.0);

    companion object {
        fun fromScore(score: Double): DifficultyBand = entries.last { score >= it.lowerBound }
    }
}
