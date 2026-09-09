package com.artillery.fehelper.loan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.ErrorRed
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.NumberField
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import com.artillery.state.StateViewModel
import com.artillery.state.collectAsState
import kotlin.math.pow
import kotlin.math.round

private enum class LoanMode(val title: String) {
    RATE("贷款利率计算"),
    SCHEDULE("还款明细计算"),
}

private enum class RepaymentMethod(val title: String) {
    EQUAL_PAYMENT("等额本息"),
    EQUAL_PRINCIPAL("等额本金"),
}

private data class LoanInstallment(
    val period: Int,
    val payment: Double,
    val principal: Double,
    val interest: Double,
    val cumulativePrincipal: Double,
    val cumulativeInterest: Double,
    val remainingPrincipal: Double,
    val remainingInterest: Double,
)

private data class LoanCalculation(
    val annualRate: Double,
    val totalRepayment: Double,
    val totalInterest: Double,
    val installments: List<LoanInstallment> = emptyList(),
)

private data class LoanInputState(
    val mode: LoanMode,
    val principal: String,
    val term: String,
    val annualRate: String,
    val totalRepayment: String,
    val method: RepaymentMethod,
    val errors: Map<String, String>,
)

private data class LoanState(
    val mode: LoanMode = LoanMode.SCHEDULE,
    val principal: String = "10000",
    val term: String = "12",
    val annualRate: String = "24",
    val totalRepayment: String = "11347.2",
    val method: RepaymentMethod = RepaymentMethod.EQUAL_PAYMENT,
    val errors: Map<String, String> = emptyMap(),
    val result: LoanCalculation? = null,
) {
    val input: LoanInputState
        get() = LoanInputState(
            mode = mode,
            principal = principal,
            term = term,
            annualRate = annualRate,
            totalRepayment = totalRepayment,
            method = method,
            errors = errors,
        )
}

private class LoanCalculatorViewModel : StateViewModel<LoanState>(initialState = LoanState()) {
    fun onModeChange(value: LoanMode) {
        setState { copy(mode = value, errors = emptyMap(), result = null) }
    }

    fun onPrincipalChange(value: String) {
        update { copy(principal = value) }
    }

    fun onTermChange(value: String) {
        update { copy(term = value) }
    }

    fun onAnnualRateChange(value: String) {
        update { copy(annualRate = value) }
    }

    fun onTotalRepaymentChange(value: String) {
        update { copy(totalRepayment = value) }
    }

    fun onMethodChange(value: RepaymentMethod) {
        setState { copy(method = value, errors = emptyMap(), result = null) }
    }

    fun calculate() {
        setState {
            val outcome = validateAndCalculate(state = this)
            copy(errors = outcome.errors, result = outcome.result)
        }
    }

    private fun update(reducer: LoanState.() -> LoanState) {
        setState {
            reducer().copy(errors = emptyMap(), result = null)
        }
    }
}

private data class CalculationOutcome(
    val errors: Map<String, String>,
    val result: LoanCalculation?,
)

private fun validateAndCalculate(state: LoanState): CalculationOutcome {
    val errors = mutableMapOf<String, String>()
    val principal = state.principal.toDoubleOrNull()
    val term = state.term.toIntOrNull()
    if (principal == null || !principal.isFinite() || principal <= 0) {
        errors["principal"] = "贷款本金请输入大于 0 的数字"
    }
    if (term == null || term !in 1..600) {
        errors["term"] = "贷款期限请输入 1 到 600 之间的整数"
    }

    val parsedPrincipal = principal ?: 0.0
    val parsedTerm = term ?: 0
    if (state.mode == LoanMode.SCHEDULE) {
        val rate = state.annualRate.toDoubleOrNull()
        if (rate == null || !rate.isFinite() || rate < 0 || rate > 100) {
            errors["annualRate"] = "年化利率请输入 0 到 100 之间的数字"
        }
        if (errors.isNotEmpty()) return CalculationOutcome(errors = errors, result = null)
        return CalculationOutcome(
            errors = emptyMap(),
            result = amortize(
                principal = parsedPrincipal,
                term = parsedTerm,
                annualRate = rate ?: 0.0,
                method = state.method,
            ),
        )
    }

    val totalRepayment = state.totalRepayment.toDoubleOrNull()
    if (totalRepayment == null || !totalRepayment.isFinite() || totalRepayment < parsedPrincipal) {
        errors["totalRepayment"] = "总还款额不能小于贷款本金"
    }
    if (errors.isNotEmpty()) return CalculationOutcome(errors = errors, result = null)
    val total = totalRepayment ?: parsedPrincipal
    val annualRate = inferAnnualRate(
        principal = parsedPrincipal,
        totalRepayment = total,
        term = parsedTerm,
        method = state.method,
    )
    return CalculationOutcome(
        errors = emptyMap(),
        result = LoanCalculation(
            annualRate = annualRate,
            totalRepayment = roundMoney(total),
            totalInterest = roundMoney(total - parsedPrincipal),
        ),
    )
}

