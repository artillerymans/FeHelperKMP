package com.artillery.fehelper

@JsFun("() => typeof process !== 'undefined' && process.env != null && process.env.NODE_ENV !== 'production'")
private external fun isWebpackDebugBuild(): Boolean

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun isDebugBuild(): Boolean = isWebpackDebugBuild()
