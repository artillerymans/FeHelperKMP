package com.artillery.fehelper.id

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import com.artillery.fehelper.common.SuccessGreen
import com.artillery.state.StateViewModel
import com.artillery.state.collectAsState
import kotlin.time.Clock

private data class UuidIdState(
    val uuidCount: String = "5",
    val uuidFormat: UuidFormat = UuidFormat.STANDARD,
    val uuids: List<String> = emptyList(),
    val uuidError: String? = null,
    val machineId: String = "1",
    val datacenterId: String = "1",
    val snowflakeCount: String = "5",
    val snowflakes: List<String> = emptyList(),
    val snowflakeError: String? = null,
    val snowflakeLastTimestamp: Long = -1,
    val snowflakeSequence: Long = 0,
    val snowflakeInput: String = "",
    val snowflakeParts: SnowflakeParts? = null,
    val parseError: String? = null,
    val nanoLength: String = "21",
    val nanoCount: String = "5",
    val nanoIds: List<String> = emptyList(),
    val nanoError: String? = null,
    val copied: String? = null,
)

private class UuidIdViewModel : StateViewModel<UuidIdState>(initialState = UuidIdState()) {
    fun onUuidCountChange(value: String) = setState { copy(uuidCount = value, uuidError = null) }
    fun onUuidFormatChange(value: UuidFormat) = setState { copy(uuidFormat = value, uuids = emptyList(), copied = null) }
    fun generateUuids() = setState {
        val error = validatePositiveRange(uuidCount, 1, MAX_ID_COUNT, "数量")
        if (error != null) copy(uuidError = error, uuids = emptyList())
        else copy(uuidError = null, uuids = generateUuids(uuidCount.toInt(), uuidFormat), copied = null)
    }

    fun onMachineIdChange(value: String) = setState { copy(machineId = value, snowflakeError = null) }
    fun onDatacenterIdChange(value: String) = setState { copy(datacenterId = value, snowflakeError = null) }
    fun onSnowflakeCountChange(value: String) = setState { copy(snowflakeCount = value, snowflakeError = null) }
    fun generateSnowflakes() = setState {
        val machineError = validatePositiveRange(machineId, 0, MAX_SNOWFLAKE_MACHINE_ID, "机器 ID")
        val datacenterError = validatePositiveRange(datacenterId, 0, MAX_SNOWFLAKE_DATACENTER_ID, "数据中心 ID")
        val countError = validatePositiveRange(snowflakeCount, 1, MAX_ID_COUNT, "数量")
        val error = listOfNotNull(machineError, datacenterError, countError).firstOrNull()
        if (error != null) copy(snowflakeError = error, snowflakes = emptyList())
        else {
            val timestamp = Clock.System.now().toEpochMilliseconds()
            val sequenceStart = if (timestamp == snowflakeLastTimestamp) snowflakeSequence else 0
            val values = generateSnowflakes(machineId.toLong(), datacenterId.toLong(), snowflakeCount.toInt(), timestamp, sequenceStart)
            copy(
            snowflakeError = null,
            snowflakes = values.map(Long::toString),
            snowflakeLastTimestamp = timestamp,
            snowflakeSequence = (sequenceStart + values.size) and 0xFFFL,
            copied = null,
            )
        }
    }

    fun onSnowflakeInputChange(value: String) = setState { copy(snowflakeInput = value, parseError = null, snowflakeParts = null) }
    fun parseSnowflake() = setState {
        val error = validateSnowflakeId(snowflakeInput)
        if (error != null) copy(parseError = error, snowflakeParts = null)
        else copy(parseError = null, snowflakeParts = parseSnowflake(snowflakeInput))
    }

    fun onNanoLengthChange(value: String) = setState { copy(nanoLength = value, nanoError = null) }
    fun onNanoCountChange(value: String) = setState { copy(nanoCount = value, nanoError = null) }
    fun generateNanoIds() = setState {
        val lengthError = validatePositiveRange(nanoLength, 1, MAX_NANO_ID_LENGTH, "长度")
        val countError = validatePositiveRange(nanoCount, 1, MAX_ID_COUNT, "数量")
        val error = listOfNotNull(lengthError, countError).firstOrNull()
        if (error != null) copy(nanoError = error, nanoIds = emptyList())
        else copy(nanoError = null, nanoIds = generateNanoIds(nanoLength.toInt(), nanoCount.toInt()), copied = null)
    }

    fun markCopied(section: String) = setState { copy(copied = section) }
}

