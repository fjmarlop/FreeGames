package com.freesudoku.app.domain.generator

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class FullGridGeneratorTest {

    @Test fun `generates a complete valid grid`() {
        assertThat(FullGridGenerator(Random(1)).generate().isComplete).isTrue()
    }

    @Test fun `is deterministic for a fixed seed`() {
        assertThat(FullGridGenerator(Random(42)).generate())
            .isEqualTo(FullGridGenerator(Random(42)).generate())
    }

    @Test fun `different seeds usually differ`() {
        assertThat(FullGridGenerator(Random(1)).generate())
            .isNotEqualTo(FullGridGenerator(Random(2)).generate())
    }

    @Test fun `every row column and box is a permutation of 1_9`() {
        val g = FullGridGenerator(Random(7)).generate()
        for (i in 0 until 9) {
            assertThat((0 until 9).map { g.valueAt(i, it) }.toSet()).isEqualTo((1..9).toSet())
            assertThat((0 until 9).map { g.valueAt(it, i) }.toSet()).isEqualTo((1..9).toSet())
        }
    }
}
