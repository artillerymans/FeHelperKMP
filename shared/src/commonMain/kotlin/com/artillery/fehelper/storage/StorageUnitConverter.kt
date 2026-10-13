package com.artillery.fehelper.storage

internal enum class StorageUnit(val label: String, val bytes: Double) {
    B("B", 1.0),
    KB("KB", 1024.0),
    MB("MB", 1024.0 * 1024.0),
    GB("GB", 1024.0 * 1024.0 * 1024.0),
    TB("TB", 1024.0 * 1024.0 * 1024.0 * 1024.0),
}

internal fun convertStorageValue(value: Double, source: StorageUnit, target: StorageUnit): Double =
    value * source.bytes / target.bytes

internal fun formatStorageValue(value: Double): String {
    if (!value.isFinite()) return "超出可表示范围"
    val rounded = kotlin.math.round(value * 1_000_000.0) / 1_000_000.0
    return rounded.toString().removeSuffix(".0")
}
