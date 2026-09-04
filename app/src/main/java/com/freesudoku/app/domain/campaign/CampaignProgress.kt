package com.freesudoku.app.domain.campaign

data class CampaignProgress(
    val currentNumber: Int,
    val highestCompleted: Int,
) {
    companion object {
        val START = CampaignProgress(currentNumber = 1, highestCompleted = 0)
    }
}
