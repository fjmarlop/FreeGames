package com.freesudoku.app.data.repository

import com.freesudoku.app.data.db.dao.PuzzleBufferDao
import com.freesudoku.app.domain.campaign.CampaignCurve
import com.freesudoku.app.domain.generator.PuzzleFactory
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.GridCodec
import com.freesudoku.app.domain.model.Puzzle
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PuzzleRepositoryTest {

    private val quickPuzzle = Puzzle(
        id = "q", number = null,
        givens = GridCodec.decode("530070000600195000098000060800060003400803001700020006060000280000419005000080079"),
        solution = GridCodec.decode("534678912672195348198342567859761423426853791713924856961537284287419635345286179"),
        difficultyScore = 44.0, band = DifficultyBand.DIFICIL,
    )

    private fun repository(factory: PuzzleFactory, bufferDao: PuzzleBufferDao = mockk(relaxed = true)) =
        PuzzleRepository(bufferDao, factory, CampaignCurve(), CoroutineScope(Dispatchers.Unconfined))

    @Test fun `puzzleForBand targets a representative score and skips the buffer`() = runTest {
        val factory = mockk<PuzzleFactory>()
        val target = slot<Double>()
        every { factory.generateForTarget(capture(target), any()) } returns quickPuzzle
        val bufferDao = mockk<PuzzleBufferDao>(relaxed = true)

        val result = repository(factory, bufferDao).puzzleForBand(DifficultyBand.DIFICIL)

        assertThat(result).isEqualTo(quickPuzzle)
        assertThat(result.number).isNull()
        assertThat(target.captured).isAtLeast(DifficultyBand.DIFICIL.lowerBound)
        assertThat(target.captured).isLessThan(DifficultyBand.EXPERTO.lowerBound)
        coVerify(exactly = 0) { bufferDao.closestTo(any(), any()) }
    }

    @Test fun `puzzleForBand picks a different target per band`() = runTest {
        val factory = mockk<PuzzleFactory>()
        val targets = slot<Double>()
        every { factory.generateForTarget(capture(targets), any()) } returns quickPuzzle
        val repo = repository(factory)

        repo.puzzleForBand(DifficultyBand.FACIL)
        val facilTarget = targets.captured
        repo.puzzleForBand(DifficultyBand.EXPERTO)
        val expertoTarget = targets.captured

        assertThat(facilTarget).isLessThan(expertoTarget)
        assertThat(facilTarget).isAtLeast(DifficultyBand.FACIL.lowerBound)
        assertThat(facilTarget).isLessThan(DifficultyBand.MEDIO.lowerBound)
        assertThat(expertoTarget).isAtLeast(DifficultyBand.EXPERTO.lowerBound)
    }
}
