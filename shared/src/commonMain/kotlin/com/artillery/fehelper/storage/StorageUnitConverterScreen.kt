package com.artillery.fehelper.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.AnnotatedString
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

private data class StorageState(
    val input: String = "2048",
    val source: StorageUnit = StorageUnit.MB,
    val target: StorageUnit = StorageUnit.GB,
    val error: String? = null,
    val copied: Boolean = false,
) {
    val converted: Double?
        get() = input.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
            ?.let { convertStorageValue(it, source, target) }
            ?.takeIf(Double::isFinite)

    val allValues: List<Pair<StorageUnit, String>>
        get() = input.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
            ?.let { value -> StorageUnit.entries.map { unit -> unit to formatStorageValue(convertStorageValue(value, source, unit)) } }
            .orEmpty()

    val converter: StorageConverterState
        get() = StorageConverterState(input = input, source = source, target = target, error = error)

    val result: StorageResultState
        get() = StorageResultState(
            text = converted?.let { "$input ${source.label} = ${formatStorageValue(it)} ${target.label}" },
            copied = copied,
        )

    val table: StorageTableState
        get() = StorageTableState(values = allValues, target = target, copied = copied)
}

private data class StorageConverterState(val input: String, val source: StorageUnit, val target: StorageUnit, val error: String?)
private data class StorageResultState(val text: String?, val copied: Boolean)
private data class StorageTableState(val values: List<Pair<StorageUnit, String>>, val target: StorageUnit, val copied: Boolean)

private class StorageViewModel : StateViewModel<StorageState>(initialState = StorageState()) {
    fun onInputChange(value: String) {
        setState { copy(input = value, error = null, copied = false) }
    }

    fun onSourceChange(value: StorageUnit) {
        setState { copy(source = value, error = null, copied = false) }
    }

    fun onTargetChange(value: StorageUnit) {
        setState { copy(target = value, error = null, copied = false) }
    }

    fun swapUnits() {
        setState { copy(source = target, target = source, error = null, copied = false) }
    }

    fun applyExample(input: String, source: StorageUnit, target: StorageUnit) {
        setState { copy(input = input, source = source, target = target, error = null, copied = false) }
    }

    fun convert() {
        setState {
            val value = input.toDoubleOrNull()
            val message = when {
                value == null || !value.isFinite() -> "请输入有效的数值"
                value < 0.0 -> "存储容量不能小于 0"
                !convertStorageValue(value, source, target).isFinite() -> "数值超出可换算范围"
                else -> null
            }
            copy(error = message)
        }
    }

    fun markCopied() {
        setState { copy(copied = true) }
    }
}

@Suppress("DEPRECATION")
@Composable
internal fun StorageUnitConverterScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val viewModel: StorageViewModel = viewModel(initializer = { StorageViewModel() })
    val converterState by viewModel.collectAsState(StorageState::converter)
    val resultState by viewModel.collectAsState(StorageState::result)
    val tableState by viewModel.collectAsState(StorageState::table)
    val clipboard = LocalClipboardManager.current
    val copyText: (String) -> Unit = { text ->
        runCatching { clipboard.setText(AnnotatedString(text)) }
            .onSuccess { viewModel.markCopied() }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(PageBackground).safeContentPadding()) {
        val wide = maxWidth >= 900.dp
        val horizontalPadding = if (wide) 32.dp else 16.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            topBar = { PageTitleBar(title = "存储单位转换", horizontalPadding = horizontalPadding, onBack = onBack) },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 1200.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(text = "存储单位换算", style = MaterialTheme.typography.headlineSmall.copy(color = Ink, fontWeight = FontWeight.Bold))
                    Text(text = "在 B、KB、MB、GB 与 TB 之间快速换算，使用 1024 进位。", style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk))
                    if (wide) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                            ConverterCard(modifier = Modifier.weight(1.2f), state = converterState, onInputChange = viewModel::onInputChange, onSourceChange = viewModel::onSourceChange, onTargetChange = viewModel::onTargetChange, onSwap = viewModel::swapUnits, onConvert = viewModel::convert, onExample = viewModel::applyExample)
                            MainResultCard(modifier = Modifier.weight(0.8f), state = resultState, onCopy = { resultState.text?.let(copyText) })
                        }
                    } else {
                        ConverterCard(modifier = Modifier.fillMaxWidth(), state = converterState, onInputChange = viewModel::onInputChange, onSourceChange = viewModel::onSourceChange, onTargetChange = viewModel::onTargetChange, onSwap = viewModel::swapUnits, onConvert = viewModel::convert, onExample = viewModel::applyExample)
                        MainResultCard(modifier = Modifier.fillMaxWidth(), state = resultState, onCopy = { resultState.text?.let(copyText) })
                    }
                    AllUnitsCard(state = tableState, onValueCopy = { unit, value -> copyText("$value ${unit.label}") })
                }
            }
        }
    }
}

