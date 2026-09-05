package com.artillery.fehelper.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val BrandBlue = Color(0xFF2F6FED)
internal val Ink = Color(0xFF000018)
internal val MutedInk = Color(0xFF516179)
internal val PageBackground = Color(0xFFF5F7FB)
internal val Border = Color(0xFFD9E1EE)

private val FeHelperColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF0FF),
    onPrimaryContainer = Ink,
    secondary = MutedInk,
    onSecondary = Color.White,
    background = PageBackground,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0F3F8),
    onSurfaceVariant = MutedInk,
    outline = Border,
)

@Composable
internal fun FeHelperTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FeHelperColorScheme, content = content)
}
