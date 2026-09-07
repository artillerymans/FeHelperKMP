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
import androidx.compose.material3.LocalTextStyle
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
    modifier: Modifier = Modifier,
    tool: ToolDefinition,
    layout: ToolLayout,
    onClick: () -> Unit,
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
                ToolEntryText(modifier = Modifier.weight(1f), tool = tool)
                Text(
                    text = "打开",
                    style = LocalTextStyle.current.copy(color = BrandBlue, fontWeight = FontWeight.SemiBold),
                )
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                ToolEntryText(tool = tool)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "打开工具",
                    style = LocalTextStyle.current.copy(color = BrandBlue, fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun ToolEntryText(modifier: Modifier = Modifier, tool: ToolDefinition) {
    Column(modifier = modifier) {
        Text(text = tool.category, style = MaterialTheme.typography.labelMedium.copy(color = MutedInk))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = tool.title, style = MaterialTheme.typography.titleMedium.copy(color = Ink, fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = tool.description, style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk))
    }
}
