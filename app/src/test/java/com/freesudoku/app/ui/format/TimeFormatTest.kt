package com.freesudoku.app.ui.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeFormatTest {

    @Test fun `formats sub-minute`() {
        assertThat(formatDuration(0)).isEqualTo("0:00")
        assertThat(formatDuration(5_000)).isEqualTo("0:05")
    }

    @Test fun `formats minutes and seconds`() {
        assertThat(formatDuration(65_000)).isEqualTo("1:05")
        assertThat(formatDuration(600_000)).isEqualTo("10:00")
    }

    @Test fun `formats hours`() {
        assertThat(formatDuration(3_725_000)).isEqualTo("1:02:05")
    }

    @Test fun `negative clamps to zero`() {
        assertThat(formatDuration(-100)).isEqualTo("0:00")
    }
}
