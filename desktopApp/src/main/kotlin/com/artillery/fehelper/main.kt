package com.artillery.fehelper

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.GraphicsEnvironment

private const val WindowScreenFraction = 0.85f
private const val MaxWindowWidth = 1200f
private const val WindowAspectRatio = 3f / 2f

fun main() {
    val workArea = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    val availableWidth = workArea.width * WindowScreenFraction
    val availableHeight = workArea.height * WindowScreenFraction
    val width = minOf(MaxWindowWidth, availableWidth, availableHeight * WindowAspectRatio)
    val windowSize = DpSize(width.dp, (width / WindowAspectRatio).dp)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                position = WindowPosition(Alignment.Center),
                size = windowSize,
            ),
            title = "前端助手",
        ) {
            App()
        }
    }
}
