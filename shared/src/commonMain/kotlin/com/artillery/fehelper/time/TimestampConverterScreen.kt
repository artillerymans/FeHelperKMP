package com.artillery.fehelper.time

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import kotlinx.coroutines.delay
import kotlin.time.Clock

@Composable
internal fun TimestampConverterScreen(onBack: () -> Unit) {
    val initialSnapshot = remember { nowSnapshot() }
    var current by remember { mutableStateOf(Clock.System.now()) }
    var isRunning by remember { mutableStateOf(true) }
    var timestampInput by remember { mutableStateOf(initialSnapshot.seconds) }
    var timestampUnit by remember { mutableStateOf(TimestampUnit.SECONDS) }
    var timestampResult by remember { mutableStateOf<TimestampConversion?>(null) }
    var localInput by remember { mutableStateOf(initialSnapshot.localTime) }
    var localResult by remember { mutableStateOf<TimestampConversion?>(null) }
    var selectedWorldClock by remember { mutableStateOf(8) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (true) {
                current = Clock.System.now()
                delay(1000)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .safeContentPadding(),
    ) {
        val wide = maxWidth >= 900.dp
        val horizontalPadding = if (wide) 32.dp else 16.dp
        val snapshot = nowSnapshot(current)
        val clocks = worldClocks(current)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            topBar = {
                PageTitleBar(
                    title = "时间（戳）转换",
                    horizontalPadding = horizontalPadding,
                    onBack = onBack,
                )
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 1200.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "以 Asia/Shanghai 为基准，快速完成本地时间、Unix 时间戳和世界时区转换",
                        color = MutedInk,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    RealtimeCard(
                        snapshot = snapshot,
                        wide = wide,
                        isRunning = isRunning,
                        onToggle = { isRunning = !isRunning },
                    )
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            TimestampInputCard(
                                input = timestampInput,
                                unit = timestampUnit,
                                result = timestampResult,
                                onInputChange = {
                                    timestampInput = it
                                    timestampResult = null
                                },
                                onUnitChange = {
                                    timestampUnit = it
                                    timestampResult = null
                                },
                                onConvert = { timestampResult = timestampToLocalTime(timestampInput, timestampUnit) },
                                modifier = Modifier.weight(1f),
                            )
                            LocalTimeInputCard(
                                input = localInput,
                                result = localResult,
                                onInputChange = {
                                    localInput = it
                                    localResult = null
                                },
                                onConvert = { localResult = localTimeToTimestamp(localInput) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        TimestampInputCard(
                            input = timestampInput,
                            unit = timestampUnit,
                            result = timestampResult,
                            onInputChange = {
                                timestampInput = it
                                timestampResult = null
                            },
                            onUnitChange = {
                                timestampUnit = it
                                timestampResult = null
                            },
                            onConvert = { timestampResult = timestampToLocalTime(timestampInput, timestampUnit) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LocalTimeInputCard(
                            input = localInput,
                            result = localResult,
                            onInputChange = {
                                localInput = it
                                localResult = null
                            },
                            onConvert = { localResult = localTimeToTimestamp(localInput) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    WorldClockCard(
                        clocks = clocks,
                        wide = wide,
                        selectedOffset = selectedWorldClock,
                        onSelect = { selectedWorldClock = it },
                    )
                }
            }
        }
    }
}

@Composable
private fun RealtimeCard(
    snapshot: TimeSnapshot,
    wide: Boolean,
    isRunning: Boolean,
    onToggle: () -> Unit,
) {
    SectionCard(
        title = "当前时间",
        description = "Asia/Shanghai · ${if (isRunning) "每秒自动更新" else "已暂停"}",
    ) {
        val stats = listOf(
            "本地时间" to snapshot.localTime,
            "Unix 秒" to snapshot.seconds,
            "Unix 毫秒" to snapshot.milliseconds,
        )
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    stats.forEach { (label, value) ->
                        StatItem(label, value, Modifier.weight(1f))
                    }
                }
                RealtimeToggleButton(isRunning = isRunning, onToggle = onToggle)
            }
        } else {
            stats.forEachIndexed { index, (label, value) ->
                StatItem(label, value, Modifier.fillMaxWidth())
                if (index != stats.lastIndex) Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(16.dp))
            RealtimeToggleButton(
                isRunning = isRunning,
                onToggle = onToggle,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RealtimeToggleButton(
    isRunning: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onToggle,
        modifier = modifier.heightIn(min = 48.dp),
    ) {
        if (!isRunning) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
        }
        Text(if (isRunning) "暂停" else "开始")
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, color = MutedInk, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        SelectionContainer {
            Text(value, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TimestampInputCard(
    input: String,
    unit: TimestampUnit,
    result: TimestampConversion?,
    onInputChange: (String) -> Unit,
    onUnitChange: (TimestampUnit) -> Unit,
    onConvert: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        SectionCard(title = "Unix 时间戳 → 本地时间", description = "按 Asia/Shanghai 展示转换结果") {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Unix 时间戳") },
                placeholder = { Text("例如 1757304000") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            UnitSelector(selected = unit, onSelected = onUnitChange)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConvert,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("转换为本地时间")
            }
            Spacer(Modifier.height(12.dp))
            ResultField("Asia/Shanghai 本地时间", result?.localTime)
            ErrorText(result?.error)
        }
    }
}

@Composable
private fun LocalTimeInputCard(
    input: String,
    result: TimestampConversion?,
    onInputChange: (String) -> Unit,
    onConvert: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        SectionCard(title = "本地时间 → Unix 时间戳", description = "输入时间按 Asia/Shanghai 解析") {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("本地时间") },
                placeholder = { Text("如 2026-10-07 10:10") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConvert,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("转换为时间戳")
            }
            Spacer(Modifier.height(12.dp))
            ResultField("Unix 秒", result?.seconds)
            Spacer(Modifier.height(8.dp))
            ResultField("Unix 毫秒", result?.milliseconds)
            ErrorText(result?.error)
        }
    }
}

@Composable
private fun UnitSelector(selected: TimestampUnit, onSelected: (TimestampUnit) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("输入单位", color = MutedInk, style = MaterialTheme.typography.labelLarge)
        TimestampUnit.entries.forEach { unit ->
            FilterChip(
                selected = selected == unit,
                onClick = { onSelected(unit) },
                label = { Text(unit.label) },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun ResultField(label: String, value: String?) {
    OutlinedTextField(
        value = value.orEmpty(),
        onValueChange = {},
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text("转换后显示") },
        readOnly = true,
        singleLine = true,
    )
}

@Composable
private fun ErrorText(error: String?) {
    error?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WorldClockCard(
    clocks: List<WorldClock>,
    wide: Boolean,
    selectedOffset: Int,
    onSelect: (Int) -> Unit,
) {
    SectionCard(
        title = "世界时钟",
        description = "以同一时刻查看 GMT-12 至 GMT+12",
    ) {
        clocks.firstOrNull { it.offsetHours == selectedOffset }?.let { selectedClock ->
            Text(
                text = "当前选择：${selectedClock.label} · ${selectedClock.localTime}",
                color = BrandBlue,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(12.dp))
        }
        val columns = if (wide) 2 else 1
        clocks.chunked(columns).forEachIndexed { rowIndex, rowClocks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowClocks.forEach { clock ->
                    WorldClockItem(
                        clock = clock,
                        selected = clock.offsetHours == selectedOffset,
                        onClick = { onSelect(clock.offsetHours) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(columns - rowClocks.size) { Spacer(Modifier.weight(1f)) }
            }
            if (rowIndex != (clocks.size - 1) / columns) Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WorldClockItem(
    clock: WorldClock,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White,
        border = BorderStroke(1.dp, if (selected) BrandBlue else Border),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(clock.label, color = if (selected) BrandBlue else Ink, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(clock.localTime, color = MutedInk, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
