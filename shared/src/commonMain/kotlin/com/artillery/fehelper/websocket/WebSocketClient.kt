package com.artillery.fehelper.websocket

internal expect class WebSocketClient(
    onOpen: () -> Unit,
    onMessage: (String) -> Unit,
    onError: (String) -> Unit,
    onClose: (Int, String) -> Unit,
) {
    fun connect(url: String)

    fun send(message: String): Boolean

    fun disconnect()
}
