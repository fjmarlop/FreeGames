package com.freesudoku.app.domain.campaign

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CampaignCurveTest {

    private val curve = CampaignCurve()

    @Test fun `target score trends upward across the campaign`() {
        val smoothed = (1..300).map { curve.targetScore(it) }.windowed(15, 15) { it.average() }
        assertThat(smoothed).isInOrder()
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
}
