package com.artillery.fehelper

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "前端助手",
    ) {
        App()
    }
}