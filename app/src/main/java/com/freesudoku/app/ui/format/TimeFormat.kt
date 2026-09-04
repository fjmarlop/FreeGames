package com.freesudoku.app.ui.format

import java.util.concurrent.TimeUnit

/** Formats a duration as `M:SS`, or `H:MM:SS` once it reaches an hour. */
fun formatDuration(millis: Long): String {
    val safe = millis.coerceAtLeast(0)
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(safe)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