@Composable
private fun ConverterCard(
    modifier: Modifier = Modifier,
    state: StorageConverterState,
    onInputChange: (String) -> Unit,
    onSourceChange: (StorageUnit) -> Unit,
    onTargetChange: (StorageUnit) -> Unit,
    onSwap: () -> Unit,
    onConvert: () -> Unit,
    onExample: (String, StorageUnit, StorageUnit) -> Unit,
) {
    SectionCard(modifier = modifier.fillMaxWidth(), title = "存储单位换算", description = "输入任意数值，选择来源单位和目标单位") {
        BoxWithConstraints {
            if (maxWidth < 680.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StorageInput(modifier = Modifier.fillMaxWidth(), value = state.input, error = state.error, onValueChange = onInputChange)
                    StorageUnitDropdown(modifier = Modifier.fillMaxWidth(), label = "来源单位", value = state.source, onValueChange = onSourceChange)
                    StorageUnitDropdown(modifier = Modifier.fillMaxWidth(), label = "目标单位", value = state.target, onValueChange = onTargetChange)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    StorageInput(modifier = Modifier.weight(1.2f), value = state.input, error = state.error, onValueChange = onInputChange)
                    StorageUnitDropdown(modifier = Modifier.weight(1f), label = "来源单位", value = state.source, onValueChange = onSourceChange)
                    StorageUnitDropdown(modifier = Modifier.weight(1f), label = "目标单位", value = state.target, onValueChange = onTargetChange)
                }
            }
        }
        FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionText(text = "立即换算", primary = true, onClick = onConvert)
            ActionText(text = "交换单位", onClick = onSwap)
            ActionText(text = "填入示例", onClick = { onExample("2048", StorageUnit.MB, StorageUnit.GB) })
            ActionText(text = "清空", onClick = { onInputChange("") })
        }
        FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExampleChip(text = "1024 MB → GB", onClick = { onExample("1024", StorageUnit.MB, StorageUnit.GB) })
            ExampleChip(text = "2048 B → KB", onClick = { onExample("2048", StorageUnit.B, StorageUnit.KB) })
            ExampleChip(text = "1.5 GB → MB", onClick = { onExample("1.5", StorageUnit.GB, StorageUnit.MB) })
        }
    }
}

@Composable
private fun StorageInput(modifier: Modifier = Modifier, value: String, error: String?, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(text = "输入数值") },
        isError = error != null,
        supportingText = error?.let { { Text(text = it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageUnitDropdown(modifier: Modifier = Modifier, label: String, value: StorageUnit, onValueChange: (StorageUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = value.label,
            onValueChange = {},
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            label = { Text(text = label) },
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            StorageUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(text = unit.label) },
                    onClick = {
                        onValueChange(unit)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun MainResultCard(modifier: Modifier = Modifier, state: StorageResultState, onCopy: () -> Unit) {
    SectionCard(modifier = modifier.fillMaxWidth(), title = "换算结果", description = "结果支持一键复制，方便贴到文档或工单") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(width = 1.dp, color = Color(0xFFD9E1EE), shape = RoundedCornerShape(8.dp)),
        ) {
            BoxWithConstraints {
                val resultText = state.text ?: "请输入有效数值"
                if (maxWidth < 560.dp) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = resultText, style = MaterialTheme.typography.titleMedium.copy(color = if (state.text == null) MutedInk else BrandBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                        if (state.text != null) ActionText(text = "复制结果", onClick = onCopy)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = resultText, style = MaterialTheme.typography.titleMedium.copy(color = if (state.text == null) MutedInk else BrandBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                        if (state.text != null) ActionText(text = "复制结果", onClick = onCopy)
                    }
                }
            }
        }
        if (state.copied) Text(text = "已复制到剪贴板", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium.copy(color = SuccessGreen))
    }
}

@Composable
private fun AllUnitsCard(state: StorageTableState, onValueCopy: (StorageUnit, String) -> Unit) {
    SectionCard(modifier = Modifier.fillMaxWidth(), title = "全部单位", description = "点击任意结果即可复制对应数值和单位") {
        Column(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White)) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F7FB)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(text = "单位", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium.copy(color = MutedInk, fontWeight = FontWeight.Bold))
                Text(text = "值", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium.copy(color = MutedInk, fontWeight = FontWeight.Bold))
            }
            state.values.forEach { (unit, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (unit == state.target) Color(0xFFEAF0FF) else Color.White)
                        .clickable(role = Role.Button) { onValueCopy(unit, value) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(text = unit.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall.copy(color = Ink))
                    Text(text = value, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall.copy(color = BrandBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                }
            }
        }
        if (state.copied) Text(text = "已复制", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen))
    }
}

@Composable
private fun ExampleChip(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF5F7FB))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        style = MaterialTheme.typography.labelSmall.copy(color = MutedInk, fontWeight = FontWeight.Medium),
    )
}

@Composable
private fun ActionText(text: String, primary: Boolean = false, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) BrandBlue else Color(0xFFF0F3F8))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelMedium.copy(color = if (primary) Color.White else Ink, fontWeight = FontWeight.SemiBold),
    )
}
