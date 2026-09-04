package com.freesudoku.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freesudoku.app.data.db.entity.CampaignProgressEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampaignProgressDaoTest : DaoTestBase() {

    @Test fun upsert_and_replace() = runTest {
        val dao = db.campaignProgressDao()
        dao.upsert(CampaignProgressEntity(0, currentPuzzleNumber = 1, highestCompletedNumber = 0, updatedAt = 0))
        assertThat(dao.get()?.currentPuzzleNumber).isEqualTo(1)

        dao.upsert(CampaignProgressEntity(0, currentPuzzleNumber = 5, highestCompletedNumber = 4, updatedAt = 1))
        assertThat(dao.get()?.currentPuzzleNumber).isEqualTo(5)
        assertThat(dao.get()?.highestCompletedNumber).isEqualTo(4)
    }
}
