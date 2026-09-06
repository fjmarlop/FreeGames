package com.freesudoku.app.data.repository

import com.freesudoku.app.data.db.dao.CurrentGameDao
import com.freesudoku.app.data.db.entity.CurrentGameEntity
import com.freesudoku.app.data.serialization.GameSnapshotDto
import com.freesudoku.app.domain.model.GameSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/writes the single in-progress game. Write coalescing (debounce) is the ViewModel's job so
 * this stays a trivially testable plain writer.
 */
@Singleton
class GameRepository @Inject constructor(
    private val dao: CurrentGameDao,
) {
    fun observeCurrentGame(): Flow<GameSnapshot?> =
        dao.observe().map { entity -> entity?.let { GameSnapshotDto.fromJsonOrNull(it.snapshotJson) } }

    suspend fun currentGame(): GameSnapshot? =
        dao.get()?.let { GameSnapshotDto.fromJsonOrNull(it.snapshotJson) }

    suspend fun save(snapshot: GameSnapshot) {
        dao.upsert(
            CurrentGameEntity(
                id = 0,
                puzzleNumber = snapshot.puzzle.number ?: -1,
                snapshotJson = GameSnapshotDto.toJson(snapshot),
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun clear() = dao.clear()
}