@Suppress("DEPRECATION")
@Composable
internal fun UuidIdGeneratorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val viewModel: UuidIdViewModel = viewModel(initializer = { UuidIdViewModel() })
    val state by viewModel.collectAsState()
    val clipboard = LocalClipboardManager.current
    val copy = { section: String, values: List<String> ->
        if (values.isNotEmpty()) {
            runCatching { clipboard.setText(AnnotatedString(values.joinToString("\n"))) }
                .onSuccess { viewModel.markCopied(section) }
        }
    }
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(PageBackground).safeContentPadding(),
    ) {
        val horizontalPadding = if (maxWidth >= 900.dp) 32.dp else 16.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            topBar = { PageTitleBar(title = "UUID / ID 生成器", horizontalPadding = horizontalPadding, onBack = onBack) },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 1120.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(text = "生成常用的唯一标识符", style = MaterialTheme.typography.headlineSmall.copy(color = Ink, fontWeight = FontWeight.Bold))
                    Text(text = "所有数据仅在当前设备本地处理。选择格式后批量生成，可直接复制结果。", style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk))
                    UuidCard(state = state, onCountChange = viewModel::onUuidCountChange, onFormatChange = viewModel::onUuidFormatChange, onGenerate = viewModel::generateUuids, onCopy = { copy("uuid", state.uuids) }, copied = state.copied == "uuid")
                    SnowflakeCard(state = state, onMachineChange = viewModel::onMachineIdChange, onDatacenterChange = viewModel::onDatacenterIdChange, onCountChange = viewModel::onSnowflakeCountChange, onGenerate = viewModel::generateSnowflakes, onCopy = { copy("snowflake", state.snowflakes) }, copied = state.copied == "snowflake")
                    ParseCard(state = state, onInputChange = viewModel::onSnowflakeInputChange, onParse = viewModel::parseSnowflake)
                    NanoIdCard(state = state, onLengthChange = viewModel::onNanoLengthChange, onCountChange = viewModel::onNanoCountChange, onGenerate = viewModel::generateNanoIds, onCopy = { copy("nano", state.nanoIds) }, copied = state.copied == "nano")
                }
            }
        }
    }
}

