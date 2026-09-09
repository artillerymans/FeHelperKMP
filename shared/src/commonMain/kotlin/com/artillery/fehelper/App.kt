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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
import com.artillery.fehelper.loan.LoanCalculatorScreen
import com.artillery.fehelper.password.RandomPasswordScreen
import com.artillery.fehelper.time.TimestampConverterScreen
import com.artillery.fehelper.totp.TotpScreen
import com.artillery.fehelper.websocket.WebSocketToolScreen
import com.artillery.state.StateViewModel
import com.artillery.state.collectAsState
import kotlinx.coroutines.flow.collect

private const val AmzToolId = "amz-water-ticket"
private const val JsonFormatterToolId = "json-formatter"
private const val TimestampConverterToolId = "timestamp-converter"
private const val TotpToolId = "totp"
private const val WebSocketToolId = "websocket-tool"
private const val RandomPasswordToolId = "random-password"
private const val LoanCalculatorToolId = "loan-calculator"

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
    ToolDefinition(
        id = TimestampConverterToolId,
        title = "时间（戳）转换",
        description = "本地时间与 Unix 时间戳互转，支持秒、毫秒和世界时钟",
        category = "开发工具",
    ),
    ToolDefinition(
        id = TotpToolId,
        title = "2FA 动态口令",
        description = "本地生成 TOTP 验证码，支持 Base32 密钥和 otpauth URI",
        category = "安全工具",
    ),
    ToolDefinition(
        id = WebSocketToolId,
        title = "WebSocket 工具",
        description = "测试 WebSocket 连接、消息收发并分析通信结果",
        category = "开发工具",
    ),
    ToolDefinition(
        id = RandomPasswordToolId,
        title = "随机密码生成器",
        description = "按自定义字符类型生成随机密码，支持批量生成和一键复制",
        category = "安全工具",
    ),
    ToolDefinition(
        id = LoanCalculatorToolId,
        title = "贷款利率计算器",
        description = "根据本金、期限和还款方式反推年化利率或查看逐月还款明细",
        category = "生活工具",
    ),
)

private enum class Destination {
    HOME,
    AMZ_CALCULATOR,
    JSON_FORMATTER,
    TIMESTAMP_CONVERTER,
    TOTP,
    WEBSOCKET_TOOL,
    RANDOM_PASSWORD,
    LOAN_CALCULATOR,
}

private data class HomeContentState(
    val layout: ToolLayout,
    val visibleTools: List<ToolDefinition>,
)

private data class HomeState(
    val tools: List<ToolDefinition>,
    val query: String = "",
    val layout: ToolLayout = ToolLayout.GRID,
) {
    val content: HomeContentState
        get() {
            val keyword = query.trim()
            val visibleTools = if (keyword.isEmpty()) {
                tools
            } else {
                tools.filter { tool ->
                    listOf(tool.title, tool.description, tool.category).any { value ->
                        value.contains(keyword, ignoreCase = true)
                    }
                }
            }
            return HomeContentState(layout = layout, visibleTools = visibleTools)
        }
}

private class HomeViewModel(tools: List<ToolDefinition>) : StateViewModel<HomeState>(initialState = HomeState(tools = tools)) {
    fun onQueryChange(value: String) {
        setState { copy(query = value) }
    }

    fun onLayoutChange(value: ToolLayout) {
        setState { copy(layout = value) }
    }
}

