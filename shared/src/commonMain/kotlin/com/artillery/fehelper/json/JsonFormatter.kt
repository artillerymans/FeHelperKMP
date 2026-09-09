package com.artillery.fehelper.json

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.state.StateViewModel
import com.artillery.state.collectAsState
import kotlinx.serialization.json.JsonElement

private enum class JsonView {
    JSON,
    TABLE,
}

private data class JsonOutputState(
    val value: String,
    val error: String?,
    val table: JsonTable?,
    val view: JsonView,
)

private data class JsonFormatterState(
    val rawJson: String = "",
    val formattedJson: String = "",
    val parsedElement: JsonElement? = null,
    val table: JsonTable? = null,
    val error: String? = null,
    val view: JsonView = JsonView.JSON,
) {
    val output: JsonOutputState
        get() = JsonOutputState(
            value = formattedJson,
            error = error,
            table = table,
            view = view,
        )
}

private sealed interface JsonFormatterEvent {
    data class InputChanged(val value: String) : JsonFormatterEvent
    object Format : JsonFormatterEvent
    object Sort : JsonFormatterEvent
    object Decode : JsonFormatterEvent
    data class ViewChanged(val view: JsonView) : JsonFormatterEvent
}

private class JsonFormatterViewModel : StateViewModel<JsonFormatterState>(initialState = JsonFormatterState()) {
    fun onEvent(event: JsonFormatterEvent) {
        when (event) {
            is JsonFormatterEvent.InputChanged -> updateFromInput(value = event.value)
            JsonFormatterEvent.Format -> parsedElementAction { state, element -> show(state = state, element = element) }
            JsonFormatterEvent.Sort -> parsedElementAction { state, element ->
                show(state = state, element = sortJson(element = element))
            }
            JsonFormatterEvent.Decode -> parsedElementAction { state, element ->
                show(state = state, element = element, text = decodeJsonText(element = element))
            }
            is JsonFormatterEvent.ViewChanged -> setState { copy(view = event.view) }
        }
    }

    private fun updateFromInput(value: String) {
        val parsed = runCatching { parseJson(text = value) }.getOrNull()
        setState {
            if (parsed == null) {
                copy(
                    rawJson = value,
                    formattedJson = "",
                    parsedElement = null,
                    table = null,
                    error = "JSON 格式无效",
                    view = JsonView.JSON,
                )
            } else {
                copy(
                    rawJson = value,
                    formattedJson = formatJson(element = parsed),
                    parsedElement = parsed,
                    table = jsonTable(element = parsed),
                    error = null,
                    view = JsonView.JSON,
                )
            }
        }
    }

    private fun parsedElementAction(action: (JsonFormatterState, JsonElement) -> JsonFormatterState) {
        setState { parsedElement?.let { element -> action(this, element) } ?: this }
    }

    private fun show(
        state: JsonFormatterState,
        element: JsonElement,
        text: String = formatJson(element = element),
    ): JsonFormatterState = state.copy(
        table = jsonTable(element = element),
        formattedJson = text,
        error = null,
        view = JsonView.JSON,
    )
}

@Composable
internal fun JsonFormatterScreen(onBack: () -> Unit) {
    val viewModel: JsonFormatterViewModel = viewModel(initializer = { JsonFormatterViewModel() })
    val rawJson by viewModel.collectAsState(JsonFormatterState::rawJson)
    val outputState by viewModel.collectAsState(JsonFormatterState::output)

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
                    title = "JSON 格式化",
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
                ) {
                    JsonToolbar(
                        view = outputState.view,
                        onEvent = viewModel::onEvent,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            JsonEditorPanel(
                                title = "原始 JSON",
                                value = rawJson,
                                onValueChange = { value -> viewModel.onEvent(JsonFormatterEvent.InputChanged(value = value)) },
                                modifier = Modifier.weight(1f),
                                height = 560.dp,
                            )
                            JsonOutputPanel(
                                title = "格式化结果",
                                state = outputState,
                                modifier = Modifier.weight(1f),
                                height = 560.dp,
                            )
                        }
                    } else {
                        JsonEditorPanel(
                            title = "原始 JSON",
                            value = rawJson,
                            onValueChange = { value -> viewModel.onEvent(JsonFormatterEvent.InputChanged(value = value)) },
                            modifier = Modifier.fillMaxWidth(),
                            height = 360.dp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        JsonOutputPanel(
                            title = "格式化结果",
                            state = outputState,
                            modifier = Modifier.fillMaxWidth(),
                            height = 360.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JsonToolbar(
    view: JsonView,
    onEvent: (JsonFormatterEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "格式化",
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(BrandBlue, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = { onEvent(JsonFormatterEvent.Format) })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
            )
            Text(
                text = "排序",
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(BrandBlue, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = { onEvent(JsonFormatterEvent.Sort) })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
            )
            Text(
                text = "解码",
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(BrandBlue, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = { onEvent(JsonFormatterEvent.Decode) })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = view == JsonView.JSON,
                onClick = { onEvent(JsonFormatterEvent.ViewChanged(view = JsonView.JSON)) },
                label = { Text(text = "JSON") },
                elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp),
            )
            FilterChip(
                selected = view == JsonView.TABLE,
                onClick = { onEvent(JsonFormatterEvent.ViewChanged(view = JsonView.TABLE)) },
                label = { Text(text = "表格") },
                elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp),
            )
        }
    }
}

@Composable
private fun JsonEditorPanel(
    modifier: Modifier,
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    height: Dp,
) {
    JsonPanel(
        modifier = modifier.height(height),
        title = title,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp).weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            placeholder = { Text(text = "粘贴 JSON") },
        )
    }
}

@Composable
private fun JsonOutputPanel(
    modifier: Modifier,
    title: String,
    state: JsonOutputState,
    height: Dp,
) {
    JsonPanel(
        modifier = modifier.height(height),
        title = title,
    ) {
        if (state.view == JsonView.TABLE && state.table != null) {
            JsonTableView(modifier = Modifier.weight(1f), table = state.table)
        } else {
            OutlinedTextField(
                value = state.value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp).weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                isError = state.error != null,
            )
        }
        state.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error))
        }
    }
}

@Composable
private fun JsonPanel(
    modifier: Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(color = Ink))
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun JsonTableView(modifier: Modifier, table: JsonTable) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .verticalScroll(rememberScrollState()),
    ) {
        Row {
            table.columns.forEach { column ->
                Text(
                    text = column,
                    modifier = Modifier.width(160.dp).padding(12.dp),
                    style = MaterialTheme.typography.labelLarge.copy(color = Ink),
                )
            }
        }
        HorizontalDivider(color = Border)
        table.rows.forEach { row ->
            Row {
                row.forEach { value ->
                    Text(
                        text = value,
                        modifier = Modifier.width(160.dp).padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = MutedInk),
                    )
                }
            }
            HorizontalDivider(color = Border)
        }
    }
}
