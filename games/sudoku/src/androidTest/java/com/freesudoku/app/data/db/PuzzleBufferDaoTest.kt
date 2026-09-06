package com.freesudoku.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.freesudoku.app.data.db.entity.PuzzleBufferEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PuzzleBufferDaoTest : DaoTestBase() {

    private fun entity(id: String, score: Double) = PuzzleBufferEntity(
        id = id,
        givens = "0".repeat(81),
        solution = "534678912672195348198342567859761423426853791713924856961537284287419635345286179",
        difficultyScore = score,
        band = "FACIL",
        createdAt = 0,
    )

    @Test fun closestTo_and_count_and_delete() = runTest {
        val dao = db.puzzleBufferDao()
        dao.insertAll(listOf(entity("a", 10.0), entity("b", 20.0), entity("c", 50.0)))

        assertThat(dao.countNow()).isEqualTo(3)
        assertThat(dao.closestTo(target = 22.0, tolerance = 5.0)?.id).isEqualTo("b")
        assertThat(dao.closestTo(target = 22.0, tolerance = 1.0)).isNull()

        dao.deleteById("b")
        assertThat(dao.countNow()).isEqualTo(2)
        assertThat(dao.allIds()).containsExactly("a", "c")
    }
}
