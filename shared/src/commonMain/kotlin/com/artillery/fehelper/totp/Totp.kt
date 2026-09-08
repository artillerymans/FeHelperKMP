package com.artillery.fehelper.totp

internal enum class TotpDigits(val value: Int, val label: String) {
    SIX(6, "6 位"),
    EIGHT(8, "8 位"),
}

internal enum class TotpPeriod(val seconds: Long, val label: String) {
    THIRTY(30, "30 秒"),
    SIXTY(60, "60 秒"),
}

internal data class TotpSetup(
    val secret: String,
    val secretBytes: ByteArray,
    val account: String? = null,
    val issuer: String? = null,
    val digits: TotpDigits = TotpDigits.SIX,
    val period: TotpPeriod = TotpPeriod.THIRTY,
)

internal data class TotpParseResult(
    val setup: TotpSetup? = null,
    val error: String? = null,
)

internal fun parseTotpInput(value: String): TotpParseResult {
    val input = value.trim()
    if (input.isEmpty()) return TotpParseResult()

    if (!input.startsWith("otpauth://", ignoreCase = true)) {
        return setupFromSecret(secret = input)
    }

    val uriParts = input.substringAfter("://", missingDelimiterValue = "").split('?', limit = 2)
    val path = uriParts.firstOrNull().orEmpty()
    val type = path.substringBefore('/').lowercase()
    if (type != "totp") return TotpParseResult(error = "仅支持 otpauth://totp URI")

    val label = path.substringAfter('/', missingDelimiterValue = "")
        .takeIf { it.isNotEmpty() }
        ?.let(::percentDecode)
    val query = uriParts.getOrNull(1).orEmpty()
        .split('&')
        .filter { it.isNotEmpty() }
        .associate { item ->
            val key = item.substringBefore('=').lowercase()
            key to percentDecode(item.substringAfter('=', missingDelimiterValue = ""))
        }
    val secret = query["secret"] ?: return TotpParseResult(error = "URI 缺少 secret 参数")
    val baseResult = setupFromSecret(secret = secret)
    val setup = baseResult.setup ?: return baseResult
    when (query["algorithm"]?.uppercase()) {
        null, "", "SHA1" -> Unit
        else -> return TotpParseResult(error = "暂不支持 ${query["algorithm"]} 算法")
    }
    val digits = when (query["digits"]) {
        null, "", "6" -> TotpDigits.SIX
        "8" -> TotpDigits.EIGHT
        else -> return TotpParseResult(error = "URI 的 digits 仅支持 6 或 8")
    }
    val period = when (query["period"]) {
        null, "", "30" -> TotpPeriod.THIRTY
        "60" -> TotpPeriod.SIXTY
        else -> return TotpParseResult(error = "URI 的 period 仅支持 30 或 60")
    }
    val account = label?.substringAfter(':', missingDelimiterValue = label)?.takeIf { it.isNotEmpty() }
    return TotpParseResult(
        setup = setup.copy(
            account = account,
            issuer = query["issuer"]?.takeIf { it.isNotEmpty() },
            digits = digits,
            period = period,
        ),
    )
}

internal fun totpCode(
    setup: TotpSetup,
    epochSeconds: Long,
    digits: TotpDigits,
    period: TotpPeriod,
): String {
    val counter = epochSeconds / period.seconds
    val message = ByteArray(8)
    for (index in 0 until 8) {
        message[7 - index] = (counter ushr (index * 8)).toByte()
    }

    val digest = hmacSha1(key = setup.secretBytes, message = message)
    val offset = digest.last().toInt() and 0x0f
    val binary = ((digest[offset].toInt() and 0x7f) shl 24) or
        ((digest[offset + 1].toInt() and 0xff) shl 16) or
        ((digest[offset + 2].toInt() and 0xff) shl 8) or
        (digest[offset + 3].toInt() and 0xff)
    val modulus = if (digits == TotpDigits.SIX) 1_000_000 else 100_000_000
    return (binary % modulus).toString().padStart(digits.value, '0')
}

private fun setupFromSecret(secret: String): TotpParseResult {
    val normalized = secret.filterNot { it == '-' || it.isWhitespace() }.uppercase()
    val decoded = decodeBase32(normalized) ?: return TotpParseResult(error = "请输入有效的 Base32 密钥")
    return TotpParseResult(setup = TotpSetup(secret = normalized, secretBytes = decoded))
}

