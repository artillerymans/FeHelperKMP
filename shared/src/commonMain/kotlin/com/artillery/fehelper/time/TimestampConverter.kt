package com.artillery.fehelper.time

import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal enum class TimestampUnit(val label: String) {
    SECONDS("秒"),
    MILLISECONDS("毫秒"),
}

internal data class TimestampConversion(
    val localTime: String? = null,
    val seconds: String? = null,
    val milliseconds: String? = null,
    val error: String? = null,
)

internal data class TimeSnapshot(
    val localTime: String,
    val seconds: String,
    val milliseconds: String,
)

internal data class WorldClock(
    val offsetHours: Int,
    val label: String,
    val location: String,
    val localTime: String,
)

private val ShanghaiTimeZone = TimeZone.of("Asia/Shanghai")
private val WorldClockLocations = listOf(
    "贝克岛",
    "帕果帕果",
    "檀香山",
    "安克雷奇",
    "洛杉矶",
    "丹佛",
    "芝加哥",
    "纽约",
    "圣地亚哥",
    "圣保罗",
    "费尔南多-迪诺罗尼亚",
    "亚速尔群岛",
    "伦敦",
    "巴黎",
    "开罗",
    "莫斯科",
    "迪拜",
    "卡拉奇",
    "达卡",
    "曼谷",
    "北京",
    "东京",
    "悉尼",
    "努美阿",
    "奥克兰",
)
private val LocalTimePattern = Regex(
    "(\\d{4})(?:-(\\d{1,2})(?:-(\\d{1,2})(?:[ T]+(\\d{1,2})(?::(\\d{1,2})(?::(\\d{1,2}))?)?)?)?)?",
)
private val TimestampPattern = Regex("[+-]?\\d+")

internal fun timestampToLocalTime(value: String, unit: TimestampUnit): TimestampConversion {
    val input = value.trim()
    if (input.isEmpty()) return TimestampConversion(error = "请输入时间戳")
    if (!TimestampPattern.matches(input)) return TimestampConversion(error = "时间戳请输入整数")

    val number = input.toLongOrNull() ?: return TimestampConversion(error = "时间戳超出范围")
    val instant = runCatching {
        when (unit) {
            TimestampUnit.SECONDS -> Instant.fromEpochSeconds(number)
            TimestampUnit.MILLISECONDS -> Instant.fromEpochMilliseconds(number)
        }
    }.getOrNull() ?: return TimestampConversion(error = "时间戳超出有效时间范围")

    return conversionFor(instant = instant)
}

internal fun localTimeToTimestamp(value: String): TimestampConversion {
    val input = value.trim()
    if (input.isEmpty()) return TimestampConversion(error = "请输入本地时间")
    val match = LocalTimePattern.matchEntire(input)
        ?: return TimestampConversion(error = "请输入 yyyy、yyyy-MM、yyyy-MM-dd 或 yyyy-MM-dd HH:mm[:ss] 格式")

    val instant = runCatching {
        LocalDateTime(
            year = match.groupValues[1].toInt(),
            month = match.groupValues[2].ifEmpty { "1" }.toInt(),
            day = match.groupValues[3].ifEmpty { "1" }.toInt(),
            hour = match.groupValues[4].ifEmpty { "0" }.toInt(),
            minute = match.groupValues[5].ifEmpty { "0" }.toInt(),
            second = match.groupValues[6].ifEmpty { "0" }.toInt(),
        ).toInstant(ShanghaiTimeZone)
    }.getOrNull() ?: return TimestampConversion(error = "本地时间无效")

    return conversionFor(instant = instant)
}

internal fun nowSnapshot(now: Instant = Clock.System.now()): TimeSnapshot {
    val localTime = now.toLocalDateTime(ShanghaiTimeZone).formatForDisplay()
    return TimeSnapshot(
        localTime = localTime,
        seconds = now.epochSeconds.toString(),
        milliseconds = now.toEpochMilliseconds().toString(),
    )
}

internal fun worldClocks(now: Instant = Clock.System.now()): List<WorldClock> =
    (-12..12).map { offsetHours ->
        val sign = if (offsetHours >= 0) "+" else "-"
        WorldClock(
            offsetHours = offsetHours,
            label = "GMT$sign${abs(offsetHours)}",
            location = WorldClockLocations[offsetHours + 12],
            localTime = now.toLocalDateTime(UtcOffset(hours = offsetHours).asTimeZone()).formatForDisplay(),
        )
    }

private fun conversionFor(instant: Instant): TimestampConversion = TimestampConversion(
    localTime = instant.toLocalDateTime(ShanghaiTimeZone).formatForDisplay(),
    seconds = instant.epochSeconds.toString(),
    milliseconds = instant.toEpochMilliseconds().toString(),
)

private fun LocalDateTime.formatForDisplay(): String =
    "${year.toString().padStart(4, '0')}-${month.number.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} " +
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:${second.toString().padStart(2, '0')}"
