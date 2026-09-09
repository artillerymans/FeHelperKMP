package com.artillery.fehelper.password

private fun secureRandomIntFromCrypto(bound: Int): Int = js(
    """
    (function(bound) {
        const limit = Math.floor(0x100000000 / bound) * bound;
        const values = new Uint32Array(1);
        do {
            globalThis.crypto.getRandomValues(values);
        } while (values[0] >= limit);
        return values[0] % bound;
    })(bound)
    """,
)

internal actual fun secureRandomInt(bound: Int): Int = secureRandomIntFromCrypto(bound = bound)
