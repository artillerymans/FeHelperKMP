package com.artillery.fehelper.color

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class ColorFormat(val label: String) {
    HEX("HEX"),
    RGB("RGB"),
    HSL("HSL"),
    HSV("HSV"),
}

private data class ColorValue(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Float,
)

private data class ColorConverterViewState(
    val color: ColorValue,
    val hexInput: String,
    val rgbInput: String,
    val hslInput: String,
    val hsvInput: String,
    val error: String?,
)

private data class ColorConverterState(
    val color: ColorValue,
    val hexInput: String,
    val rgbInput: String,
    val hslInput: String,
    val hsvInput: String,
    val error: String? = null,
) {
    val view: ColorConverterViewState
        get() = ColorConverterViewState(
            color = color,
            hexInput = hexInput,
            rgbInput = rgbInput,
            hslInput = hslInput,
            hsvInput = hsvInput,
            error = error,
        )
}

private class ColorConverterViewModel : StateViewModel<ColorConverterState>(initialState = initialState()) {
    fun onInputChange(format: ColorFormat, value: String) {
        setState {
            val next = when (format) {
                ColorFormat.HEX -> copy(hexInput = value)
                ColorFormat.RGB -> copy(rgbInput = value)
                ColorFormat.HSL -> copy(hslInput = value)
                ColorFormat.HSV -> copy(hsvInput = value)
            }
            parseColor(format = format, value = value)?.let { parsed ->
                next.copy(color = parsed, error = null).withFormattedInputs()
            } ?: next.copy(error = "请输入有效的 ${format.label} 颜色值")
        }
    }

    fun onAlphaChange(value: Float) {
        setState { copy(color = color.copy(alpha = value.coerceIn(0f, 1f)), error = null).withFormattedInputs() }
    }

    private fun ColorConverterState.withFormattedInputs(): ColorConverterState = copy(
        hexInput = color.toHex(),
        rgbInput = color.toRgb(),
        hslInput = color.toHsl(),
        hsvInput = color.toHsv(),
    )
}

@Composable
internal fun ColorConverterScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val viewModel: ColorConverterViewModel = viewModel(initializer = { ColorConverterViewModel() })
    val state by viewModel.collectAsState(ColorConverterState::view)
    val clipboardManager = LocalClipboardManager.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .safeContentPadding(),
    ) {
        val wide = maxWidth >= 1200.dp
        val horizontalPadding = if (wide) 32.dp else 16.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            topBar = {
                PageTitleBar(
                    title = "颜色转换工具",
                    horizontalPadding = horizontalPadding,
                    onBack = onBack,
                )
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 1400.dp)
                        .fillMaxSize()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                Text(
                    text = "// 支持颜色值的 RGB/RGBA、HEX、HSL/HSLA、HSV/HSVA 互转。",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = TextStyle(color = MutedInk, fontSize = 12.sp),
                )
                AlphaControl(
                    alpha = state.color.alpha,
                    onAlphaChange = viewModel::onAlphaChange,
                )
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        ColorCard(
                            modifier = Modifier.weight(1f),
                            format = ColorFormat.HEX,
                            input = state.hexInput,
                            color = state.color,
                            error = state.error.takeIf { state.hexInput != state.color.toHex() },
                            onInputChange = { viewModel.onInputChange(ColorFormat.HEX, it) },
                            onCopy = { value -> clipboardManager.setText(AnnotatedString(value)) },
                        )
                        ColorCard(
                            modifier = Modifier.weight(1f),
                            format = ColorFormat.RGB,
                            input = state.rgbInput,
                            color = state.color,
                            error = state.error.takeIf { state.rgbInput != state.color.toRgb() },
                            onInputChange = { viewModel.onInputChange(ColorFormat.RGB, it) },
                            onCopy = { value -> clipboardManager.setText(AnnotatedString(value)) },
                        )
                        ColorCard(
                            modifier = Modifier.weight(1f),
                            format = ColorFormat.HSL,
                            input = state.hslInput,
                            color = state.color,
                            error = state.error.takeIf { state.hslInput != state.color.toHsl() },
                            onInputChange = { viewModel.onInputChange(ColorFormat.HSL, it) },
                            onCopy = { value -> clipboardManager.setText(AnnotatedString(value)) },
                        )
                        ColorCard(
                            modifier = Modifier.weight(1f),
                            format = ColorFormat.HSV,
                            input = state.hsvInput,
                            color = state.color,
                            error = state.error.takeIf { state.hsvInput != state.color.toHsv() },
                            onInputChange = { viewModel.onInputChange(ColorFormat.HSV, it) },
                            onCopy = { value -> clipboardManager.setText(AnnotatedString(value)) },
                        )
                    }
                } else {
                    listOf(
                        Triple(ColorFormat.HEX, state.hexInput, state.color.toHex()),
                        Triple(ColorFormat.RGB, state.rgbInput, state.color.toRgb()),
                        Triple(ColorFormat.HSL, state.hslInput, state.color.toHsl()),
                        Triple(ColorFormat.HSV, state.hsvInput, state.color.toHsv()),
                    ).forEach { (format, input, output) ->
                        ColorCard(
                            format = format,
                            input = input,
                            color = state.color,
                            error = state.error.takeIf { input != output },
                            onInputChange = { viewModel.onInputChange(format, it) },
                            onCopy = { value -> clipboardManager.setText(AnnotatedString(value)) },
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun AlphaControl(
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "全局透明度 (Alpha):", style = MaterialTheme.typography.labelLarge.copy(color = Ink))
        Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            valueRange = 0f..1f,
        )
        Text(text = alpha.format(2), style = MaterialTheme.typography.labelMedium.copy(color = MutedInk))
    }
}

