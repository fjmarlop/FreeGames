package com.freesudoku.app.ui.game

import com.freesudoku.app.domain.model.GameStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameScreenTest {

    @Test fun `keeps the screen on only while the puzzle is in progress`() {
        assertThat(keepScreenOnFor(GameStatus.IN_PROGRESS)).isTrue()
        assertThat(keepScreenOnFor(GameStatus.COMPLETED)).isFalse()
        assertThat(keepScreenOnFor(GameStatus.FAILED)).isFalse()
    }
}
