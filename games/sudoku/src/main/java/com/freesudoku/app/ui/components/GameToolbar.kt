package com.freesudoku.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
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
        ToolButton(Icons.AutoMirrored.Filled.Undo, "Deshacer", enabled = canUndo, onClick = onUndo, tag = "tool_undo")
        ToolButton(Icons.AutoMirrored.Filled.Redo, "Rehacer", enabled = canRedo, onClick = onRedo, tag = "tool_redo")
        ToolButton(Icons.AutoMirrored.Filled.Backspace, "Borrar", onClick = onErase, tag = "tool_erase")
        ToolButton(
            Icons.Filled.EditNote,
            if (notesMode) "Notas ON" else "Notas",
            onClick = onToggleNotes,
            tag = "tool_notes",
            highlighted = notesMode,
        )
        ToolButton(Icons.Filled.Lightbulb, "Pista", onClick = onHint, tag = "tool_hint")
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    val content = when {
        !enabled -> MaterialTheme.colorScheme.outline
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = 52.dp)
            .testTag(tag),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val iconBg = if (highlighted) MaterialTheme.colorScheme.surfaceContainerHigh else null
            androidx.compose.material3.Surface(
                color = iconBg ?: androidx.compose.ui.graphics.Color.Transparent,
                border = if (highlighted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.padding(4.dp).size(18.dp))
            }
            Text(label, fontSize = 10.sp, color = content, style = MaterialTheme.typography.labelSmall)
        }
    }
}