private fun amortize(
    principal: Double,
    term: Int,
    annualRate: Double,
    method: RepaymentMethod,
): LoanCalculation {
    val monthlyRate = annualRate / 100 / 12
    val payment = if (method == RepaymentMethod.EQUAL_PAYMENT) {
        roundMoney(annuityPayment(principal = principal, term = term, monthlyRate = monthlyRate))
    } else {
        0.0
    }
    val rawInstallments = buildList {
        var balance = principal
        var cumulativePrincipal = 0.0
        var cumulativeInterest = 0.0
        repeat(term) { index ->
            val interest = roundMoney(balance * monthlyRate)
            val principalPart = if (index == term - 1) {
                balance
            } else if (method == RepaymentMethod.EQUAL_PAYMENT) {
                payment - interest
            } else {
                roundMoney(principal / term)
            }
            val currentPayment = if (method == RepaymentMethod.EQUAL_PAYMENT) {
                payment
            } else {
                roundMoney(principalPart + interest)
            }
            balance = roundMoney((balance - principalPart).coerceAtLeast(0.0))
            cumulativePrincipal += principalPart
            cumulativeInterest += interest
            add(
                LoanInstallment(
                    period = index + 1,
                    payment = roundMoney(currentPayment),
                    principal = roundMoney(principalPart),
                    interest = roundMoney(interest),
                    cumulativePrincipal = roundMoney(cumulativePrincipal),
                    cumulativeInterest = roundMoney(cumulativeInterest),
                    remainingPrincipal = roundMoney(balance),
                    remainingInterest = 0.0,
                ),
            )
        }
    }
    val totalRepayment = rawInstallments.sumOf { it.payment }
    val totalInterest = roundMoney(totalRepayment - principal)
    val previousInterest = rawInstallments.sumOf { it.interest } - rawInstallments.last().interest
    val installments = rawInstallments.mapIndexed { index, installment ->
        if (index == rawInstallments.lastIndex) {
            val finalInterest = roundMoney(totalInterest - previousInterest)
            installment.copy(
                payment = roundMoney(installment.principal + finalInterest),
                interest = finalInterest,
                cumulativeInterest = totalInterest,
                remainingInterest = 0.0,
                remainingPrincipal = 0.0,
            )
        } else {
            installment.copy(remainingInterest = roundMoney(totalInterest - installment.cumulativeInterest))
        }
    }
    return LoanCalculation(
        annualRate = annualRate,
        totalRepayment = roundMoney(totalRepayment),
        totalInterest = roundMoney(totalInterest),
        installments = installments,
    )
}

private fun inferAnnualRate(
    principal: Double,
    totalRepayment: Double,
    term: Int,
    method: RepaymentMethod,
): Double {
    if (totalRepayment == principal) return 0.0
    if (method == RepaymentMethod.EQUAL_PRINCIPAL) {
        val monthlyRate = (totalRepayment - principal) / principal * 2 / (term + 1)
        return monthlyRate * 12 * 100
    }

    val targetPayment = totalRepayment / term
    var low = 0.0
    var high = 1.0
    repeat(80) {
        val middle = (low + high) / 2
        if (annuityPayment(principal = principal, term = term, monthlyRate = middle) < targetPayment) {
            low = middle
        } else {
            high = middle
        }
    }
    return ((low + high) / 2) * 12 * 100
}

private fun annuityPayment(principal: Double, term: Int, monthlyRate: Double): Double {
    if (monthlyRate == 0.0) return principal / term
    val factor = (1 + monthlyRate).pow(term)
    return principal * monthlyRate * factor / (factor - 1)
}

