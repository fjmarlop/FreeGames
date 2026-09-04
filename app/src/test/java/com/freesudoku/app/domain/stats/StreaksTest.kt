package com.freesudoku.app.domain.stats

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreaksTest {

    private val today = 20_000L
    private fun day(epochDay: Long) = epochDay * 86_400_000L

    @Test fun `empty history is zero`() {
        assertThat(computeStreaks(emptyList(), today)).isEqualTo(Streaks(0, 0))
    }

    @Test fun `single completion today is a streak of one`() {
        assertThat(computeStreaks(listOf(day(today)), today)).isEqualTo(Streaks(1, 1))
    }

    @Test fun `consecutive days ending today count as current`() {
        val ts = listOf(day(today), day(today - 1), day(today - 2), day(today - 5))
        assertThat(computeStreaks(ts, today)).isEqualTo(Streaks(current = 3, longest = 3))
    }

    @Test fun `streak ending yesterday still counts as current`() {
        val ts = listOf(day(today - 1), day(today - 2))
        assertThat(computeStreaks(ts, today)).isEqualTo(Streaks(current = 2, longest = 2))
    }

    @Test fun `stale history has zero current but keeps longest`() {
        val ts = listOf(day(today - 5), day(today - 6), day(today - 7))
        assertThat(computeStreaks(ts, today)).isEqualTo(Streaks(current = 0, longest = 3))
    }

    @Test fun `multiple completions on the same day collapse to one`() {
        val ts = listOf(day(today), day(today), day(today - 1))
        assertThat(computeStreaks(ts, today)).isEqualTo(Streaks(2, 2))
    }
}
