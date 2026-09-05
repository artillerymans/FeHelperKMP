package com.artillery.fehelper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.artillery.fehelper.amz.AmzCalculatorScreen
import com.artillery.fehelper.common.FeHelperTheme
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.ToolDefinition
import com.artillery.fehelper.common.ToolEntryCard
import com.artillery.fehelper.common.ToolLayout
import com.artillery.fehelper.json.JsonFormatterScreen

private const val AmzToolId = "amz-water-ticket"
private const val JsonFormatterToolId = "json-formatter"

private val tools = listOf(
    ToolDefinition(
        id = AmzToolId,
        title = "AMZ 水票计算",
        description = "计算亚马逊货物的运费、税金、贴标费用和优惠后的支付金额",
        category = "亚马逊物流",
    ),
    ToolDefinition(
        id = JsonFormatterToolId,
        title = "JSON 格式化",
        description = "格式化、排序、解码 JSON，支持表格视图",
        category = "开发工具",
    ),
)

private enum class Destination {
    HOME,
    AMZ_CALCULATOR,
    JSON_FORMATTER,
}

@Composable
@Preview
fun App() {
    FeHelperTheme {
        val backStack = remember { mutableStateListOf(Destination.HOME) }
        val navigateBack = {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        }
        NavDisplay(
            backStack = backStack,
            onBack = navigateBack,
            entryProvider = entryProvider {
                entry(Destination.HOME) {
                    HomeScreen(
                        tools = tools,
                        onToolClick = { tool ->
                            when (tool.id) {
                                AmzToolId -> backStack.add(Destination.AMZ_CALCULATOR)
                                JsonFormatterToolId -> backStack.add(Destination.JSON_FORMATTER)
                            }
                        },
                    )
                }
                entry(Destination.AMZ_CALCULATOR) {
                    AmzCalculatorScreen(onBack = navigateBack)
                }
                entry(Destination.JSON_FORMATTER) {
                    JsonFormatterScreen(onBack = navigateBack)
                }
            },
        )
    }
}

@Composable
private fun HomeScreen(
    tools: List<ToolDefinition>,
    onToolClick: (ToolDefinition) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var layout by remember { mutableStateOf(ToolLayout.GRID) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .safeContentPadding(),
    ) {
        val wide = maxWidth >= 720.dp
        val keyword = query.trim()
        val visibleTools = if (keyword.isEmpty()) {
            tools
        } else {
            tools.filter { tool ->
                listOf(tool.title, tool.description, tool.category).any { it.contains(keyword, ignoreCase = true) }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 1200.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 24.dp),
        ) {
            Text("前端工具", color = Ink, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("选择一个工具开始使用", color = MutedInk, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchField(query, { query = it }, Modifier.weight(1f))
                    LayoutToggle(layout, { layout = it })
                }
            } else {
                SearchField(query, { query = it }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    LayoutToggle(layout, { layout = it })
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("工具目录", color = Ink, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            ToolResults(visibleTools, layout, wide, onToolClick)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        label = { Text("搜索工具") },
        placeholder = { Text("输入工具名称或类别") },
        singleLine = true,
    )
}

@Composable
private fun LayoutToggle(
    layout: ToolLayout,
    onLayoutChange: (ToolLayout) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("展示方式", color = MutedInk, style = MaterialTheme.typography.labelLarge)
        FilterChip(
            selected = layout == ToolLayout.LIST,
            onClick = { onLayoutChange(ToolLayout.LIST) },
            label = { Text("列表") },
            modifier = Modifier.heightIn(min = 48.dp),
        )
        FilterChip(
            selected = layout == ToolLayout.GRID,
            onClick = { onLayoutChange(ToolLayout.GRID) },
            label = { Text("网格") },
            modifier = Modifier.heightIn(min = 48.dp),
        )
    }
}

@Composable
private fun ToolResults(
    tools: List<ToolDefinition>,
    layout: ToolLayout,
    wide: Boolean,
    onToolClick: (ToolDefinition) -> Unit,
) {
    if (tools.isEmpty()) {
        Text("没有匹配的工具", color = MutedInk, style = MaterialTheme.typography.bodyLarge)
        return
    }

    if (layout == ToolLayout.LIST) {
        tools.forEachIndexed { index, tool ->
            ToolEntryCard(tool, layout, { onToolClick(tool) }, Modifier.fillMaxWidth())
            if (index != tools.lastIndex) Spacer(Modifier.height(12.dp))
        }
        return
    }

    val columns = if (wide) 3 else 2
    tools.chunked(columns).forEachIndexed { index, rowTools ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rowTools.forEach { tool ->
                ToolEntryCard(tool, layout, { onToolClick(tool) }, Modifier.weight(1f))
            }
            repeat(columns - rowTools.size) { Spacer(Modifier.weight(1f)) }
        }
        if (index != (tools.size - 1) / columns) Spacer(Modifier.height(12.dp))
    }
}
