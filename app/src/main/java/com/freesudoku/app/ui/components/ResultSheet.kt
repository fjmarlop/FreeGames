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
import com.freesudoku.app.ui.format.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(
    elapsedMsText: String,
    mistakes: Int,
    hintsUsed: Int,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onHome, modifier = Modifier.testTag("result_sheet")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("¡Completado!", style = MaterialTheme.typography.headlineSmall)
            Text("Tiempo: $elapsedMsText")
            Text("Errores: $mistakes")
            Text("Pistas: $hintsUsed")
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().testTag("result_next")) {
                Text("Siguiente puzzle")
            }
            TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                Text("Volver a Home")
            }
        }
    }
}

fun resultTime(millis: Long): String = formatDuration(millis)
