package com.freesudoku.app.data.repository

import com.freesudoku.app.data.db.dao.CampaignProgressDao
import com.freesudoku.app.data.db.entity.CampaignProgressEntity
import com.freesudoku.app.domain.campaign.CampaignProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampaignRepository @Inject constructor(
    private val dao: CampaignProgressDao,
) {
    fun observeProgress(): Flow<CampaignProgress> =
        dao.observe().map { it?.toDomain() ?: CampaignProgress.START }

    suspend fun ensureInitialized() {
        if (dao.get() == null) {
            dao.upsert(
                CampaignProgressEntity(
                    id = 0,
                    currentPuzzleNumber = 1,
                    highestCompletedNumber = 0,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun currentNumber(): Int {
        ensureInitialized()
        return dao.get()!!.currentPuzzleNumber
    }

    /** Marks [completedNumber] done and moves the pointer to the next puzzle. */
    suspend fun advanceAfterCompleting(completedNumber: Int) {
        val existing = dao.get()
        val highest = max(existing?.highestCompletedNumber ?: 0, completedNumber)
        dao.upsert(
            CampaignProgressEntity(
                id = 0,
                currentPuzzleNumber = completedNumber + 1,
                highestCompletedNumber = highest,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun CampaignProgressEntity.toDomain() = CampaignProgress(
        currentNumber = currentPuzzleNumber,
        highestCompleted = highestCompletedNumber,
    )
}
