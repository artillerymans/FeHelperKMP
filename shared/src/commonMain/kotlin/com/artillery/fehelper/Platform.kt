package com.artillery.fehelper

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform