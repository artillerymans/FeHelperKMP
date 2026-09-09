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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import com.artillery.state.StateViewModel
import com.artillery.state.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

private data class RealtimeState(
    val snapshot: TimeSnapshot,
    val isRunning: Boolean,
)

private data class TimestampInputState(
    val input: String,
    val unit: TimestampUnit,
    val result: TimestampConversion?,
)

private data class LocalTimeInputState(
    val input: String,
    val result: TimestampConversion?,
)

private data class WorldClockState(
    val clocks: List<WorldClock>,
    val selectedOffset: Int,
)

private data class TimestampConverterState(
    val current: Instant,
    val isRunning: Boolean,
    val timestampInput: String,
    val timestampUnit: TimestampUnit,
    val timestampResult: TimestampConversion?,
    val localInput: String,
    val localResult: TimestampConversion?,
    val selectedWorldClock: Int,
) {
    val realtime: RealtimeState
        get() = RealtimeState(snapshot = nowSnapshot(now = current), isRunning = isRunning)

    val timestamp: TimestampInputState
        get() = TimestampInputState(input = timestampInput, unit = timestampUnit, result = timestampResult)

    val localTime: LocalTimeInputState
        get() = LocalTimeInputState(input = localInput, result = localResult)

    val worldClock: WorldClockState
        get() = WorldClockState(clocks = worldClocks(now = current), selectedOffset = selectedWorldClock)
}

private fun initialTimestampConverterState(): TimestampConverterState {
    val now = Clock.System.now()
    val snapshot = nowSnapshot(now = now)
    return TimestampConverterState(
        current = now,
        isRunning = true,
        timestampInput = snapshot.seconds,
        timestampUnit = TimestampUnit.SECONDS,
        timestampResult = null,
        localInput = snapshot.localTime,
        localResult = null,
        selectedWorldClock = 8,
    )
}

private class TimestampConverterViewModel : StateViewModel<TimestampConverterState>(
    initialState = initialTimestampConverterState(),
) {
    init {
        viewModelScope.launch {
            while (isActive) {
                if (state.value.isRunning) setState { copy(current = Clock.System.now()) }
                delay(1000.milliseconds)
            }
        }
    }

    fun onToggleRealtime() {
        setState { copy(isRunning = !isRunning) }
    }

    fun onTimestampInputChange(value: String) {
        setState { copy(timestampInput = value, timestampResult = null) }
    }

    fun onTimestampUnitChange(value: TimestampUnit) {
        setState { copy(timestampUnit = value, timestampResult = null) }
    }

    fun onConvertTimestamp() {
        setState {
            copy(timestampResult = timestampToLocalTime(value = timestampInput, unit = timestampUnit))
        }
    }

    fun onLocalInputChange(value: String) {
        setState { copy(localInput = value, localResult = null) }
    }

    fun onConvertLocalTime() {
        setState { copy(localResult = localTimeToTimestamp(value = localInput)) }
    }

    fun onWorldClockSelect(offset: Int) {
        setState { copy(selectedWorldClock = offset) }
    }
}

