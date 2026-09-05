package com.artillery.fehelper.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
)

internal enum class ToolLayout {
    LIST,
    GRID,
}

@Composable
internal fun ToolEntryCard(
    tool: ToolDefinition,
    layout: ToolLayout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Border),
    ) {
        if (layout == ToolLayout.LIST) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ToolEntryText(tool, Modifier.weight(1f))
                Text("打开", color = BrandBlue, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                ToolEntryText(tool)
                Spacer(Modifier.height(20.dp))
                Text("打开工具", color = BrandBlue, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ToolEntryText(tool: ToolDefinition, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(tool.category, color = MutedInk, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Text(tool.title, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(tool.description, color = MutedInk, style = MaterialTheme.typography.bodyMedium)
    }
}
