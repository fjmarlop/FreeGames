package com.freesudoku.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun GameTopStatus(
    puzzleNumber: Int,
    elapsedText: String,
    mistakes: Int,
    mistakeLimitEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Puzzle #$puzzleNumber", style = MaterialTheme.typography.labelLarge)
        Text(
            if (mistakeLimitEnabled) "Errores: $mistakes/${3}" else "Errores: $mistakes",
            style = MaterialTheme.typography.labelLarge,
            color = if (mistakeLimitEnabled && mistakes >= 2) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.testTag("mistake_counter"),
        )
        Text(elapsedText, style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("timer"))
    }
}
