package com.artillery.fehelper.totp

import androidx.compose.foundation.background
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
import kotlinx.coroutines.delay

@Composable
internal fun TotpScreen(onBack: () -> Unit) {
    var secretInput by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf(TotpDigits.SIX) }
    var period by remember { mutableStateOf(TotpPeriod.THIRTY) }
    var epochSeconds by remember { mutableStateOf(Clock.System.now().epochSeconds) }
    val parsed = remember(secretInput) { parseTotpInput(secretInput) }
    val setup = parsed.setup
    val code = setup?.let {
        totpCode(setup = it, epochSeconds = epochSeconds, digits = digits, period = period)
    }
    val remaining = if (setup == null) 0 else period.seconds - (epochSeconds % period.seconds)
    val progress = if (setup == null) 0f else (period.seconds - remaining).toFloat() / period.seconds

    LaunchedEffect(key1 = Unit) {
        while (true) {
            epochSeconds = Clock.System.now().epochSeconds
            delay(1.seconds)
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
                        title = "导入密钥",
                        description = "支持直接输入 Base32 密钥，或粘贴 otpauth:// URI",
                    ) {
                        OutlinedTextField(
                            value = secretInput,
                            onValueChange = { secretInput = it },
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
