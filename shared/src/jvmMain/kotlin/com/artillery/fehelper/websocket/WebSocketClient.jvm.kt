package com.artillery.fehelper.websocket

import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import javax.swing.SwingUtilities

internal actual class WebSocketClient actual constructor(
    private val onOpen: () -> Unit,
    private val onMessage: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onClose: (Int, String) -> Unit,
) {
    private val httpClient = HttpClient.newHttpClient()

    @Volatile
    private var generation = 0

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var pending: CompletableFuture<WebSocket>? = null

    actual fun connect(url: String) {
        disconnect()
        val attempt = ++generation
        val listener = SocketListener(attempt = attempt)
        val future = runCatching {
            httpClient.newWebSocketBuilder().buildAsync(URI.create(url), listener)
        }.getOrElse { error ->
            dispatch(attempt = attempt) { onError(error.message ?: "连接失败") }
            return
        }

        pending = future
        future.whenComplete { _, error ->
            if (error != null) {
                dispatch(attempt = attempt) {
                    onError(error.cause?.message ?: error.message ?: "连接失败")
                }
            }
        }
    }

    actual fun send(message: String): Boolean {
        val current = socket ?: return false
        return runCatching { current.sendText(message, true) }.isSuccess
    }

    actual fun disconnect() {
        generation++
        pending?.cancel(true)
        pending = null
        runCatching { socket?.sendClose(WebSocket.NORMAL_CLOSURE, "用户断开") }
        socket = null
    }

    private fun dispatch(attempt: Int, action: () -> Unit) {
        SwingUtilities.invokeLater {
            if (attempt == generation) action()
        }
    }

    private inner class SocketListener(private val attempt: Int) : WebSocket.Listener {
        private val text = StringBuilder()

        override fun onOpen(webSocket: WebSocket) {
            if (attempt != generation) {
                webSocket.abort()
                return
            }
            socket = webSocket
            pending = null
            webSocket.request(1)
            dispatch(attempt = attempt, action = onOpen)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
            text.append(data)
            if (last) {
                val message = text.toString()
                text.clear()
                dispatch(attempt = attempt) { onMessage(message) }
            }
            webSocket.request(1)
            return CompletableFuture.completedFuture(null)
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
            if (attempt != generation) return CompletableFuture.completedFuture(null)
            if (socket === webSocket) socket = null
            dispatch(attempt = attempt) { onClose(statusCode, reason) }
            return CompletableFuture.completedFuture(null)
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            if (attempt != generation) return
            if (socket === webSocket) socket = null
            dispatch(attempt = attempt) { onError(error.message ?: "连接异常") }
        }
    }
}