private fun roundMoney(value: Double): Double = round(value * 100) / 100

private fun money(value: Double): String {
    val cents = round(value * 100).toLong()
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$whole.$fraction"
}

private fun percent(value: Double): String = "${money(value)}%"

@Composable
internal fun LoanCalculatorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    val viewModel: LoanCalculatorViewModel = viewModel(initializer = { LoanCalculatorViewModel() })
    val inputState by viewModel.collectAsState(LoanState::input)
    val result by viewModel.collectAsState(LoanState::result)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .safeContentPadding(),
    ) {
        val wide = maxWidth >= 900.dp
        val horizontalPadding = if (wide) 32.dp else 16.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = PageBackground,
            topBar = {
                PageTitleBar(
                    title = "贷款利率计算器",
                    horizontalPadding = horizontalPadding,
                    onBack = onBack,
                )
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize()
                        .widthIn(max = 1200.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "根据本金、期限和还款方式，快速反推贷款年化利率或查看逐月还款明细",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk),
                    )
                    LoanModeSwitch(
                        mode = inputState.mode,
                        onModeChange = viewModel::onModeChange,
                    )
                    LoanForm(
                        state = inputState,
                        wide = wide,
                        onPrincipalChange = viewModel::onPrincipalChange,
                        onTermChange = viewModel::onTermChange,
                        onAnnualRateChange = viewModel::onAnnualRateChange,
                        onTotalRepaymentChange = viewModel::onTotalRepaymentChange,
                        onMethodChange = viewModel::onMethodChange,
                        onCalculate = viewModel::calculate,
                    )
                    LoanResultCard(result = result, mode = inputState.mode, wide = wide)
                }
            }
        }
    }
}

@Composable
private fun LoanModeSwitch(
    mode: LoanMode,
    onModeChange: (LoanMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LoanMode.values().forEach { option ->
            FilterChip(
                selected = mode == option,
                onClick = { onModeChange(option) },
                label = { Text(text = option.title) },
                modifier = Modifier.heightIn(min = 48.dp),
                elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp),
            )
        }
    }
}