@Composable
private fun ColorCard(
    modifier: Modifier = Modifier,
    format: ColorFormat,
    input: String,
    color: ColorValue,
    error: String?,
    onInputChange: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "${format.label} 输入", style = MaterialTheme.typography.titleSmall.copy(color = Ink, fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    label = { Text(text = format.label) },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(text = message) } },
                )
                Box(
                    modifier = Modifier
                        .background(color = color.toComposeColor(), shape = RoundedCornerShape(4.dp))
                        .heightIn(min = 40.dp)
                        .widthIn(min = 40.dp),
                )
            }
            Text(text = "输出：", style = MaterialTheme.typography.labelMedium.copy(color = MutedInk))
            ColorFormat.values().filter { it != format }.forEach { outputFormat ->
                val value = color.format(outputFormat)
                OutputRow(label = outputFormat.label, value = value, onCopy = onCopy)
            }
        }
    }
}

@Composable
private fun OutputRow(
    label: String,
    value: String,
    onCopy: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.widthIn(min = 32.dp), style = MaterialTheme.typography.labelSmall.copy(color = MutedInk))
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF5F7FB), RoundedCornerShape(5.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            style = TextStyle(color = Ink, fontSize = 11.sp),
        )
        Text(
            text = "复制",
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = { onCopy(value) })
                .padding(horizontal = 8.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelSmall.copy(color = BrandBlue, fontWeight = FontWeight.Bold),
        )
    }
}

private fun initialState(): ColorConverterState {
    val color = ColorValue(red = 67, green = 173, blue = 127, alpha = 127f / 255f)
    return ColorConverterState(
        color = color,
        hexInput = color.toHex(),
        rgbInput = color.toRgb(),
        hslInput = color.toHsl(),
        hsvInput = color.toHsv(),
    )
}

private fun ColorValue.toComposeColor(): Color = Color(
    red = red / 255f,
    green = green / 255f,
    blue = blue / 255f,
    alpha = alpha,
)

private fun ColorValue.toHex(): String = "#${red.toHex()}${green.toHex()}${blue.toHex()}${(alpha * 255f).toInt().coerceIn(0, 255).toHex()}"

private fun ColorValue.toRgb(): String = "rgba($red, $green, $blue, ${alpha.format(2)})"

