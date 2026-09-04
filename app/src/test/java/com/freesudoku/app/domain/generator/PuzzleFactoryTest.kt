package com.freesudoku.app.domain.generator

import com.freesudoku.app.domain.campaign.CampaignCurve
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.solver.SolutionCounter
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

class PuzzleFactoryTest {

    private fun factory(seed: Long) = PuzzleFactory(random = Random(seed), curve = CampaignCurve())

    @Test fun `produces a puzzle with a unique solution and matching solution grid`() {
        val puzzle = factory(1).generateForTarget(targetScore = 20.0)
        assertThat(SolutionCounter().countUpTo(puzzle.givens, limit = 2)).isEqualTo(1)
        assertThat(SolutionCounter().solve(puzzle.givens)).isEqualTo(puzzle.solution)
    }

    @Test fun `band reflects score`() {
        val puzzle = factory(2).generateForTarget(targetScore = 20.0)
        assertThat(puzzle.band).isEqualTo(DifficultyBand.fromScore(puzzle.difficultyScore))
    }

    @Test fun `higher target yields higher average score`() {
        val easy = (1..3).map { factory(it.toLong()).generateForTarget(10.0).difficultyScore }.average()
        val hard = (1..3).map { factory(it.toLong()).generateForTarget(55.0).difficultyScore }.average()
        assertThat(hard).isGreaterThan(easy)
    }

    @Test fun `generateForCampaign assigns the puzzle number`() {
        assertThat(factory(3).generateForCampaign(number = 12).number).isEqualTo(12)
    }

    @Test fun `the first campaign puzzle is dense and lands in a beginner band`() {
        for (seed in 1L..5L) {
            val puzzle = factory(seed).generateForCampaign(number = 1)
            assertThat(puzzle.givens.filledCount).isAtLeast(CampaignCurve.GIVENS_START)
            assertThat(puzzle.band.ordinal).isAtMost(DifficultyBand.FACIL.ordinal)
        }
    }

    @Test fun `campaign puzzles get denser than a plain target-only carve at the same score`() {
        val campaignPuzzle = factory(7).generateForCampaign(number = 1)
        val plainPuzzle = factory(7).generateForTarget(targetScore = campaignPuzzle.difficultyScore)
        assertThat(campaignPuzzle.givens.filledCount).isGreaterThan(plainPuzzle.givens.filledCount)
    }

    @Test fun `id is non-empty`() {
        assertThat(factory(4).generateForTarget(15.0).id).isNotEmpty()
    }
}