@Composable
internal fun TimestampConverterScreen(onBack: () -> Unit) {
    val viewModel: TimestampConverterViewModel = viewModel(initializer = { TimestampConverterViewModel() })
    val realtimeState by viewModel.collectAsState(TimestampConverterState::realtime)
    val timestampState by viewModel.collectAsState(TimestampConverterState::timestamp)
    val localTimeState by viewModel.collectAsState(TimestampConverterState::localTime)
    val worldClockState by viewModel.collectAsState(TimestampConverterState::worldClock)

    BoxWithConstraints(
        modifier = Modifier
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
                        state = realtimeState,
                        wide = wide,
                        onToggle = viewModel::onToggleRealtime,
                    )
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            TimestampInputCard(
                                state = timestampState,
                                onInputChange = viewModel::onTimestampInputChange,
                                onUnitChange = viewModel::onTimestampUnitChange,
                                onConvert = viewModel::onConvertTimestamp,
                                modifier = Modifier.weight(1f),
                            )
                            LocalTimeInputCard(
                                state = localTimeState,
                                onInputChange = viewModel::onLocalInputChange,
                                onConvert = viewModel::onConvertLocalTime,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        TimestampInputCard(
                            state = timestampState,
                            onInputChange = viewModel::onTimestampInputChange,
                            onUnitChange = viewModel::onTimestampUnitChange,
                            onConvert = viewModel::onConvertTimestamp,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LocalTimeInputCard(
                            state = localTimeState,
                            onInputChange = viewModel::onLocalInputChange,
                            onConvert = viewModel::onConvertLocalTime,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    WorldClockCard(
                        state = worldClockState,
                        wide = wide,
                        onSelect = viewModel::onWorldClockSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun RealtimeCard(
    state: RealtimeState,
    wide: Boolean,
    onToggle: () -> Unit,
) {
    SectionCard(
        title = "当前时间",
        description = "Asia/Shanghai · ${if (state.isRunning) "每秒自动更新" else "已暂停"}",
    ) {
        val stats = listOf(
            "本地时间" to state.snapshot.localTime,
            "Unix 秒" to state.snapshot.seconds,
            "Unix 毫秒" to state.snapshot.milliseconds,
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
                RealtimeToggleButton(isRunning = state.isRunning, onToggle = onToggle)
            }
        } else {
            stats.forEachIndexed { index, (label, value) ->
                StatItem(modifier = Modifier.fillMaxWidth(), label = label, value = value)
                if (index != stats.lastIndex) Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            RealtimeToggleButton(
                isRunning = state.isRunning,
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
    state: TimestampInputState,
    onInputChange: (String) -> Unit,
    onUnitChange: (TimestampUnit) -> Unit,
    onConvert: () -> Unit,
) {
    Column(modifier = modifier) {
        SectionCard(title = "Unix 时间戳 → 本地时间", description = "按 Asia/Shanghai 展示转换结果") {
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Unix 时间戳") },
                placeholder = { Text(text = "例如 1757304000") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            UnitSelector(selected = state.unit, onSelected = onUnitChange)
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
            ResultField(label = "Asia/Shanghai 本地时间", value = state.result?.localTime)
            ErrorText(error = state.result?.error)
        }
    }
}

@Composable
private fun LocalTimeInputCard(
    modifier: Modifier,
    state: LocalTimeInputState,
    onInputChange: (String) -> Unit,
    onConvert: () -> Unit,
) {
    Column(modifier = modifier) {
        SectionCard(title = "本地时间 → Unix 时间戳", description = "输入时间按 Asia/Shanghai 解析") {
            OutlinedTextField(
                value = state.input,
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
            ResultField(label = "Unix 秒", value = state.result?.seconds)
            Spacer(modifier = Modifier.height(8.dp))
            ResultField(label = "Unix 毫秒", value = state.result?.milliseconds)
            ErrorText(error = state.result?.error)
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
    state: WorldClockState,
    wide: Boolean,
    onSelect: (Int) -> Unit,
) {
    SectionCard(
        title = "世界时钟",
        description = "代表城市按标准时区归类，夏令时期间实际偏移可能变化",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.clocks.firstOrNull { it.offsetHours == state.selectedOffset }?.let { selectedClock ->
                Text(
                    text = "当前选择：${selectedClock.label} · ${selectedClock.location} · ${selectedClock.localTime}",
                    style = MaterialTheme.typography.labelLarge.copy(color = BrandBlue),
                )
            }
            val columns = if (wide) 2 else 1
            state.clocks.chunked(columns).forEach { rowClocks ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowClocks.forEach { clock ->
                        WorldClockItem(
                            clock = clock,
                            selected = clock.offsetHours == state.selectedOffset,
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
            Text(
                text = "${clock.label} · ${clock.location}",
                style = MaterialTheme.typography.labelLarge.copy(color = if (selected) BrandBlue else Ink),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = clock.localTime, style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk))
        }
    }
}
