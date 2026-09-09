package com.artillery.fehelper.password

internal const val DEFAULT_SPECIAL_CHARACTERS = "!@#$%^&*()_+-=[]{}|;:,.<>?"

internal const val MIN_PASSWORD_LENGTH = 1
internal const val MAX_PASSWORD_LENGTH = 128
internal const val MIN_PASSWORD_COUNT = 1
internal const val MAX_PASSWORD_COUNT = 50

internal expect fun secureRandomInt(bound: Int): Int

internal data class PasswordInputErrors(
    val length: String? = null,
    val count: String? = null,
    val selection: String? = null,
    val specialCharacters: String? = null,
) {
    val hasErrors: Boolean
        get() = length != null || count != null || selection != null || specialCharacters != null
}

internal fun validatePasswordInput(
    lengthInput: String,
    countInput: String,
    includeDigits: Boolean,
    includeLowercase: Boolean,
    includeUppercase: Boolean,
    includeSpecialCharacters: Boolean,
    specialCharacters: String,
): PasswordInputErrors {
    val length = lengthInput.toIntOrNull()
    val count = countInput.toIntOrNull()
    val characterPool = buildCharacterPool(
        includeDigits = includeDigits,
        includeLowercase = includeLowercase,
        includeUppercase = includeUppercase,
        includeSpecialCharacters = includeSpecialCharacters,
        specialCharacters = specialCharacters,
    )
    return PasswordInputErrors(
        length = when {
            length == null -> "请输入数字"
            length !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH -> "长度需在 $MIN_PASSWORD_LENGTH-$MAX_PASSWORD_LENGTH 位之间"
            else -> null
        },
        count = when {
            count == null -> "请输入数字"
            count !in MIN_PASSWORD_COUNT..MAX_PASSWORD_COUNT -> "数量需在 $MIN_PASSWORD_COUNT-$MAX_PASSWORD_COUNT 个之间"
            else -> null
        },
        selection = if (characterPool.isEmpty()) "至少选择一种字符类型" else null,
        specialCharacters = if (includeSpecialCharacters && specialCharacters.isEmpty()) {
            "请输入至少一个特殊字符"
        } else {
            null
        },
    )
}

internal fun generatePasswords(
    length: Int,
    count: Int,
    includeDigits: Boolean,
    includeLowercase: Boolean,
    includeUppercase: Boolean,
    includeSpecialCharacters: Boolean,
    specialCharacters: String,
): List<String> {
    val characterPool = buildCharacterPool(
        includeDigits = includeDigits,
        includeLowercase = includeLowercase,
        includeUppercase = includeUppercase,
        includeSpecialCharacters = includeSpecialCharacters,
        specialCharacters = specialCharacters,
    )
    check(characterPool.isNotEmpty()) { "At least one character type must be selected" }
    return List(count) {
        buildString(capacity = length) {
            repeat(length) {
                append(characterPool[secureRandomInt(bound = characterPool.length)])
            }
        }
    }
}

private fun buildCharacterPool(
    includeDigits: Boolean,
    includeLowercase: Boolean,
    includeUppercase: Boolean,
    includeSpecialCharacters: Boolean,
    specialCharacters: String,
): String = buildString {
    if (includeDigits) append("0123456789")
    if (includeLowercase) append("abcdefghijklmnopqrstuvwxyz")
    if (includeUppercase) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    if (includeSpecialCharacters) append(specialCharacters)
}
