package com.freesudoku.app.domain.campaign

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CampaignCurveTest {

    private val curve = CampaignCurve()

    @Test fun `target score trends upward before it saturates`() {
        // averaged in coarse blocks over the pre-saturation range so the +-noise doesn't matter
        val smoothed = (1..120).map { curve.targetScore(it) }.windowed(20, 20) { it.average() }
        assertThat(smoothed).isInOrder()
    }

    @Test fun `later puzzles are harder than earlier ones`() {
        assertThat(curve.targetScore(100)).isGreaterThan(curve.targetScore(1) + 15.0)
    }

    @Test fun `first puzzle sits in the beginner range`() {
        assertThat(curve.targetScore(1)).isLessThan(15.0)
    }

    @Test fun `score saturates at or below the max`() {
        assertThat(curve.targetScore(5000)).isAtMost(CampaignCurve.CURVE_MAX)
    }

    @Test fun `tolerance window widens with attempts and is capped`() {
        assertThat(curve.toleranceWindow(0)).isLessThan(curve.toleranceWindow(5))
        assertThat(curve.toleranceWindow(999)).isEqualTo(CampaignCurve.CARVE_TOLERANCE_MAX)
    }

    @Test fun `same puzzle number always yields the same target`() {
        assertThat(curve.targetScore(37)).isEqualTo(curve.targetScore(37))
    }

    @Test fun `first puzzle gets the densest, most forgiving grid`() {
        assertThat(curve.minGivensFor(1)).isEqualTo(CampaignCurve.GIVENS_START)
    }

    @Test fun `minGivens decreases then floors out for a full carve`() {
        val values = (1..100).map { curve.minGivensFor(it) }
        for (i in 1 until values.size) assertThat(values[i]).isAtMost(values[i - 1])
        assertThat(curve.minGivensFor(100)).isEqualTo(CampaignCurve.GIVENS_FLOOR)
    }

    @Test fun `minGivens never drops below the classic unique-solution floor`() {
        assertThat(curve.minGivensFor(1)).isAtLeast(CampaignCurve.GIVENS_FLOOR)
        assertThat(curve.minGivensFor(10_000)).isAtLeast(CampaignCurve.GIVENS_FLOOR)
    }
}
