package com.artillery.fehelper.password

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.ErrorRed
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import com.artillery.fehelper.common.SuccessGreen
import com.artillery.state.StateViewModel
import com.artillery.state.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class RandomPasswordInputState(
    val length: String,
    val count: String,
    val includeDigits: Boolean,
    val includeLowercase: Boolean,
    val includeUppercase: Boolean,
    val includeSpecialCharacters: Boolean,
    val specialCharacters: String,
    val lengthError: String?,
    val countError: String?,
    val selectionError: String?,
    val specialCharactersError: String?,
)

private data class RandomPasswordResultState(
    val passwords: List<String>,
    val copied: Boolean,
    val copyError: Boolean,
)

private data class RandomPasswordState(
    val lengthInput: String = "20",
    val countInput: String = "1",
    val includeDigits: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeUppercase: Boolean = true,
    val includeSpecialCharacters: Boolean = false,
    val specialCharacters: String = DEFAULT_SPECIAL_CHARACTERS,
    val lengthError: String? = null,
    val countError: String? = null,
    val selectionError: String? = null,
    val specialCharactersError: String? = null,
    val passwords: List<String> = emptyList(),
    val copied: Boolean = false,
    val copyError: Boolean = false,
) {
    val input: RandomPasswordInputState
        get() = RandomPasswordInputState(
            length = lengthInput,
            count = countInput,
            includeDigits = includeDigits,
            includeLowercase = includeLowercase,
            includeUppercase = includeUppercase,
            includeSpecialCharacters = includeSpecialCharacters,
            specialCharacters = specialCharacters,
            lengthError = lengthError,
            countError = countError,
            selectionError = selectionError,
            specialCharactersError = specialCharactersError,
        )

    val result: RandomPasswordResultState
        get() = RandomPasswordResultState(
            passwords = passwords,
            copied = copied,
            copyError = copyError,
        )
}

private class RandomPasswordViewModel : StateViewModel<RandomPasswordState>(initialState = RandomPasswordState()) {
    fun onLengthChange(value: String) {
        setState { copy(lengthInput = value, lengthError = null, passwords = emptyList(), copied = false, copyError = false) }
    }

    fun onCountChange(value: String) {
        setState { copy(countInput = value, countError = null, passwords = emptyList(), copied = false, copyError = false) }
    }

    fun onDigitsChange(value: Boolean) {
        setState { copy(includeDigits = value, selectionError = null, passwords = emptyList(), copied = false, copyError = false) }
    }

    fun onLowercaseChange(value: Boolean) {
        setState { copy(includeLowercase = value, selectionError = null, passwords = emptyList(), copied = false, copyError = false) }
    }

    fun onUppercaseChange(value: Boolean) {
        setState { copy(includeUppercase = value, selectionError = null, passwords = emptyList(), copied = false, copyError = false) }
    }

    fun onSpecialCharactersChange(value: Boolean) {
        setState {
            copy(
                includeSpecialCharacters = value,
                selectionError = null,
                specialCharactersError = null,
                passwords = emptyList(),
                copied = false,
                copyError = false,
            )
        }
    }

    fun onSpecialCharactersInputChange(value: String) {
        setState {
            copy(
                specialCharacters = value,
                specialCharactersError = null,
                passwords = emptyList(),
                copied = false,
                copyError = false,
            )
        }
    }

    fun restoreDefaultSpecialCharacters() {
        setState {
            copy(
                specialCharacters = DEFAULT_SPECIAL_CHARACTERS,
                specialCharactersError = null,
                passwords = emptyList(),
                copied = false,
                copyError = false,
            )
        }
    }

