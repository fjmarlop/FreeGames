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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    elapsedMsText: String,
    mistakes: Int,
    hintsUsed: Int,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onHome,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.testTag("result_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ApexChip(text = "Objetivo cumplido", emphasized = true)
            Text(
                "¡Completado!",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            ResultStat("Tiempo", elapsedMsText)
            ResultStat("Errores", mistakes.toString())
            ResultStat("Pistas", hintsUsed.toString())
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .testTag("result_next"),
            ) {
                Text("Siguiente puzzle")
            }
            TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text("Volver a Home")
            }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String) {
    Text(
        "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
