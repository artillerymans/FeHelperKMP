package com.artillery.fehelper.json

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val formatterJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

internal data class JsonTable(
    val columns: List<String>,
    val rows: List<List<String>>,
)

internal fun parseJson(text: String): JsonElement = Json.parseToJsonElement(text)

internal fun formatJson(element: JsonElement): String = formatterJson.encodeToString(element)

internal fun sortJson(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(element.entries.sortedBy { it.key }.associate { (key, value) -> key to sortJson(value) })
    is JsonArray -> JsonArray(element.map(::sortJson))
    else -> element
}

internal fun decodeJsonText(element: JsonElement): String {
    if (element is JsonPrimitive && element.isString) {
        val nested = runCatching { parseJson(element.content) }.getOrNull()
        return nested?.let(::formatJson) ?: element.content
    }
    return formatJson(element)
}

internal fun jsonTable(element: JsonElement): JsonTable {
    if (element is JsonArray && element.isNotEmpty() && element.all { it is JsonObject }) {
        val objects = element.map { it as JsonObject }
        val columns = objects.flatMap { it.keys }.distinct()
        return JsonTable(
            columns = columns,
            rows = objects.map { row -> columns.map { key -> tableValue(row[key]) } },
        )
    }

    if (element is JsonObject) {
        val columns = element.keys.toList()
        return JsonTable(columns = columns, rows = listOf(columns.map { tableValue(element[it]) }))
    }

    val values = if (element is JsonArray) element.toList() else listOf(element)
    return JsonTable(columns = listOf("value"), rows = values.map { listOf(tableValue(it)) })
}

private fun tableValue(element: JsonElement?): String = when (element) {
    null, JsonNull -> ""
    is JsonPrimitive -> element.content
    else -> formatJson(element).replace('\n', ' ')
}