@Composable
private fun UuidCard(modifier: Modifier = Modifier, state: UuidIdState, onCountChange: (String) -> Unit, onFormatChange: (UuidFormat) -> Unit, onGenerate: () -> Unit, onCopy: () -> Unit, copied: Boolean) {
    SectionCard(modifier = modifier.fillMaxWidth(), title = "UUID v4 生成", description = "生成符合 RFC 4122 的随机 UUID") {
        BoxWithConstraints {
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberInput(modifier = Modifier.fillMaxWidth(), label = "生成数量", value = state.uuidCount, onValueChange = onCountChange, error = state.uuidError)
                    SelectField(modifier = Modifier.fillMaxWidth(), label = "格式", value = state.uuidFormat.label, options = UuidFormat.entries.map { it.label }, onOptionSelected = { label -> onFormatChange(UuidFormat.entries.first { it.label == label }) })
                    ActionText(text = "生成", onClick = onGenerate)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    NumberInput(modifier = Modifier.weight(0.8f), label = "生成数量", value = state.uuidCount, onValueChange = onCountChange, error = state.uuidError)
                    SelectField(modifier = Modifier.weight(1.5f), label = "格式", value = state.uuidFormat.label, options = UuidFormat.entries.map { it.label }, onOptionSelected = { label -> onFormatChange(UuidFormat.entries.first { it.label == label }) })
                    ActionText(text = "生成", onClick = onGenerate, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        ResultList(values = state.uuids, emptyText = "点击“生成”查看 UUID", copied = copied, onCopy = onCopy)
    }
}

@Composable
private fun SnowflakeCard(modifier: Modifier = Modifier, state: UuidIdState, onMachineChange: (String) -> Unit, onDatacenterChange: (String) -> Unit, onCountChange: (String) -> Unit, onGenerate: () -> Unit, onCopy: () -> Unit, copied: Boolean) {
    SectionCard(modifier = modifier.fillMaxWidth(), title = "Snowflake ID 生成", description = "按时间、数据中心、机器和序列号组合生成 64 位 ID") {
        BoxWithConstraints {
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberInput(modifier = Modifier.fillMaxWidth(), label = "机器 ID", value = state.machineId, onValueChange = onMachineChange)
                    NumberInput(modifier = Modifier.fillMaxWidth(), label = "数据中心 ID", value = state.datacenterId, onValueChange = onDatacenterChange)
                    NumberInput(modifier = Modifier.fillMaxWidth(), label = "数量", value = state.snowflakeCount, onValueChange = onCountChange, error = state.snowflakeError)
                    ActionText(text = "生成", onClick = onGenerate)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    NumberInput(modifier = Modifier.weight(1f), label = "机器 ID", value = state.machineId, onValueChange = onMachineChange)
                    NumberInput(modifier = Modifier.weight(1.1f), label = "数据中心 ID", value = state.datacenterId, onValueChange = onDatacenterChange)
                    NumberInput(modifier = Modifier.weight(0.8f), label = "数量", value = state.snowflakeCount, onValueChange = onCountChange, error = state.snowflakeError)
                    ActionText(text = "生成", onClick = onGenerate, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        ResultList(values = state.snowflakes, emptyText = "点击“生成”查看 Snowflake ID", copied = copied, onCopy = onCopy)
    }
}

@Composable
private fun ParseCard(modifier: Modifier = Modifier, state: UuidIdState, onInputChange: (String) -> Unit, onParse: () -> Unit) {
    SectionCard(modifier = modifier.fillMaxWidth(), title = "Snowflake ID 解析", description = "解析时间戳、数据中心、机器和序列号字段") {
        BoxWithConstraints {
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SnowflakeInput(state = state, onInputChange = onInputChange)
                    ActionText(text = "解析", onClick = onParse)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    SnowflakeInput(state = state, onInputChange = onInputChange, modifier = Modifier.weight(1f))
                    ActionText(text = "解析", onClick = onParse, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        state.snowflakeParts?.let { parts ->
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ParseValue(label = "生成时间", value = parts.dateTime)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ParseValue(label = "数据中心", value = parts.datacenterId.toString())
                    ParseValue(label = "机器", value = parts.machineId.toString())
                    ParseValue(label = "序列号", value = parts.sequence.toString())
                }
            }
        }
    }
}

@Composable
private fun NanoIdCard(modifier: Modifier = Modifier, state: UuidIdState, onLengthChange: (String) -> Unit, onCountChange: (String) -> Unit, onGenerate: () -> Unit, onCopy: () -> Unit, copied: Boolean) {
    SectionCard(modifier = modifier.fillMaxWidth(), title = "NanoID 生成", description = "生成 URL 安全、紧凑且适合前端使用的随机 ID") {
        BoxWithConstraints {
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberInput(modifier = Modifier.fillMaxWidth(), label = "长度", value = state.nanoLength, onValueChange = onLengthChange, error = state.nanoError)
                    NumberInput(modifier = Modifier.fillMaxWidth(), label = "数量", value = state.nanoCount, onValueChange = onCountChange)
                    ActionText(text = "生成", onClick = onGenerate)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    NumberInput(modifier = Modifier.weight(1f), label = "长度", value = state.nanoLength, onValueChange = onLengthChange, error = state.nanoError)
                    NumberInput(modifier = Modifier.weight(1f), label = "数量", value = state.nanoCount, onValueChange = onCountChange)
                    ActionText(text = "生成", onClick = onGenerate, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        ResultList(values = state.nanoIds, emptyText = "点击“生成”查看 NanoID", copied = copied, onCopy = onCopy)
    }
}

@Composable
private fun NumberInput(modifier: Modifier, label: String, value: String, onValueChange: (String) -> Unit, error: String? = null) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = modifier, label = { Text(text = label) }, isError = error != null, supportingText = error?.let { { Text(text = it) } }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

@Composable
private fun SnowflakeInput(modifier: Modifier = Modifier, state: UuidIdState, onInputChange: (String) -> Unit) {
    OutlinedTextField(value = state.snowflakeInput, onValueChange = onInputChange, modifier = modifier.fillMaxWidth(), label = { Text(text = "Snowflake ID") }, placeholder = { Text(text = "输入数字 ID") }, isError = state.parseError != null, supportingText = state.parseError?.let { { Text(text = it) } }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SelectField(modifier: Modifier, label: String, value: String, options: List<String>, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text(text = label) },
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ResultList(modifier: Modifier = Modifier, values: List<String>, emptyText: String, copied: Boolean, onCopy: () -> Unit) {
    Card(modifier = modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9E1EE))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = if (values.isEmpty()) emptyText else "生成结果（${values.size}）", style = MaterialTheme.typography.labelLarge.copy(color = MutedInk))
                if (values.isNotEmpty()) ActionText(modifier = Modifier, text = "复制", onClick = onCopy)
            }
            if (values.isNotEmpty()) {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        values.forEach { value -> Text(text = value, softWrap = true, style = TextStyle(color = Ink, fontFamily = FontFamily.Monospace)) }
                    }
                }
                Text(text = if (copied) "已复制到剪贴板" else "可选择文本或点击右上角复制", style = MaterialTheme.typography.labelSmall.copy(color = if (copied) SuccessGreen else MutedInk))
            }
        }
    }
}

@Composable
private fun ParseValue(modifier: Modifier = Modifier, label: String, value: String) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = MutedInk))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(color = Ink, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun ActionText(modifier: Modifier = Modifier, text: String, onClick: () -> Unit) {
    Text(text = text, modifier = modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp)).background(BrandBlue).clickable(role = Role.Button, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 18.dp, vertical = 14.dp), style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
}
