package com.freesudoku.app.data.repository

import com.freesudoku.app.data.db.dao.PuzzleBufferDao
import com.freesudoku.app.data.mapper.toBufferEntity
import com.freesudoku.app.data.mapper.toDomain
import com.freesudoku.app.di.ApplicationScope
import com.freesudoku.app.domain.campaign.CampaignCurve
import com.freesudoku.app.domain.generator.PuzzleFactory
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.domain.model.Puzzle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serves the next campaign puzzle from the pre-generated buffer when possible, generating on demand
 * otherwise, and keeps the buffer topped up in the background.
 */
@Singleton
class PuzzleRepository @Inject constructor(
    private val bufferDao: PuzzleBufferDao,
    private val factory: PuzzleFactory,
    private val curve: CampaignCurve,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val refillMutex = Mutex()

    suspend fun puzzleForCampaign(number: Int): Puzzle {
        val target = curve.targetScore(number)
        val buffered = bufferDao.closestTo(target, curve.toleranceWindow(0))
        val puzzle = if (buffered != null) {
            bufferDao.deleteById(buffered.id)
            buffered.toDomain().copy(number = number)
        } else {
            factory.generateForCampaign(number)
        }
        maybeRefill(number)
        return puzzle
    }

    /**
     * A single puzzle for "Partida rápida": targets a representative score inside [band], no
     * campaign number, no buffer involved (a one-off action, not the continuous campaign stream).
     */
    suspend fun puzzleForBand(band: DifficultyBand): Puzzle =
        factory.generateForTarget(QUICK_PLAY_TARGET_SCORE.getValue(band))

    /** Blocking refill — used by tests and first-run warm-up. */
    suspend fun refillNow(fromNumber: Int) = refillMutex.withLock {
        val missing = MIN_BUFFER - bufferDao.countNow()
        if (missing <= 0) return@withLock
        val fresh = (1..missing).map { factory.generateForCampaign(fromNumber + it).toBufferEntity() }
        bufferDao.insertAll(fresh)
    }

    private fun maybeRefill(fromNumber: Int) {
        scope.launch { refillNow(fromNumber) }
    }

    companion object {
        const val MIN_BUFFER = 4

        /** Representative score per band for "Partida rápida" — comfortably inside each range. */
        private val QUICK_PLAY_TARGET_SCORE: Map<DifficultyBand, Double> = mapOf(
            DifficultyBand.FACIL to 14.0,
            DifficultyBand.MEDIO to 31.0,
            DifficultyBand.DIFICIL to 44.0,
            DifficultyBand.EXPERTO to 52.0,
        )
    }
}
