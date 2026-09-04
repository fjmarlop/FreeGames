package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DifficultyBandTest {

    @Test fun `fromScore maps ranges`() {
        assertThat(DifficultyBand.fromScore(0.0)).isEqualTo(DifficultyBand.PRINCIPIANTE)
        assertThat(DifficultyBand.fromScore(11.9)).isEqualTo(DifficultyBand.PRINCIPIANTE)
        assertThat(DifficultyBand.fromScore(12.0)).isEqualTo(DifficultyBand.FACIL)
        assertThat(DifficultyBand.fromScore(39.9)).isEqualTo(DifficultyBand.MEDIO)
        assertThat(DifficultyBand.fromScore(58.0)).isEqualTo(DifficultyBand.EXPERTO)
        assertThat(DifficultyBand.fromScore(1000.0)).isEqualTo(DifficultyBand.MAESTRO)
    }

    @Test fun `bands are ordered by lowerBound`() {
        val bounds = DifficultyBand.entries.map { it.lowerBound }
        assertThat(bounds).isInOrder()
    }
}
