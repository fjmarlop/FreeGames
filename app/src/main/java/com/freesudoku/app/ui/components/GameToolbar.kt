package com.freesudoku.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    notesMode: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onErase: () -> Unit,
    onToggleNotes: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToolButton("Deshacer", enabled = canUndo, onClick = onUndo, tag = "tool_undo")
        ToolButton("Rehacer", enabled = canRedo, onClick = onRedo, tag = "tool_redo")
        ToolButton("Borrar", onClick = onErase, tag = "tool_erase")
        ToolButton(
            if (notesMode) "Notas ON" else "Notas",
            onClick = onToggleNotes,
            tag = "tool_notes",
            highlighted = notesMode,
        )
        ToolButton("Pista", onClick = onHint, tag = "tool_hint")
    }
}

@Composable
private fun ToolButton(
    label: String,
    onClick: () -> Unit,
    tag: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag(tag),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
