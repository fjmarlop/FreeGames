package com.freesudoku.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.ui.common.label
import com.freesudoku.app.ui.game.GameUiState

@Composable
fun GameTopStatus(
    puzzleNumber: Int,
    elapsedText: String,
    mistakes: Int,
    mistakeLimitEnabled: Boolean,
    band: DifficultyBand?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ApexChip(text = "Puzzle #$puzzleNumber")
        if (mistakeLimitEnabled) {
            Lives(mistakes = mistakes, modifier = Modifier.testTag("mistake_counter"))
        } else {
            Text(
                "Errores: $mistakes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("mistake_counter"),
            )
        }
        Text(
            elapsedText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("timer"),
        )
    }
    if (band != null) {
        Text(
            band.label(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun Lives(mistakes: Int, modifier: Modifier = Modifier) {
    val remaining = (GameUiState.MISTAKE_LIMIT - mistakes).coerceAtLeast(0)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(GameUiState.MISTAKE_LIMIT) { i ->
            val alive = i < remaining
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = if (alive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
