package com.freesudoku.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.freesudoku.app.data.db.entity.CurrentGameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrentGameDao {

    @Query("SELECT * FROM current_game WHERE id = 0")
    fun observe(): Flow<CurrentGameEntity?>

    @Query("SELECT * FROM current_game WHERE id = 0")
    suspend fun get(): CurrentGameEntity?

    @Upsert
    suspend fun upsert(game: CurrentGameEntity)

    @Query("DELETE FROM current_game")
    suspend fun clear()
}
