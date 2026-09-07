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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import kotlinx.serialization.json.JsonElement

private enum class JsonView {
    JSON,
    TABLE,
}

@Composable
internal fun JsonFormatterScreen(onBack: () -> Unit) {
    var rawJson by remember { mutableStateOf("") }
    var formattedJson by remember { mutableStateOf("") }
    var displayElement by remember { mutableStateOf<JsonElement?>(null) }
    var parsedElement by remember { mutableStateOf<JsonElement?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var view by remember { mutableStateOf(JsonView.JSON) }

    fun updateFromInput(value: String) {
        rawJson = value
        val parsed = runCatching { parseJson(text = value) }.getOrNull()
        parsedElement = parsed
        displayElement = parsed
        if (parsed == null) {
            formattedJson = ""
            error = "JSON 格式无效"
        } else {
            formattedJson = formatJson(element = parsed)
            error = null
        }
        view = JsonView.JSON
    }

    fun show(element: JsonElement, text: String = formatJson(element)) {
        displayElement = element
        formattedJson = text
        error = null
        view = JsonView.JSON
    }

    fun format() {
        parsedElement?.let { show(element = it) } ?: Unit
    }

    fun sort() {
        parsedElement?.let { show(element = sortJson(element = it)) } ?: Unit
    }

    fun decode() {
        parsedElement?.let { show(element = it, text = decodeJsonText(element = it)) } ?: Unit
    }

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
                        onFormat = ::format,
                        onSort = ::sort,
                        onDecode = ::decode,
                        view = view,
                        onViewChange = { view = it },
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
                                onValueChange = ::updateFromInput,
                                modifier = Modifier.weight(1f),
                                height = 560.dp,
                            )
                            JsonOutputPanel(
                                title = "格式化结果",
                                value = formattedJson,
                                error = error,
                                element = displayElement,
                                view = view,
                                modifier = Modifier.weight(1f),
                                height = 560.dp,
                            )
                        }
                    } else {
                        JsonEditorPanel(
                            title = "原始 JSON",
                            value = rawJson,
                            onValueChange = ::updateFromInput,
                            modifier = Modifier.fillMaxWidth(),
                            height = 360.dp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        JsonOutputPanel(
                            title = "格式化结果",
                            value = formattedJson,
                            error = error,
                            element = displayElement,
                            view = view,
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
    onFormat: () -> Unit,
    onSort: () -> Unit,
    onDecode: () -> Unit,
    view: JsonView,
    onViewChange: (JsonView) -> Unit,
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
                    .clickable(role = Role.Button, onClick = onFormat)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
            )
            Text(
                text = "排序",
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(BrandBlue, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onSort)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
            )
            Text(
                text = "解码",
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(BrandBlue, RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onDecode)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = view == JsonView.JSON,
                onClick = { onViewChange(JsonView.JSON) },
                label = { Text(text = "JSON") },
            )
            FilterChip(
                selected = view == JsonView.TABLE,
                onClick = { onViewChange(JsonView.TABLE) },
                label = { Text(text = "表格") },
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
    value: String,
    error: String?,
    element: JsonElement?,
    view: JsonView,
    height: Dp,
) {
    JsonPanel(
        modifier = modifier.height(height),
        title = title,
    ) {
        if (view == JsonView.TABLE && element != null) {
            JsonTableView(modifier = Modifier.weight(1f), table = jsonTable(element = element))
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp).weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                isError = error != null,
            )
        }
        error?.let {
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
