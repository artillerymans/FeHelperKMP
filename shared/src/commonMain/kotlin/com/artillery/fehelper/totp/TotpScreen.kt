package com.artillery.fehelper.totp

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.TimeZone
import kotlinx.coroutines.delay

@Suppress("DEPRECATION")
@Composable
internal fun TotpScreen(onBack: () -> Unit) {
    var secretInput by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf(TotpDigits.SIX) }
    var period by remember { mutableStateOf(TotpPeriod.THIRTY) }
    var current by remember { mutableStateOf(Clock.System.now()) }
    var copied by remember { mutableStateOf(false) }
    var copyError by remember { mutableStateOf(false) }
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val parsed = remember(secretInput) { parseTotpInput(secretInput) }
    val setup = parsed.setup
    val code = setup?.let {
        totpCode(setup = it, epochSeconds = current.epochSeconds, digits = digits, period = period)
    }
    val remaining = if (setup == null) 0 else period.seconds - (current.epochSeconds % period.seconds)
    val progress = if (setup == null) 0f else (period.seconds - remaining).toFloat() / period.seconds
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(key1 = Unit) {
        while (true) {
            current = Clock.System.now()
            delay(1.seconds)
        }
    }
    LaunchedEffect(key1 = copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    LaunchedEffect(key1 = secretInput) {
        if (secretInput.trim().startsWith("otpauth://", ignoreCase = true)) {
            setup?.let {
                digits = it.digits
                period = it.period
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
            .safeContentPadding(),
    ) {
        val horizontalPadding = if (maxWidth >= 900.dp) 32.dp else 16.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            topBar = {
                PageTitleBar(
                    title = "2FA 动态口令",
                    horizontalPadding = horizontalPadding,
                    onBack = onBack,
                )
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "本地生成 TOTP 验证码，密钥只在当前设备计算",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk),
                    )
                    SectionCard(
                        title = "当前时间",
                        description = "验证码按设备当前时间计算",
                    ) {
                        Text(
                            text = formatTotpLocalTime(instant = current, timeZone = timeZone),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Ink,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Text(
                            text = "时区：${timeZone.id}",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk),
                        )
                    }
                    SectionCard(
                        title = "导入密钥",
                        description = "支持直接输入 Base32 密钥，或粘贴 otpauth:// URI",
                    ) {
                        OutlinedTextField(
                            value = secretInput,
                            onValueChange = {
                                secretInput = it
                                copied = false
                                copyError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = "Base32 密钥或 otpauth:// URI") },
                            placeholder = { Text(text = "例如：JBSWY3DPEHPK3PXP") },
                            supportingText = {
                                Text(
                                    text = parsed.error ?: "密钥不会上传到网络",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (parsed.error == null) MutedInk else MaterialTheme.colorScheme.error,
                                    ),
                                )
                            },
                            isError = parsed.error != null,
                            singleLine = true,
                        )
                        if (setup != null && (setup.issuer != null || setup.account != null)) {
                            Text(
                                text = listOfNotNull(setup.issuer, setup.account).joinToString(" · "),
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk),
                            )
                        }
                    }
                    SectionCard(
                        title = "验证码设置",
                        description = "按服务端配置选择位数和刷新周期",
                    ) {
                        SettingGroup(
                            title = "验证码位数",
                            options = TotpDigits.entries,
                            selected = digits,
                            label = { it.label },
                            onSelect = { digits = it },
                        )
                        SettingGroup(
                            modifier = Modifier.padding(top = 16.dp),
                            title = "验证码周期",
                            options = TotpPeriod.entries,
                            selected = period,
                            label = { it.label },
                            onSelect = { period = it },
                        )
                    }
                    SectionCard(
                        title = "当前验证码",
                        description = if (setup == null) "输入有效密钥后生成" else "每 ${period.seconds} 秒自动刷新",
                    ) {
                        if (code == null) {
                            Text(
                                text = "等待有效密钥",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.headlineSmall.copy(color = MutedInk),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    text = code,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        color = Ink,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Text(
                                text = "剩余 $remaining 秒",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk),
                                textAlign = TextAlign.Center,
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .heightIn(min = 4.dp),
                                color = BrandBlue,
                                trackColor = Border,
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .heightIn(min = 48.dp)
                                    .background(BrandBlue, RoundedCornerShape(8.dp))
                                    .clickable(
                                        role = Role.Button,
                                        onClick = {
                                            val success = runCatching {
                                                clipboardManager.setText(AnnotatedString(code))
                                            }.isSuccess
                                            copied = success
                                            copyError = !success
                                        },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (copied) "已复制" else if (copyError) "重试复制" else "一键复制",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                            if (copyError) {
                                Text(
                                    text = "复制失败，请手动选择验证码复制",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                    SectionCard(
                        title = "使用说明",
                        description = "按服务端提供的信息完成配置",
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "1. 粘贴 Base32 密钥，或直接粘贴 otpauth://totp URI。",
                                "2. 根据服务端要求选择验证码位数和周期。",
                                "3. 点击“一键复制”，将当前验证码粘贴到登录页面。",
                                "4. 验证码会随倒计时自动刷新，设备时间不准时请先校准系统时间。",
                            ).forEach { instruction ->
                                Text(
                                    text = instruction,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                                )
                            }
                        }
                    }
                    SectionCard(
                        title = "常见问题",
                        description = "使用 TOTP 时的常见排查方式",
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf(
                                "为什么验证码无效？" to "请检查设备时间、时区、验证码位数和周期是否与服务端一致。",
                                "密钥会上传到服务器吗？" to "不会，密钥和验证码仅在当前页面本地计算。",
                                "为什么 URI 导入失败？" to "请确认 URI 以 otpauth://totp 开头，并包含有效的 secret 参数。",
                            ).forEach { (question, answer) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = question,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = Ink,
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                    Text(
                                        text = answer,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SettingGroup(
    modifier: Modifier = Modifier,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge.copy(color = Ink))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(text = label(option)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp),
                )
            }
        }
    }
}