@Composable
private fun LoanForm(
    state: LoanInputState,
    wide: Boolean,
    onPrincipalChange: (String) -> Unit,
    onTermChange: (String) -> Unit,
    onAnnualRateChange: (String) -> Unit,
    onTotalRepaymentChange: (String) -> Unit,
    onMethodChange: (RepaymentMethod) -> Unit,
    onCalculate: () -> Unit,
) {
    SectionCard(
        title = if (state.mode == LoanMode.SCHEDULE) "还款参数" else "利率参数",
        description = if (state.mode == LoanMode.SCHEDULE) {
            "输入年化利率，查看每期本金、利息和剩余金额"
        } else {
            "输入合同总还款额，反推出实际年化利率"
        },
    ) {
        LoanFieldPair(
            wide = wide,
            first = {
                NumberField(
                    modifier = it,
                    label = "贷款本金",
                    value = state.principal,
                    suffix = "元",
                    error = state.errors["principal"],
                    onValueChange = onPrincipalChange,
                )
            },
            second = {
                NumberField(
                    modifier = it,
                    label = "贷款期限",
                    value = state.term,
                    suffix = "月",
                    error = state.errors["term"],
                    onValueChange = onTermChange,
                )
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        NumberField(
            label = if (state.mode == LoanMode.SCHEDULE) "年化利率" else "总还款额",
            value = if (state.mode == LoanMode.SCHEDULE) state.annualRate else state.totalRepayment,
            suffix = if (state.mode == LoanMode.SCHEDULE) "%" else "元",
            error = if (state.mode == LoanMode.SCHEDULE) state.errors["annualRate"] else state.errors["totalRepayment"],
            onValueChange = if (state.mode == LoanMode.SCHEDULE) onAnnualRateChange else onTotalRepaymentChange,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还款方式",
            style = MaterialTheme.typography.labelLarge.copy(color = Ink, fontWeight = FontWeight.SemiBold),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RepaymentMethod.values().forEach { method ->
                FilterChip(
                    selected = state.method == method,
                    onClick = { onMethodChange(method) },
                    label = { Text(text = method.title) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = 0.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "计算",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(BrandBlue, RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onCalculate)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun LoanResultCard(
    result: LoanCalculation?,
    mode: LoanMode,
    wide: Boolean,
) {
    if (mode == LoanMode.RATE) {
        LoanSummaryCard(result = result)
    } else {
        LoanScheduleCard(result = result, wide = wide)
    }
}

@Composable
private fun LoanSummaryCard(result: LoanCalculation?) {
    SectionCard(title = "推荐结果", description = "结果按输入的还款方式计算，实际合同以金融机构披露为准") {
        if (result == null) {
            EmptyResult()
        } else {
            SummaryLine(label = "实际年化利率", value = percent(result.annualRate), emphasize = true)
            SummaryLine(label = "利息总额", value = money(result.totalInterest), emphasize = true)
            SummaryLine(label = "总还款额", value = money(result.totalRepayment), emphasize = false)
        }
    }
}

@Composable
private fun LoanScheduleCard(
    result: LoanCalculation?,
    wide: Boolean,
) {
    SectionCard(title = "推荐结果", description = "按月展示本金、利息、累计还款和剩余金额") {
        if (result == null) {
            EmptyResult()
        } else {
            SummaryLine(label = "总还款额", value = money(result.totalRepayment), emphasize = true)
            SummaryLine(label = "利息总额", value = money(result.totalInterest), emphasize = true)
            Spacer(modifier = Modifier.height(16.dp))
            if (wide) {
                ScheduleTable(result = result)
            } else {
                ScheduleList(result = result)
            }
        }
    }
}

@Composable
private fun EmptyResult() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = "填写参数后点击“计算”查看结果",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk),
        )
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    emphasize: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = if (emphasize) ErrorRed else Ink,
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

private val scheduleColumns = listOf(
    "期数", "月供金额", "本金", "利息", "累计本金", "累计利息", "剩余本金", "剩余利息",
)

@Composable
private fun ScheduleTable(result: LoanCalculation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        Row(modifier = Modifier.width(980.dp).background(MaterialTheme.colorScheme.primaryContainer)) {
            scheduleColumns.forEach { title ->
                TableCell(text = title, emphasized = true)
            }
        }
        Row(modifier = Modifier.width(980.dp).background(Color(0xFFFFFDE7))) {
            TableCell(text = "合计", emphasized = true)
            TableCell(text = money(result.totalRepayment), emphasized = true)
            TableCell(text = money(result.totalRepayment - result.totalInterest), emphasized = true)
            TableCell(text = money(result.totalInterest), emphasized = true)
            TableCell(text = "—", emphasized = true)
            TableCell(text = "—", emphasized = true)
            TableCell(text = money(result.totalRepayment - result.totalInterest), emphasized = true)
            TableCell(text = money(result.totalInterest), emphasized = true)
        }
        result.installments.forEach { installment ->
            Row(modifier = Modifier.width(980.dp)) {
                TableCell(text = installment.period.toString())
                TableCell(text = money(installment.payment))
                TableCell(text = money(installment.principal))
                TableCell(text = money(installment.interest))
                TableCell(text = money(installment.cumulativePrincipal))
                TableCell(text = money(installment.cumulativeInterest))
                TableCell(text = money(installment.remainingPrincipal))
                TableCell(text = money(installment.remainingInterest))
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    emphasized: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier
            .width(122.dp)
            .heightIn(min = 40.dp)
            .border(width = 1.dp, color = Border)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodySmall.copy(
            color = if (emphasized) Ink else MutedInk,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        ),
        textAlign = TextAlign.End,
    )
}

@Composable
private fun ScheduleList(result: LoanCalculation) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        result.installments.forEach { installment ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Border),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "第 ${installment.period} 期 · 月供 ${money(installment.payment)}",
                        style = MaterialTheme.typography.titleSmall.copy(color = Ink, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ScheduleValue(label = "本金", value = money(installment.principal))
                        ScheduleValue(label = "利息", value = money(installment.interest))
                        ScheduleValue(label = "剩余本金", value = money(installment.remainingPrincipal))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleValue(
    label: String,
    value: String,
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = MutedInk))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(color = Ink, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun LoanFieldPair(
    wide: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    }
}