    fun generate() {
        setState {
            val errors = validatePasswordInput(
                lengthInput = lengthInput,
                countInput = countInput,
                includeDigits = includeDigits,
                includeLowercase = includeLowercase,
                includeUppercase = includeUppercase,
                includeSpecialCharacters = includeSpecialCharacters,
                specialCharacters = specialCharacters,
            )
            if (errors.hasErrors) {
                copy(
                    lengthError = errors.length,
                    countError = errors.count,
                    selectionError = errors.selection,
                    specialCharactersError = errors.specialCharacters,
                    passwords = emptyList(),
                    copied = false,
                    copyError = false,
                )
            } else {
                copy(
                    passwords = generatePasswords(
                        length = lengthInput.toInt(),
                        count = countInput.toInt(),
                        includeDigits = includeDigits,
                        includeLowercase = includeLowercase,
                        includeUppercase = includeUppercase,
                        includeSpecialCharacters = includeSpecialCharacters,
                        specialCharacters = specialCharacters,
                    ),
                    lengthError = null,
                    countError = null,
                    selectionError = null,
                    specialCharactersError = null,
                    copied = false,
                    copyError = false,
                )
            }
        }
    }

    fun onCopyResult(success: Boolean) {
        setState { copy(copied = success, copyError = !success) }
        if (success) {
            viewModelScope.launch {
                delay(1500)
                setState { if (copied) copy(copied = false) else this }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
internal fun RandomPasswordScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val viewModel: RandomPasswordViewModel = viewModel(initializer = { RandomPasswordViewModel() })
    val inputState by viewModel.collectAsState(RandomPasswordState::input)
    val resultState by viewModel.collectAsState(RandomPasswordState::result)
    val clipboardManager = LocalClipboardManager.current
    val copyGeneratedPasswords = {
        val text = resultState.passwords.joinToString(separator = "\n")
        val success = runCatching { clipboardManager.setText(AnnotatedString(text)) }.isSuccess
        viewModel.onCopyResult(success = success)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .safeContentPadding(),
    ) {
        val wideContent = maxWidth >= 760.dp
        val horizontalPadding = if (maxWidth >= 900.dp) 32.dp else 16.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            topBar = {
                PageTitleBar(
                    title = "随机密码生成器",
                    horizontalPadding = horizontalPadding,
                    onBack = onBack,
                )
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 1000.dp)
                        .fillMaxWidth()
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "按需生成随机密码，密码仅在当前设备本地处理",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk),
                    )
                    if (wideContent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            PasswordSettingsCard(
                                modifier = Modifier.weight(1f),
                                state = inputState,
                                onLengthChange = viewModel::onLengthChange,
                                onCountChange = viewModel::onCountChange,
                                onDigitsChange = viewModel::onDigitsChange,
                                onLowercaseChange = viewModel::onLowercaseChange,
                                onUppercaseChange = viewModel::onUppercaseChange,
                                onSpecialCharactersChange = viewModel::onSpecialCharactersChange,
                                onSpecialCharactersInputChange = viewModel::onSpecialCharactersInputChange,
                                onRestoreDefault = viewModel::restoreDefaultSpecialCharacters,
                                onGenerate = viewModel::generate,
                            )
                            PasswordResultCard(
                                modifier = Modifier.weight(1f),
                                state = resultState,
                                onCopy = copyGeneratedPasswords,
                            )
                        }
                    } else {
                        PasswordSettingsCard(
                            modifier = Modifier.fillMaxWidth(),
                            state = inputState,
                            onLengthChange = viewModel::onLengthChange,
                            onCountChange = viewModel::onCountChange,
                            onDigitsChange = viewModel::onDigitsChange,
                            onLowercaseChange = viewModel::onLowercaseChange,
                            onUppercaseChange = viewModel::onUppercaseChange,
                            onSpecialCharactersChange = viewModel::onSpecialCharactersChange,
                            onSpecialCharactersInputChange = viewModel::onSpecialCharactersInputChange,
                            onRestoreDefault = viewModel::restoreDefaultSpecialCharacters,
                            onGenerate = viewModel::generate,
                        )
                        PasswordResultCard(
                            modifier = Modifier.fillMaxWidth(),
                            state = resultState,
                            onCopy = copyGeneratedPasswords,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordSettingsCard(
    modifier: Modifier,
    state: RandomPasswordInputState,
    onLengthChange: (String) -> Unit,
    onCountChange: (String) -> Unit,
    onDigitsChange: (Boolean) -> Unit,
    onLowercaseChange: (Boolean) -> Unit,
    onUppercaseChange: (Boolean) -> Unit,
    onSpecialCharactersChange: (Boolean) -> Unit,
    onSpecialCharactersInputChange: (String) -> Unit,
    onRestoreDefault: () -> Unit,
    onGenerate: () -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = "生成设置",
        description = "选择字符类型、密码长度和生成数量",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.length,
                onValueChange = onLengthChange,
                modifier = Modifier.weight(1f),
                label = { Text(text = "密码长度") },
                suffix = { Text(text = "位") },
                supportingText = state.lengthError?.let { error -> { Text(text = error) } },
                isError = state.lengthError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = state.count,
                onValueChange = onCountChange,
                modifier = Modifier.weight(1f),
                label = { Text(text = "生成数量") },
                suffix = { Text(text = "个") },
                supportingText = state.countError?.let { error -> { Text(text = error) } },
                isError = state.countError != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Text(
            text = "字符类型",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.labelLarge.copy(color = Ink),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PasswordOptionRow(
                modifier = Modifier.fillMaxWidth(),
                checked = state.includeDigits,
                label = "数字（0-9）",
                onCheckedChange = onDigitsChange,
            )
            PasswordOptionRow(
                modifier = Modifier.fillMaxWidth(),
                checked = state.includeLowercase,
                label = "小写字母（a-z）",
                onCheckedChange = onLowercaseChange,
            )
            PasswordOptionRow(
                modifier = Modifier.fillMaxWidth(),
                checked = state.includeUppercase,
                label = "大写字母（A-Z）",
                onCheckedChange = onUppercaseChange,
            )
            PasswordOptionRow(
                modifier = Modifier.fillMaxWidth(),
                checked = state.includeSpecialCharacters,
                label = "特殊符号",
                onCheckedChange = onSpecialCharactersChange,
            )
        }
        state.selectionError?.let { error ->
            Text(
                text = error,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed),
            )
        }
        OutlinedTextField(
            value = state.specialCharacters,
            onValueChange = onSpecialCharactersInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            enabled = state.includeSpecialCharacters,
            label = { Text(text = "特殊字符集") },
            supportingText = {
                Text(
                    text = state.specialCharactersError ?: "开启特殊符号后，可自定义参与生成的字符",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (state.specialCharactersError == null) MutedInk else ErrorRed,
                    ),
                )
            },
            isError = state.specialCharactersError != null,
            singleLine = true,
        )
        Text(
            text = "恢复默认",
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onRestoreDefault)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge.copy(color = BrandBlue),
        )
        Text(
            text = "生成密码",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(BrandBlue, RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onGenerate)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun PasswordOptionRow(
    modifier: Modifier,
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium.copy(color = Ink),
        )
    }
}

@Composable
private fun PasswordResultCard(
    modifier: Modifier,
    state: RandomPasswordResultState,
    onCopy: () -> Unit,
) {
    SectionCard(
        modifier = modifier,
        title = "生成结果",
        description = if (state.passwords.isEmpty()) "生成后在这里查看密码" else "已生成 ${state.passwords.size} 个密码",
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
        ) {
            if (state.passwords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "点击“生成密码”查看结果",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                SelectionContainer {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.passwords.forEachIndexed { index, password ->
                            Text(
                                text = if (state.passwords.size == 1) password else "${index + 1}. $password",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Ink,
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }
                    }
                }
            }
        }
        if (state.passwords.isNotEmpty()) {
            Text(
                text = when {
                    state.copied -> "已复制"
                    state.copyError -> "复制失败，请重试"
                    state.passwords.size == 1 -> "复制"
                    else -> "复制全部"
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .heightIn(min = 48.dp)
                    .padding(top = 8.dp)
                    .clickable(role = Role.Button, onClick = onCopy)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = if (state.copyError) ErrorRed else if (state.copied) SuccessGreen else BrandBlue,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
        Text(
            text = "密码仅在当前页面生成，不会上传到服务器",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall.copy(color = MutedInk),
        )
    }
}
