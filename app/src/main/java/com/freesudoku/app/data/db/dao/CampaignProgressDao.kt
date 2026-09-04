package com.freesudoku.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.freesudoku.app.data.db.entity.CampaignProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignProgressDao {

    @Query("SELECT * FROM campaign_progress WHERE id = 0")
    fun observe(): Flow<CampaignProgressEntity?>

    @Query("SELECT * FROM campaign_progress WHERE id = 0")
    suspend fun get(): CampaignProgressEntity?

    @Upsert
    suspend fun upsert(progress: CampaignProgressEntity)
}
