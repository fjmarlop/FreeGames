package com.freesudoku.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freesudoku.app.data.db.entity.PuzzleBufferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PuzzleBufferDao {

    @Query("SELECT COUNT(*) FROM puzzle_buffer")
    fun count(): Flow<Int>

    @Query("SELECT COUNT(*) FROM puzzle_buffer")
    suspend fun countNow(): Int

    @Query(
        """
        SELECT * FROM puzzle_buffer
        WHERE ABS(difficultyScore - :target) <= :tolerance
        ORDER BY ABS(difficultyScore - :target) ASC
        LIMIT 1
        """
    )
    suspend fun closestTo(target: Double, tolerance: Double): PuzzleBufferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(puzzles: List<PuzzleBufferEntity>)

    @Query("DELETE FROM puzzle_buffer WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT id FROM puzzle_buffer")
    suspend fun allIds(): List<String>
}
