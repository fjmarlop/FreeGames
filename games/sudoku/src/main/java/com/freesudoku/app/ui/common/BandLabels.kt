package com.freesudoku.app.ui.common

import com.freesudoku.app.domain.model.DifficultyBand

fun DifficultyBand.label(): String = when (this) {
    DifficultyBand.FACIL -> "Fácil"
    DifficultyBand.MEDIO -> "Medio"
    DifficultyBand.DIFICIL -> "Difícil"
    DifficultyBand.EXPERTO -> "Experto"
    DifficultyBand.MAESTRO -> "Maestro"
}
