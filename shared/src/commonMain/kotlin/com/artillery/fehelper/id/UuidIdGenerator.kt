package com.artillery.fehelper.id

import kotlin.time.Clock

internal enum class UuidFormat(val label: String) {
    STANDARD("标准（带连字符）"),
    COMPACT("无连字符"),
    UPPERCASE("大写"),
    BRACED("带花括号"),
}

internal data class SnowflakeParts(
    val timestamp: Long,
    val dateTime: String,
    val machineId: Long,
    val datacenterId: Long,
    val sequence: Long,
)

internal const val MAX_ID_COUNT = 100
internal const val MAX_NANO_ID_LENGTH = 128
internal const val MAX_SNOWFLAKE_MACHINE_ID = 31
internal const val MAX_SNOWFLAKE_DATACENTER_ID = 31

internal expect fun secureRandomInt(bound: Int): Int

internal fun generateUuids(count: Int, format: UuidFormat): List<String> = List(count) {
    val bytes = ByteArray(16).also { bytes ->
        bytes.indices.forEach { index -> bytes[index] = secureRandomInt(256).toByte() }
    }
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
    val compact = bytes.joinToString(separator = "") { byte ->
        (byte.toInt() and 0xFF).toString(radix = 16).padStart(length = 2, padChar = '0')
    }
    val standard = buildString(capacity = 36) {
        append(compact, 0, 8)
        append('-')
        append(compact, 8, 12)
        append('-')
        append(compact, 12, 16)
        append('-')
        append(compact, 16, 20)
        append('-')
        append(compact, 20, 32)
    }
    when (format) {
        UuidFormat.STANDARD -> standard
        UuidFormat.COMPACT -> compact
        UuidFormat.UPPERCASE -> standard.uppercase()
        UuidFormat.BRACED -> "{$standard}"
    }
}

private const val SNOWFLAKE_EPOCH = 1609459200000L

internal fun generateSnowflakes(
    machineId: Long,
    datacenterId: Long,
    count: Int,
    timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    sequenceStart: Long = 0,
): List<Long> {
    val timestampBits = (timestamp - SNOWFLAKE_EPOCH).coerceAtLeast(0L)
    return List(count) { index ->
        (timestampBits shl 22) or
            ((datacenterId and 0x1F) shl 17) or
            ((machineId and 0x1F) shl 12) or
            ((sequenceStart + index) and 0xFFFL)
    }
}

internal fun parseSnowflake(value: String): SnowflakeParts? {
    val id = value.trim().toLongOrNull() ?: return null
    if (id < 0) return null
    val timestamp = (id ushr 22) + SNOWFLAKE_EPOCH
    val machineId = (id ushr 12) and 0x1F
    val datacenterId = (id ushr 17) and 0x1F
    val sequence = id and 0xFFFL
    return SnowflakeParts(
        timestamp = timestamp,
        dateTime = formatTimestamp(timestamp),
        machineId = machineId,
        datacenterId = datacenterId,
        sequence = sequence,
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = kotlin.time.Instant.fromEpochMilliseconds(timestamp)
    return instant.toString()
}

private const val NANO_ID_ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

internal fun generateNanoIds(length: Int, count: Int): List<String> = List(count) {
    buildString(capacity = length) {
        repeat(length) {
            append(NANO_ID_ALPHABET[secureRandomInt(NANO_ID_ALPHABET.length)])
        }
    }
}

internal fun validatePositiveRange(input: String, min: Int, max: Int, label: String): String? {
    val value = input.toIntOrNull()
    return when {
        value == null -> "${label}请输入数字"
        value !in min..max -> "${label}需在 $min-$max 之间"
        else -> null
    }
}

internal fun validateSnowflakeId(input: String): String? = when {
    input.isBlank() -> "请输入 Snowflake ID"
    input.trim().toLongOrNull() == null -> "请输入有效的数字 ID"
    else -> null
}
