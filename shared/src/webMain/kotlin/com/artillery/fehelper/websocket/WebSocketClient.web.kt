package com.artillery.fehelper.websocket

import js.reflect.unsafeCast
import web.events.EventHandler
import web.messaging.MessageEvent
import web.sockets.CloseEvent
import web.sockets.WebSocket

internal actual class WebSocketClient actual constructor(
    private val onOpen: () -> Unit,
    private val onMessage: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onClose: (Int, String) -> Unit,
) {
    private var socket: WebSocket? = null

    actual fun connect(url: String) {
        disconnect()
        val current = runCatching { WebSocket(url = url) }.getOrElse { error ->
            onError(error.message ?: "连接失败")
            return
        }
        socket = current
        current.onopen = EventHandler {
            if (socket === current) onOpen()
        }
        current.onmessage = unsafeCast { event: MessageEvent<*> ->
            if (socket === current) onMessage(event.data?.toString() ?: "")
        }
        current.onerror = EventHandler {
            if (socket === current) onError("连接失败，请检查地址和服务状态")
        }
        current.onclose = unsafeCast { event: CloseEvent ->
            if (socket === current) {
                socket = null
                onClose(event.code.toInt(), event.reason)
            }
        }
    }

    actual fun send(message: String): Boolean {
        val current = socket?.takeIf { it.readyState == WebSocket.OPEN } ?: return false
        return runCatching { current.send(data = message) }.isSuccess
    }

    actual fun disconnect() {
        socket?.close(code = 1000, reason = "用户断开")
        socket = null
    }
}
