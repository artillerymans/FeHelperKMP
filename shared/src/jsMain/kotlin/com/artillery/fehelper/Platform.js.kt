package com.artillery.fehelper

import web.navigator.navigator

class JsPlatform : Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
        ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
        ?: "Unknown"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun isDebugBuild(): Boolean = js(
    "typeof process !== 'undefined' && process.env != null && process.env.NODE_ENV !== 'production'",
)
