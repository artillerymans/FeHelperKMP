package com.artillery.fehelper.id

import java.security.SecureRandom

private val random = SecureRandom()

internal actual fun secureRandomInt(bound: Int): Int = random.nextInt(bound)