private fun ColorValue.toHsl(): String {
    val hsl = rgbToHsl(this)
    return "hsla(${hsl.hue.roundToInt()}, ${(hsl.saturation * 100f).format(1)}%, ${(hsl.lightness * 100f).format(1)}%, ${alpha.format(2)})"
}

private fun ColorValue.toHsv(): String {
    val hsv = rgbToHsv(this)
    return "hsva(${hsv.hue.roundToInt()}, ${(hsv.saturation * 100f).format(1)}%, ${(hsv.value * 100f).format(1)}%, ${alpha.format(2)})"
}

private fun ColorValue.format(format: ColorFormat): String = when (format) {
    ColorFormat.HEX -> toHex()
    ColorFormat.RGB -> toRgb()
    ColorFormat.HSL -> toHsl()
    ColorFormat.HSV -> toHsv()
}

private fun Int.toHex(): String = toString(16).padStart(2, '0')

private fun Float.format(decimals: Int): String {
    val scale = when (decimals) {
        1 -> 10f
        2 -> 100f
        else -> 1000f
    }
    return ((this * scale).roundToInt() / scale).toString().let { value ->
        if (value.contains('.')) value.padEnd(value.indexOf('.') + decimals + 1, '0') else "$value.${"0".repeat(decimals)}"
    }
}

private fun parseColor(format: ColorFormat, value: String): ColorValue? = when (format) {
    ColorFormat.HEX -> parseHex(value)
    ColorFormat.RGB -> parseRgb(value)
    ColorFormat.HSL -> parseHsl(value)
    ColorFormat.HSV -> parseHsv(value)
}

private fun parseHex(value: String): ColorValue? {
    val hex = value.trim().removePrefix("#")
    val expanded = when (hex.length) {
        3 -> hex.map { "$it$it" }.joinToString("") + "ff"
        4 -> hex.map { "$it$it" }.joinToString("")
        6 -> "$hex" + "ff"
        8 -> hex
        else -> return null
    }
    if (!expanded.all { it in "0123456789abcdefABCDEF" }) return null
    return ColorValue(
        red = expanded.substring(0, 2).toInt(16),
        green = expanded.substring(2, 4).toInt(16),
        blue = expanded.substring(4, 6).toInt(16),
        alpha = expanded.substring(6, 8).toInt(16) / 255f,
    )
}

private fun parseRgb(value: String): ColorValue? {
    val parts = parseFunction(value, "rgba?", 3..4) ?: return null
    val red = parseChannel(parts[0], 255f) ?: return null
    val green = parseChannel(parts[1], 255f) ?: return null
    val blue = parseChannel(parts[2], 255f) ?: return null
    val alpha = if (parts.size == 4) parseAlpha(parts[3]) ?: return null else 1f
    return if (alpha in 0f..1f) ColorValue(red.roundToInt(), green.roundToInt(), blue.roundToInt(), alpha) else null
}

private fun parseHsl(value: String): ColorValue? {
    val parts = parseFunction(value, "hsla?", 3..4) ?: return null
    val hue = parts[0].removeSuffix("deg").trim().toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
    val saturation = parsePercent(parts[1]) ?: return null
    val lightness = parsePercent(parts[2]) ?: return null
    val alpha = if (parts.size == 4) parseAlpha(parts[3]) ?: return null else 1f
    return if (saturation in 0f..1f && lightness in 0f..1f && alpha in 0f..1f) hslToRgb(hue, saturation, lightness, alpha) else null
}

private fun parseHsv(value: String): ColorValue? {
    val parts = parseFunction(value, "hsva?", 3..4) ?: return null
    val hue = parts[0].removeSuffix("deg").trim().toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
    val saturation = parsePercent(parts[1]) ?: return null
    val valueComponent = parsePercent(parts[2]) ?: return null
    val alpha = if (parts.size == 4) parseAlpha(parts[3]) ?: return null else 1f
    return if (saturation in 0f..1f && valueComponent in 0f..1f && alpha in 0f..1f) hsvToRgb(hue, saturation, valueComponent, alpha) else null
}

private fun parseFunction(value: String, function: String, count: IntRange): List<String>? {
    val match = Regex("^$function\\s*\\((.*)\\)$", RegexOption.IGNORE_CASE).matchEntire(value.trim()) ?: return null
    val parts = match.groupValues[1].split(',').map(String::trim)
    return parts.takeIf { it.size in count }
}

private fun parseChannel(value: String, maxValue: Float): Float? {
    val number = value.removeSuffix("%").trim().toFloatOrNull()?.takeIf(Float::isFinite) ?: return null
    return if (value.trim().endsWith('%')) number.takeIf { it in 0f..100f }?.times(maxValue / 100f) else number.takeIf { it in 0f..maxValue }
}

private fun parsePercent(value: String): Float? = value.trim().removeSuffix("%").toFloatOrNull()
    ?.takeIf(Float::isFinite)
    ?.div(100f)

private fun parseAlpha(value: String): Float? = if (value.trim().endsWith('%')) {
    value.trim().removeSuffix("%").toFloatOrNull()?.takeIf(Float::isFinite)?.div(100f)
} else {
    value.trim().toFloatOrNull()?.takeIf(Float::isFinite)
}

private data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)
private data class Hsv(val hue: Float, val saturation: Float, val value: Float)

private fun rgbToHsl(color: ColorValue): Hsl {
    val red = color.red / 255f
    val green = color.green / 255f
    val blue = color.blue / 255f
    val maxValue = max(red, max(green, blue))
    val minValue = min(red, min(green, blue))
    val delta = maxValue - minValue
    val lightness = (maxValue + minValue) / 2f
    if (delta == 0f) return Hsl(0f, 0f, lightness)
    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hue = when (maxValue) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Hsl(hue, saturation, lightness)
}

private fun rgbToHsv(color: ColorValue): Hsv {
    val red = color.red / 255f
    val green = color.green / 255f
    val blue = color.blue / 255f
    val maxValue = max(red, max(green, blue))
    val minValue = min(red, min(green, blue))
    val delta = maxValue - minValue
    val hue = if (delta == 0f) 0f else when (maxValue) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Hsv(hue, if (maxValue == 0f) 0f else delta / maxValue, maxValue)
}

private fun hslToRgb(hue: Float, saturation: Float, lightness: Float, alpha: Float): ColorValue {
    val normalizedHue = hue.mod(360f)
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val x = chroma * (1f - abs((normalizedHue / 60f % 2f) - 1f))
    val match = lightness - chroma / 2f
    val (red, green, blue) = when (normalizedHue / 60f) {
        in 0f..1f -> Triple(chroma, x, 0f)
        in 1f..2f -> Triple(x, chroma, 0f)
        in 2f..3f -> Triple(0f, chroma, x)
        in 3f..4f -> Triple(0f, x, chroma)
        in 4f..5f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    return ColorValue(((red + match) * 255f).roundToInt(), ((green + match) * 255f).roundToInt(), ((blue + match) * 255f).roundToInt(), alpha)
}

private fun hsvToRgb(hue: Float, saturation: Float, value: Float, alpha: Float): ColorValue {
    val normalizedHue = hue.mod(360f)
    val chroma = value * saturation
    val x = chroma * (1f - abs((normalizedHue / 60f % 2f) - 1f))
    val match = value - chroma
    val (red, green, blue) = when (normalizedHue / 60f) {
        in 0f..1f -> Triple(chroma, x, 0f)
        in 1f..2f -> Triple(x, chroma, 0f)
        in 2f..3f -> Triple(0f, chroma, x)
        in 3f..4f -> Triple(0f, x, chroma)
        in 4f..5f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    return ColorValue(((red + match) * 255f).roundToInt(), ((green + match) * 255f).roundToInt(), ((blue + match) * 255f).roundToInt(), alpha)
}
