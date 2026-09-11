package com.freesudoku.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.freesudoku.app.domain.model.DifficultyBand
import com.freesudoku.app.ui.common.label

/** Difficulty picker for "Partida rápida": one puzzle, no campaign position. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPlaySheet(
    onPick: (DifficultyBand) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.testTag("quick_play_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Partida rápida", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Un puzzle suelto, sin afectar tu campaña. Elegí el nivel:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DifficultyBand.QUICK_PLAY_SELECTABLE.forEach { band ->
                Button(
                    onClick = { onPick(band) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_play_${band.name}"),
                ) {
                    Text(band.label())
                }
            }
        }
    }
}