@Composable
@Preview
fun App() {
    FeHelperTheme {
        val backStack = remember { mutableStateListOf(Destination.HOME) }
        if (isDebugBuild()) {
            LaunchedEffect(key1 = Unit) {
                snapshotFlow(block = { backStack.toList() }).collect { stack ->
                    println("[Nav] currentPage=${stack.lastOrNull()}, backStack=$stack")
                }
            }
        }
        val navigateBack = {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        }
        NavDisplay(
            backStack = backStack,
            onBack = navigateBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry(Destination.HOME) {
                    HomeScreen(
                        tools = tools,
                        onToolClick = { tool ->
                            when (tool.id) {
                                AmzToolId -> backStack.add(Destination.AMZ_CALCULATOR)
                                JsonFormatterToolId -> backStack.add(Destination.JSON_FORMATTER)
                                TimestampConverterToolId -> backStack.add(Destination.TIMESTAMP_CONVERTER)
                                TotpToolId -> backStack.add(Destination.TOTP)
                                WebSocketToolId -> backStack.add(Destination.WEBSOCKET_TOOL)
                                RandomPasswordToolId -> backStack.add(Destination.RANDOM_PASSWORD)
                                LoanCalculatorToolId -> backStack.add(Destination.LOAN_CALCULATOR)
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
                entry(Destination.TIMESTAMP_CONVERTER) {
                    TimestampConverterScreen(onBack = navigateBack)
                }
                entry(Destination.TOTP) {
                    TotpScreen(onBack = navigateBack)
                }
                entry(Destination.WEBSOCKET_TOOL) {
                    WebSocketToolScreen(
                        modifier = Modifier,
                        onBack = navigateBack,
                    )
                }
                entry(Destination.RANDOM_PASSWORD) {
                    RandomPasswordScreen(
                        modifier = Modifier,
                        onBack = navigateBack,
                    )
                }
                entry(Destination.LOAN_CALCULATOR) {
                    LoanCalculatorScreen(
                        modifier = Modifier,
                        onBack = navigateBack,
                    )
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
    val viewModel: HomeViewModel = viewModel(initializer = { HomeViewModel(tools = tools) })

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .safeContentPadding(),
    ) {
        val wide = maxWidth >= 720.dp

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 1200.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 24.dp),
        ) {
            Text(text = "前端工具", style = MaterialTheme.typography.headlineMedium.copy(color = Ink))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "选择一个工具开始使用", style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk))
            Spacer(modifier = Modifier.height(24.dp))

            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val query by viewModel.collectAsState(HomeState::query)
                    SearchField(
                        modifier = Modifier.weight(1f),
                        query = query,
                        onQueryChange = viewModel::onQueryChange,
                    )
                    val layout by viewModel.collectAsState(HomeState::layout)
                    LayoutToggle(layout = layout, onLayoutChange = viewModel::onLayoutChange)
                }
            } else {
                val query by viewModel.collectAsState(HomeState::query)
                SearchField(
                    modifier = Modifier.fillMaxWidth(),
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val layout by viewModel.collectAsState(HomeState::layout)
                    LayoutToggle(layout = layout, onLayoutChange = viewModel::onLayoutChange)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "工具目录", style = MaterialTheme.typography.titleLarge.copy(color = Ink))
            Spacer(modifier = Modifier.height(12.dp))
            val resultsState by viewModel.collectAsState(HomeState::content)
            ToolResults(state = resultsState, wide = wide, onToolClick = onToolClick)
        }
    }
}

@Composable
private fun SearchField(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        label = { Text(text = "搜索工具") },
        placeholder = { Text(text = "输入工具名称或类别") },
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
        Text(text = "展示方式", style = MaterialTheme.typography.labelLarge.copy(color = MutedInk))
        FilterChip(
            selected = layout == ToolLayout.LIST,
            onClick = { onLayoutChange(ToolLayout.LIST) },
            label = { Text(text = "列表") },
            modifier = Modifier.heightIn(min = 48.dp),
            elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp),
        )
        FilterChip(
            selected = layout == ToolLayout.GRID,
            onClick = { onLayoutChange(ToolLayout.GRID) },
            label = { Text(text = "网格") },
            modifier = Modifier.heightIn(min = 48.dp),
            elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp),
        )
    }
}

@Composable
private fun ToolResults(
    state: HomeContentState,
    wide: Boolean,
    onToolClick: (ToolDefinition) -> Unit,
) {
    if (state.visibleTools.isEmpty()) {
        Text(text = "没有匹配的工具", style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk))
        return
    }

    if (state.layout == ToolLayout.LIST) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.visibleTools.forEach { tool ->
                ToolEntryCard(
                    modifier = Modifier.fillMaxWidth(),
                    tool = tool,
                    layout = state.layout,
                    onClick = { onToolClick(tool) },
                )
            }
        }
        return
    }

    val columns = if (wide) 3 else 2
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.visibleTools.chunked(columns).forEach { rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowTools.forEach { tool ->
                    ToolEntryCard(
                        modifier = Modifier.weight(1f),
                        tool = tool,
                        layout = state.layout,
                        onClick = { onToolClick(tool) },
                    )
                }
                repeat(columns - rowTools.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