private fun decodeBase32(value: String): ByteArray? {
    if (value.isEmpty()) return null
    val unpadded = value.trimEnd('=')
    if (unpadded.isEmpty() || unpadded.any { it == '=' || Base32Alphabet.indexOf(it) < 0 }) return null

    val output = ArrayList<Byte>(unpadded.length * 5 / 8)
    var buffer = 0
    var bits = 0
    for (character in unpadded) {
        buffer = (buffer shl 5) or Base32Alphabet.indexOf(character)
        bits += 5
        while (bits >= 8) {
            bits -= 8
            output += ((buffer shr bits) and 0xff).toByte()
            buffer = if (bits == 0) 0 else buffer and ((1 shl bits) - 1)
        }
    }
    if (bits > 0 && buffer != 0) return null
    return output.toByteArray().takeIf { it.isNotEmpty() }
}

private fun percentDecode(value: String): String = buildString(value.length) {
    var index = 0
    while (index < value.length) {
        if (value[index] == '%' && index + 2 < value.length) {
            val decoded = value.substring(index + 1, index + 3).toIntOrNull(radix = 16)
            if (decoded != null) {
                append(decoded.toChar())
                index += 3
                continue
            }
        }
        append(if (value[index] == '+') ' ' else value[index])
        index++
    }
}

private const val Base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

// ponytail: SHA-1 keeps commonMain dependency-free; add SHA-256/512 only when imported providers require them.
private fun hmacSha1(key: ByteArray, message: ByteArray): ByteArray {
    val keyBlock = ByteArray(64)
    val normalizedKey = if (key.size > keyBlock.size) sha1(key) else key
    normalizedKey.copyInto(destination = keyBlock)
    val inner = ByteArray(keyBlock.size + message.size) { index ->
        (keyBlock[index % keyBlock.size].toInt() xor 0x36).toByte()
    }
    message.copyInto(destination = inner, destinationOffset = keyBlock.size)
    val outer = ByteArray(keyBlock.size + 20) { index ->
        (keyBlock[index % keyBlock.size].toInt() xor 0x5c).toByte()
    }
    sha1(inner).copyInto(destination = outer, destinationOffset = keyBlock.size)
    return sha1(outer)
}

private fun sha1(input: ByteArray): ByteArray {
    val paddedSize = ((input.size + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedSize)
    input.copyInto(destination = padded)
    padded[input.size] = 0x80.toByte()
    val bitLength = input.size.toLong() * 8
    for (index in 0 until 8) {
        padded[padded.lastIndex - index] = (bitLength ushr (index * 8)).toByte()
    }

    var h0 = 0x67452301
    var h1 = 0xefcdab89.toInt()
    var h2 = 0x98badcfe.toInt()
    var h3 = 0x10325476
    var h4 = 0xc3d2e1f0.toInt()
    val words = IntArray(80)
    for (offset in padded.indices step 64) {
        for (index in 0 until 16) {
            val start = offset + index * 4
            words[index] = ((padded[start].toInt() and 0xff) shl 24) or
                ((padded[start + 1].toInt() and 0xff) shl 16) or
                ((padded[start + 2].toInt() and 0xff) shl 8) or
                (padded[start + 3].toInt() and 0xff)
        }
        for (index in 16 until 80) {
            words[index] = rotateLeft(words[index - 3] xor words[index - 8] xor words[index - 14] xor words[index - 16], 1)
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        for (index in 0 until 80) {
            val (function, constant) = when (index) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5a827999
                in 20..39 -> (b xor c xor d) to 0x6ed9eba1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8f1bbcdc.toInt()
                else -> (b xor c xor d) to 0xca62c1d6.toInt()
            }
            val next = rotateLeft(a, 5) + function + e + constant + words[index]
            e = d
            d = c
            c = rotateLeft(b, 30)
            b = a
            a = next
        }
        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
    }

    return byteArrayOf(
        (h0 ushr 24).toByte(), (h0 ushr 16).toByte(), (h0 ushr 8).toByte(), h0.toByte(),
        (h1 ushr 24).toByte(), (h1 ushr 16).toByte(), (h1 ushr 8).toByte(), h1.toByte(),
        (h2 ushr 24).toByte(), (h2 ushr 16).toByte(), (h2 ushr 8).toByte(), h2.toByte(),
        (h3 ushr 24).toByte(), (h3 ushr 16).toByte(), (h3 ushr 8).toByte(), h3.toByte(),
        (h4 ushr 24).toByte(), (h4 ushr 16).toByte(), (h4 ushr 8).toByte(), h4.toByte(),
    )
}

private fun rotateLeft(value: Int, distance: Int): Int = (value shl distance) or (value ushr (32 - distance))
