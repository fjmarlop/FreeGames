package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DifficultyBandTest {

    @Test fun `fromScore maps each band to its calibrated range`() {
        assertThat(DifficultyBand.fromScore(0.0)).isEqualTo(DifficultyBand.FACIL)
        assertThat(DifficultyBand.fromScore(-5.0)).isEqualTo(DifficultyBand.FACIL) // piso, no lanza
        for (band in DifficultyBand.entries) {
            assertThat(DifficultyBand.fromScore(band.lowerBound)).isEqualTo(band)
            assertThat(DifficultyBand.fromScore(band.lowerBound + 0.01)).isEqualTo(band)
            if (band.ordinal > 0) {
                assertThat(DifficultyBand.fromScore(band.lowerBound - 0.01))
                    .isEqualTo(DifficultyBand.entries[band.ordinal - 1])
            }
        }
        assertThat(DifficultyBand.fromScore(1000.0)).isEqualTo(DifficultyBand.MAESTRO)
    }

    @Test fun `bands are ordered by lowerBound`() {
        val bounds = DifficultyBand.entries.map { it.lowerBound }
        assertThat(bounds).isInOrder()
    }

    @Test fun `there are five bands and none is called PRINCIPIANTE`() {
        assertThat(DifficultyBand.entries).hasSize(5)
        assertThat(DifficultyBand.entries.map { it.name }).containsNoneOf("PRINCIPIANTE", "")
    }

    @Test fun `parseOrFloor falls back to FACIL for an unknown or removed name`() {
        assertThat(DifficultyBand.parseOrFloor("PRINCIPIANTE")).isEqualTo(DifficultyBand.FACIL)
        assertThat(DifficultyBand.parseOrFloor("MEDIO")).isEqualTo(DifficultyBand.MEDIO)
    }
}
