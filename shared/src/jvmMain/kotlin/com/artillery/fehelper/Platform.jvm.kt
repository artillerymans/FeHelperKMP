package com.artillery.fehelper

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun isDebugBuild(): Boolean = System.getProperty("fehelper.debug").toBoolean()
