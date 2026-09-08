package com.artillery.fehelper.websocket

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.ErrorRed
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import com.artillery.fehelper.common.SuccessGreen
import com.artillery.fehelper.json.formatJson
import com.artillery.fehelper.json.parseJson
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private const val MaxTrafficItems = 200

private enum class ConnectionState(val label: String) {
    DISCONNECTED("未连接"),
    CONNECTING("连接中"),
    CONNECTED("已连接"),
}

private enum class TrafficDirection(val label: String) {
    EVENT("事件"),
    SENT("发送"),
    RECEIVED("收到"),
    ERROR("错误"),
}

private data class TrafficItem(
    val direction: TrafficDirection,
    val time: String,
    val content: String,
    val format: String,
    val characterCount: Int,
)

@Composable
internal fun WebSocketToolScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var endpoint by remember { mutableStateOf("") }
    var endpointError by remember { mutableStateOf<String?>(null) }
    var connectionState by remember { mutableStateOf(ConnectionState.DISCONNECTED) }
    var messageInput by remember { mutableStateOf("") }
    var traffic by remember { mutableStateOf(emptyList<TrafficItem>()) }
    var trafficRevision by remember { mutableStateOf(0) }

    fun appendTraffic(direction: TrafficDirection, content: String) {
        traffic = (traffic + trafficItem(direction = direction, content = content)).takeLast(MaxTrafficItems)
        trafficRevision += 1
    }
    val client = remember {
        WebSocketClient(
            onOpen = {
                connectionState = ConnectionState.CONNECTED
                appendTraffic(direction = TrafficDirection.EVENT, content = "连接成功")
            },
            onMessage = { message -> appendTraffic(direction = TrafficDirection.RECEIVED, content = message) },
            onError = { message ->
                connectionState = ConnectionState.DISCONNECTED
                appendTraffic(direction = TrafficDirection.ERROR, content = message)
            },
            onClose = { code, reason ->
                connectionState = ConnectionState.DISCONNECTED
                val detail = reason.takeIf { it.isNotBlank() }?.let { "：$it" }.orEmpty()
                appendTraffic(direction = TrafficDirection.EVENT, content = "连接已关闭（$code）$detail")
            },
        )
    }

    DisposableEffect(key1 = client) {
        onDispose { client.disconnect() }
    }

    val connect = {
        val value = endpoint.trim()
        val error = validateEndpoint(value = value)
        endpointError = error
        if (error == null) {
            endpoint = value
            connectionState = ConnectionState.CONNECTING
            appendTraffic(direction = TrafficDirection.EVENT, content = "正在连接 $value")
            client.connect(url = value)
        }
    }
    val disconnect = {
        client.disconnect()
        connectionState = ConnectionState.DISCONNECTED
        appendTraffic(direction = TrafficDirection.EVENT, content = "已断开连接")
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .safeContentPadding(),
    ) {
        val wide = maxWidth >= 900.dp
        val horizontalPadding = if (wide) 32.dp else 16.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            topBar = {
                PageTitleBar(
                    title = "WebSocket 工具",
                    horizontalPadding = horizontalPadding,
                    onBack = onBack,
                )
            },
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .padding(paddingValues = contentPadding)
                    .padding(horizontal = horizontalPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "连接 WebSocket 服务，发送文本消息并查看实时通信结果",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk),
                    )
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            ConnectionCard(
                                modifier = Modifier.weight(1f),
                                endpoint = endpoint,
                                endpointError = endpointError,
                                state = connectionState,
                                onEndpointChange = {
                                    endpoint = it
                                    endpointError = null
                                },
                                onConnect = connect,
                                onDisconnect = disconnect,
                            )
                            AnalysisCard(
                                modifier = Modifier.weight(1f),
                                state = connectionState,
                                traffic = traffic,
                            )
                        }
                    } else {
                        ConnectionCard(
                            modifier = Modifier.fillMaxWidth(),
                            endpoint = endpoint,
                            endpointError = endpointError,
                            state = connectionState,
                            onEndpointChange = {
                                endpoint = it
                                endpointError = null
                            },
                            onConnect = connect,
                            onDisconnect = disconnect,
                        )
                        AnalysisCard(
                            modifier = Modifier.fillMaxWidth(),
                            state = connectionState,
                            traffic = traffic,
                        )
                    }
                    TrafficCard(
                        modifier = Modifier.fillMaxWidth(),
                        traffic = traffic,
                        revision = trafficRevision,
                        height = if (wide) 440.dp else 360.dp,
                        onClear = {
                            traffic = emptyList()
                            trafficRevision += 1
                        },
                    )
                    SendCard(
                        modifier = Modifier.fillMaxWidth(),
                        input = messageInput,
                        enabled = connectionState == ConnectionState.CONNECTED,
                        onInputChange = { messageInput = it },
                        onSend = {
                            val message = messageInput
                            if (client.send(message = message)) {
                                appendTraffic(direction = TrafficDirection.SENT, content = message)
                            } else {
                                appendTraffic(direction = TrafficDirection.ERROR, content = "消息发送失败，连接已不可用")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    modifier: Modifier,
    endpoint: String,
    endpointError: String?,
    state: ConnectionState,
    onEndpointChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = "连接测试",
        description = "支持 ws:// 与 wss:// 地址",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = endpoint,
                onValueChange = onEndpointChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = state == ConnectionState.DISCONNECTED,
                label = { Text(text = "WebSocket 地址") },
                placeholder = { Text(text = "wss://example.com/socket") },
                singleLine = true,
                isError = endpointError != null,
                supportingText = endpointError?.let { error -> { Text(text = error) } },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConnectionStatus(state = state)
                ActionText(
                    modifier = Modifier.widthIn(min = 112.dp),
                    text = if (state == ConnectionState.DISCONNECTED) "连接" else "断开",
                    enabled = true,
                    containerColor = if (state == ConnectionState.DISCONNECTED) BrandBlue else ErrorRed,
                    onClick = if (state == ConnectionState.DISCONNECTED) onConnect else onDisconnect,
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatus(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.DISCONNECTED -> MutedInk
        ConnectionState.CONNECTING -> BrandBlue
        ConnectionState.CONNECTED -> SuccessGreen
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(10.dp).background(color = color, shape = CircleShape))
        Text(
            text = state.label,
            style = MaterialTheme.typography.labelLarge.copy(color = color, fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun AnalysisCard(
    modifier: Modifier,
    state: ConnectionState,
    traffic: List<TrafficItem>,
) {
    val sent = traffic.count { it.direction == TrafficDirection.SENT }
    val received = traffic.count { it.direction == TrafficDirection.RECEIVED }
    val receivedCharacters = traffic.filter { it.direction == TrafficDirection.RECEIVED }.sumOf { it.characterCount }
    SectionCard(
        modifier = modifier,
        title = "结果分析",
        description = "当前状态：${state.label}",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnalysisValue(modifier = Modifier.weight(1f), label = "已发送", value = sent.toString())
            AnalysisValue(modifier = Modifier.weight(1f), label = "已接收", value = received.toString())
            AnalysisValue(modifier = Modifier.weight(1f), label = "接收字符", value = receivedCharacters.toString())
        }
    }
}

@Composable
private fun AnalysisValue(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(color = MutedInk))
        SelectionContainer {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(color = Ink, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun TrafficCard(
    modifier: Modifier,
    traffic: List<TrafficItem>,
    revision: Int,
    height: Dp,
    onClear: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(key1 = revision) {
        if (traffic.isNotEmpty()) listState.scrollToItem(index = traffic.lastIndex)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(width = 1.dp, color = Border),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "消息日志",
                        style = MaterialTheme.typography.titleMedium.copy(color = Ink, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "按时间记录连接事件和收发消息",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedInk),
                    )
                }
                ActionText(
                    modifier = Modifier,
                    text = "清空",
                    enabled = traffic.isNotEmpty(),
                    containerColor = Color.Transparent,
                    contentColor = BrandBlue,
                    onClick = onClear,
                )
            }
            HorizontalDivider(color = Border)
            if (traffic.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(height),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无通信记录",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(height),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    itemsIndexed(items = traffic) { index, item ->
                        TrafficRow(modifier = Modifier.fillMaxWidth(), item = item)
                        if (index != traffic.lastIndex) HorizontalDivider(color = Border)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficRow(modifier: Modifier, item: TrafficItem) {
    val directionColor = when (item.direction) {
        TrafficDirection.EVENT -> MutedInk
        TrafficDirection.SENT -> BrandBlue
        TrafficDirection.RECEIVED -> SuccessGreen
        TrafficDirection.ERROR -> ErrorRed
    }
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.direction.label,
                style = MaterialTheme.typography.labelLarge.copy(color = directionColor, fontWeight = FontWeight.Bold),
            )
            Text(
                text = "${item.format} · ${item.characterCount} 字符",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium.copy(color = MutedInk),
            )
            Text(text = item.time, style = MaterialTheme.typography.labelMedium.copy(color = MutedInk))
        }
        SelectionContainer {
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium.copy(color = Ink),
            )
        }
    }
}

@Composable
private fun SendCard(
    modifier: Modifier,
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = "消息发送",
        description = if (enabled) "输入要发送的文本内容" else "连接成功后可发送消息",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                label = { Text(text = "消息内容") },
                placeholder = { Text(text = "输入文本或 JSON") },
                minLines = 4,
                maxLines = 10,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                ActionText(
                    modifier = Modifier.widthIn(min = 160.dp),
                    text = "发送消息",
                    enabled = enabled && input.isNotEmpty(),
                    containerColor = BrandBlue,
                    onClick = onSend,
                )
            }
        }
    }
}

