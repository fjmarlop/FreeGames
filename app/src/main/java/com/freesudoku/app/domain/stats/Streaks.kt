package com.freesudoku.app.domain.stats

import java.util.concurrent.TimeUnit

data class Streaks(val current: Int, val longest: Int)

/**
 * Day-based streaks from completion timestamps (epoch millis, any order).
 * [todayEpochDay] and [zoneOffsetMillis] make this a pure function testable without a clock.
 *
 * - current: consecutive days ending today or yesterday.
 * - longest: the longest consecutive-day run anywhere in the history.
 */
fun computeStreaks(
    timestampsMillis: List<Long>,
    todayEpochDay: Long,
    zoneOffsetMillis: Int = 0,
): Streaks {
    if (timestampsMillis.isEmpty()) return Streaks(0, 0)

    val days = timestampsMillis
        .map { TimeUnit.MILLISECONDS.toDays(it + zoneOffsetMillis) }
        .toSortedSet()
        .toList()

    var longest = 1
    var run = 1
    for (i in 1 until days.size) {
        run = if (days[i] == days[i - 1] + 1) run + 1 else 1
        if (run > longest) longest = run
    }

    val mostRecent = days.last()
    val current = if (mostRecent < todayEpochDay - 1) {
        0
    } else {
        var c = 1
        var expected = mostRecent - 1
        var idx = days.size - 2
        while (idx >= 0 && days[idx] == expected) {
            c++
            expected--
            idx--
        }
        c
    }
    return Streaks(current = current, longest = longest)
}
