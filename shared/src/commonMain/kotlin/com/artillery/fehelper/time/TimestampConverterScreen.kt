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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        val snapshot = nowSnapshot(now = current)
        val clocks = worldClocks(now = current)

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
                        style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk),
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
                                onConvert = { timestampResult = timestampToLocalTime(value = timestampInput, unit = timestampUnit) },
                                modifier = Modifier.weight(1f),
                            )
                            LocalTimeInputCard(
                                input = localInput,
                                result = localResult,
                                onInputChange = {
                                    localInput = it
                                    localResult = null
                                },
                                onConvert = { localResult = localTimeToTimestamp(value = localInput) },
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
                            onConvert = { timestampResult = timestampToLocalTime(value = timestampInput, unit = timestampUnit) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LocalTimeInputCard(
                            input = localInput,
                            result = localResult,
                            onInputChange = {
                                localInput = it
                                localResult = null
                            },
                            onConvert = { localResult = localTimeToTimestamp(value = localInput) },
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
                        StatItem(modifier = Modifier.weight(1f), label = label, value = value)
                    }
                }
                RealtimeToggleButton(isRunning = isRunning, onToggle = onToggle)
            }
        } else {
            stats.forEachIndexed { index, (label, value) ->
                StatItem(modifier = Modifier.fillMaxWidth(), label = label, value = value)
                if (index != stats.lastIndex) Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
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
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    onToggle: () -> Unit,
) {
    Text(
        text = if (isRunning) "暂停" else "开始",
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(BrandBlue, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
private fun StatItem(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(color = MutedInk))
        Spacer(modifier = Modifier.height(4.dp))
        SelectionContainer {
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(color = Ink, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun TimestampInputCard(
    modifier: Modifier,
    input: String,
    unit: TimestampUnit,
    result: TimestampConversion?,
    onInputChange: (String) -> Unit,
    onUnitChange: (TimestampUnit) -> Unit,
    onConvert: () -> Unit,
) {
    Column(modifier = modifier) {
        SectionCard(title = "Unix 时间戳 → 本地时间", description = "按 Asia/Shanghai 展示转换结果") {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Unix 时间戳") },
                placeholder = { Text(text = "例如 1757304000") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            UnitSelector(selected = unit, onSelected = onUnitChange)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "转换为本地时间",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(BrandBlue, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onConvert)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White, textAlign = TextAlign.Center),
            )
            Spacer(modifier = Modifier.height(12.dp))
            ResultField(label = "Asia/Shanghai 本地时间", value = result?.localTime)
            ErrorText(error = result?.error)
        }
    }
}

@Composable
private fun LocalTimeInputCard(
    modifier: Modifier,
    input: String,
    result: TimestampConversion?,
    onInputChange: (String) -> Unit,
    onConvert: () -> Unit,
) {
    Column(modifier = modifier) {
        SectionCard(title = "本地时间 → Unix 时间戳", description = "输入时间按 Asia/Shanghai 解析") {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "本地时间") },
                placeholder = { Text(text = "如 2026-10-07 10:10") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "转换为时间戳",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(BrandBlue, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onConvert)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White, textAlign = TextAlign.Center),
            )
            Spacer(modifier = Modifier.height(12.dp))
            ResultField(label = "Unix 秒", value = result?.seconds)
            Spacer(modifier = Modifier.height(8.dp))
            ResultField(label = "Unix 毫秒", value = result?.milliseconds)
            ErrorText(error = result?.error)
        }
    }
}

@Composable
private fun UnitSelector(selected: TimestampUnit, onSelected: (TimestampUnit) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "输入单位", style = MaterialTheme.typography.labelLarge.copy(color = MutedInk))
        TimestampUnit.entries.forEach { unit ->
            FilterChip(
                selected = selected == unit,
                onClick = { onSelected(unit) },
                label = { Text(text = unit.label) },
                modifier = Modifier.heightIn(min = 48.dp),
                elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp),
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
        label = { Text(text = label) },
        placeholder = { Text(text = "转换后显示") },
        readOnly = true,
        singleLine = true,
    )
}

@Composable
private fun ErrorText(error: String?) {
    error?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error))
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            clocks.firstOrNull { it.offsetHours == selectedOffset }?.let { selectedClock ->
                Text(
                    text = "当前选择：${selectedClock.label} · ${selectedClock.localTime}",
                    style = MaterialTheme.typography.labelLarge.copy(color = BrandBlue),
                )
            }
            val columns = if (wide) 2 else 1
            clocks.chunked(columns).forEach { rowClocks ->
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
                    repeat(columns - rowClocks.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun WorldClockItem(
    modifier: Modifier,
    clock: WorldClock,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White,
        border = BorderStroke(1.dp, if (selected) BrandBlue else Border),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = clock.label, style = MaterialTheme.typography.labelLarge.copy(color = if (selected) BrandBlue else Ink))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = clock.localTime, style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk))
        }
    }
}
