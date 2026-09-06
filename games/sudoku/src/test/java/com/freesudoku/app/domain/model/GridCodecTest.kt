package com.freesudoku.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class GridCodecTest {

    private val puzzle81 =
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079"

    @Test fun `decode then encode is identity`() {
        assertThat(GridCodec.encode(GridCodec.decode(puzzle81))).isEqualTo(puzzle81)
    }

    @Test fun `decode accepts dots as empty`() {
        val withDots = puzzle81.replace('0', '.')
        assertThat(GridCodec.decode(withDots)).isEqualTo(GridCodec.decode(puzzle81))
    }

    @Test fun `decode rejects wrong length`() {
        assertThrows(IllegalArgumentException::class.java) { GridCodec.decode("123") }
    }

    @Test fun `decode rejects invalid characters`() {
        assertThrows(IllegalArgumentException::class.java) { GridCodec.decode("x".repeat(81)) }
    }
}