@Composable
private fun ActionText(
    modifier: Modifier,
    text: String,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color = Color.White,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(
                color = if (enabled) containerColor else Border,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            color = if (enabled) contentColor else MutedInk,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
    )
}

private fun validateEndpoint(value: String): String? = when {
    value.isEmpty() -> "请输入 WebSocket 地址"
    value.any { it.isWhitespace() } -> "地址不能包含空格"
    !(value.startsWith(prefix = "ws://", ignoreCase = true) || value.startsWith(prefix = "wss://", ignoreCase = true)) ->
        "地址必须以 ws:// 或 wss:// 开头"
    value.substringAfter(delimiter = "://").isBlank() -> "请输入有效的服务地址"
    else -> null
}

private fun trafficItem(direction: TrafficDirection, content: String): TrafficItem {
    val shouldFormat = direction == TrafficDirection.SENT || direction == TrafficDirection.RECEIVED
    val formatted = if (shouldFormat) {
        runCatching { formatJson(element = parseJson(text = content)) }.getOrNull()
    } else {
        null
    }
    return TrafficItem(
        direction = direction,
        time = currentTime(),
        content = formatted ?: content,
        format = when {
            formatted != null -> "JSON"
            shouldFormat -> "文本"
            else -> direction.label
        },
        characterCount = content.length,
    )
}

private fun currentTime(): String {
    val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${time.hour.toString().padStart(length = 2, padChar = '0')}:" +
        "${time.minute.toString().padStart(length = 2, padChar = '0')}:" +
        time.second.toString().padStart(length = 2, padChar = '0')
}
