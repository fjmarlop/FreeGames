package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.solver.SolutionCounter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class PuzzleCarverTest {

    @Test fun `carved puzzle keeps a unique solution`() {
        val full = FullGridGenerator(Random(3)).generate()
        val carved = PuzzleCarver(Random(3)).carve(full)
        assertThat(SolutionCounter().countUpTo(carved.givens, limit = 2)).isEqualTo(1)
        assertThat(carved.solution).isEqualTo(full)
    }

    @Test fun `carved puzzle has fewer clues than a full grid but stays sane`() {
        val full = FullGridGenerator(Random(5)).generate()
        val carved = PuzzleCarver(Random(5)).carve(full)
        assertThat(carved.givens.filledCount).isLessThan(81)
        assertThat(carved.givens.filledCount).isAtLeast(17)
    }

    @Test fun `deterministic for a fixed seed`() {
        val full = FullGridGenerator(Random(9)).generate()
        val a = PuzzleCarver(Random(11)).carve(full)
        val b = PuzzleCarver(Random(11)).carve(full)
        assertThat(a.givens).isEqualTo(b.givens)
    }

    @Test fun `respects a higher minGivens floor and stays unique`() {
        val full = FullGridGenerator(Random(21)).generate()
        val carved = PuzzleCarver(Random(23)).carve(full, minGivens = 40)
        assertThat(carved.givens.filledCount).isAtLeast(40)
        assertThat(SolutionCounter().countUpTo(carved.givens, limit = 2)).isEqualTo(1)
    }

    @Test fun `a dense minGivens carve still yields fewer clues than the full grid`() {
        val full = FullGridGenerator(Random(29)).generate()
        val carved = PuzzleCarver(Random(31)).carve(full, minGivens = 40)
        assertThat(carved.givens.filledCount).isLessThan(81)
    }

    @Test fun `rejects a minGivens below the classic unique minimum`() {
        val full = FullGridGenerator(Random(37)).generate()
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            PuzzleCarver(Random(1)).carve(full, minGivens = 10)
        }
    }

    @Test fun `every given matches the solution`() {
        val full = FullGridGenerator(Random(13)).generate()
        val carved = PuzzleCarver(Random(17)).carve(full)
        for (i in 0 until 81) {
            val g = carved.givens.cells[i]
            if (g != 0) assertThat(g).isEqualTo(full.cells[i])
        }
    }
}
