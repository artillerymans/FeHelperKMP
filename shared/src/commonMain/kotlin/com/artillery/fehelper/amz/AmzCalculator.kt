package com.artillery.fehelper.amz

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.NumberField
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import kotlin.math.round

private data class ProductInput(
    val unitPrice: String,
    val quantity: String,
)

private data class CalculatorState(
    val freightUnitPrice: String = "700",
    val volume: String = "2.1",
    val declarationRate: String = "20",
    val taxRate: String = "13",
    val exchangeRate: String = "0.045",
    val products: List<ProductInput> = listOf(
        ProductInput(unitPrice = "8999", quantity = "90"),
    ),
    val smallLabelPrice: String = "1",
    val largeLabelPrice: String = "3",
    val boxCount: String = "17",
    val discountUnitPrice: String = "0",
    val errors: Map<String, String> = emptyMap(),
)

private data class ProductValues(
    val unitPrice: Double,
    val quantity: Double,
)

private data class CalculatorInput(
    val freightUnitPrice: Double,
    val volume: Double,
    val declarationRate: Double,
    val taxRate: Double,
    val exchangeRate: Double,
    val products: List<ProductValues>,
    val smallLabelPrice: Double,
    val largeLabelPrice: Double,
    val boxCount: Double,
    val discountUnitPrice: Double,
)

private data class CalculationResult(
    val freight: Double,
    val tax: Double,
    val labeling: Double,
    val discount: Double,
    val total: Double,
)

private fun calculate(input: CalculatorInput): CalculationResult {
    val productValue = input.products.sumOf { it.unitPrice * it.quantity }
    val totalQuantity = input.products.sumOf { it.quantity }
    val freight = input.freightUnitPrice * input.volume
    val tax = productValue * input.declarationRate * input.taxRate * input.exchangeRate
    val labeling = totalQuantity * input.smallLabelPrice + input.boxCount * input.largeLabelPrice
    val discount = input.discountUnitPrice * input.volume

    return CalculationResult(
        freight = freight,
        tax = tax,
        labeling = labeling,
        discount = discount,
        total = freight + tax + labeling - discount,
    )
}

private fun formatAmount(value: Double): String {
    val rounded = round(value * 100) / 100
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}

private fun parseNumber(value: String, label: String, integer: Boolean, maximum: Double? = null): String? {
    val number = value.toDoubleOrNull() ?: return "${label}请输入数字"
    if (!number.isFinite()) return "${label}请输入有效数字"
    if (number < 0) return "${label}不能小于 0"
    if (integer && number % 1 != 0.0) return "${label}请输入整数"
    if (maximum != null && number > maximum) return "${label}应在 0 到 ${formatAmount(maximum)} 之间"
    return null
}

@Composable
internal fun AmzCalculatorScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf(CalculatorState()) }
    var result by remember { mutableStateOf<CalculationResult?>(null) }

    fun confirm() {
        result = validateAndCalculate(state) { state = state.copy(errors = it) }
    }

    val updateState: (CalculatorState) -> Unit = {
        state = it
        result = null
    }
    val addProduct = {
        state = state.copy(products = state.products + ProductInput("0", "0"))
        result = null
    }
    val removeProduct: (Int) -> Unit = { index ->
        state = state.copy(
            products = state.products.filterIndexed { productIndex, _ -> productIndex != index },
            errors = emptyMap(),
        )
        result = null
    }

    BoxWithConstraints(
        modifier = Modifier
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
                    title = "AMZ 水票计算",
                    horizontalPadding = horizontalPadding,
                    onBack = onBack,
                )
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 50.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        ConfirmButton(
                            onClick = ::confirm,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .widthIn(max = 1200.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPadding, vertical = 24.dp),
                ) {
                    Text(
                        text = "估算亚马逊货物的运费、税金、贴标费用和优惠后的支付金额",
                        color = MutedInk,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(24.dp))

                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                CalculatorFields(
                                    state = state,
                                    wide = wide,
                                    onStateChange = updateState,
                                    onAddProduct = addProduct,
                                    onRemoveProduct = removeProduct,
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                ResultColumn(result)
                            }
                        }
                    } else {
                        CalculatorFields(
                            state = state,
                            wide = wide,
                            onStateChange = updateState,
                            onAddProduct = addProduct,
                            onRemoveProduct = removeProduct,
                        )
                        Spacer(Modifier.height(16.dp))
                        ResultColumn(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultColumn(result: CalculationResult?) {
    ResultCard(result)
    Spacer(Modifier.height(16.dp))
    CalculationRules()
}

@Composable
private fun CalculationRules() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("计算规则", color = Ink, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                text = """
                    运费 = 运费单价 × 货物体积
                    税金 = 产品申报总额 × 报税比例 × 税率 × 汇率
                    产品申报总额 = 各产品（单价 × 数量）之和
                    贴标费用 = 产品总数 × 小标单价 + 箱数 × 大标单价
                    优惠 = 优惠单价 × 货物体积
                    支付金额 = 运费 + 税金 + 贴标费用 - 优惠
                """.trimIndent(),
                color = MutedInk,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CalculatorFields(
    state: CalculatorState,
    wide: Boolean,
    onStateChange: (CalculatorState) -> Unit,
    onAddProduct: () -> Unit,
    onRemoveProduct: (Int) -> Unit,
) {
    SectionCard(title = "运费", description = "按货物总体积计算") {
        FieldPair(
            wide = wide,
            first = { modifier ->
                NumberField(
                    "运费单价",
                    state.freightUnitPrice,
                    "元/立方",
                    state.errors["freightUnitPrice"],
                    { onStateChange(state.copy(freightUnitPrice = it, errors = state.errors - "freightUnitPrice")) },
                    modifier,
                )
            },
            second = { modifier ->
                NumberField(
                    "货物体积",
                    state.volume,
                    "立方",
                    state.errors["volume"],
                    { onStateChange(state.copy(volume = it, errors = state.errors - "volume")) },
                    modifier,
                )
            },
        )
    }
    Spacer(Modifier.height(16.dp))

    SectionCard(title = "税金", description = "报税比例和税率请填写百分数") {
        FieldPair(
            wide = wide,
            first = { modifier ->
                NumberField(
                    "报税比例",
                    state.declarationRate,
                    "%",
                    state.errors["declarationRate"],
                    { onStateChange(state.copy(declarationRate = it, errors = state.errors - "declarationRate")) },
                    modifier,
                )
            },
            second = { modifier ->
                NumberField(
                    "税率",
                    state.taxRate,
                    "%",
                    state.errors["taxRate"],
                    { onStateChange(state.copy(taxRate = it, errors = state.errors - "taxRate")) },
                    modifier,
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        NumberField(
            "汇率",
            state.exchangeRate,
            "兑换比例",
            state.errors["exchangeRate"],
            { onStateChange(state.copy(exchangeRate = it, errors = state.errors - "exchangeRate")) },
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("产品明细", fontWeight = FontWeight.SemiBold, color = Ink)
                Text("按亚马逊上架价格填写报关单价", color = MutedInk, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onAddProduct) {
                Text("添加产品")
            }
        }
        Spacer(Modifier.height(4.dp))
        state.products.forEachIndexed { index, product ->
            ProductRow(
                index = index,
                product = product,
                wide = wide,
                unitPriceError = state.errors["product-$index-unitPrice"],
                quantityError = state.errors["product-$index-quantity"],
                onChange = { updated ->
                    onStateChange(
                        state.copy(
                            products = state.products.mapIndexed { productIndex, current ->
                                if (productIndex == index) updated else current
                            },
                            errors = state.errors - "product-$index-unitPrice" - "product-$index-quantity",
                        ),
                    )
                },
                onRemove = { onRemoveProduct(index) },
                canRemove = state.products.size > 1,
            )
            if (index != state.products.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Border)
            }
        }
    }
    Spacer(Modifier.height(16.dp))

    SectionCard(title = "贴标费用", description = "小标按产品数量计算，大标按箱数计算") {
        FieldPair(
            wide = wide,
            first = { modifier ->
                NumberField(
                    "小标单价",
                    state.smallLabelPrice,
                    "元/个",
                    state.errors["smallLabelPrice"],
                    { onStateChange(state.copy(smallLabelPrice = it, errors = state.errors - "smallLabelPrice")) },
                    modifier,
                )
            },
            second = { modifier ->
                NumberField(
                    "大标单价",
                    state.largeLabelPrice,
                    "元/箱",
                    state.errors["largeLabelPrice"],
                    { onStateChange(state.copy(largeLabelPrice = it, errors = state.errors - "largeLabelPrice")) },
                    modifier,
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        NumberField(
            "箱数",
            state.boxCount,
            "箱",
            state.errors["boxCount"],
            { onStateChange(state.copy(boxCount = it, errors = state.errors - "boxCount")) },
        )
    }
    Spacer(Modifier.height(16.dp))

    SectionCard(title = "优惠", description = "按货物体积抵扣，默认无优惠") {
        NumberField(
            "优惠单价",
            state.discountUnitPrice,
            "元/立方",
            state.errors["discountUnitPrice"],
            { onStateChange(state.copy(discountUnitPrice = it, errors = state.errors - "discountUnitPrice")) },
        )
    }
}

@Composable
private fun FieldPair(
    wide: Boolean,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProductRow(
    index: Int,
    product: ProductInput,
    wide: Boolean,
    unitPriceError: String?,
    quantityError: String?,
    onChange: (ProductInput) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("产品 ${index + 1}", color = Ink, fontWeight = FontWeight.Medium)
            TextButton(onClick = onRemove, enabled = canRemove) {
                Text("移除")
            }
        }
        FieldPair(
            wide = wide,
            first = { modifier ->
                NumberField(
                    label = "产品 ${index + 1} 单价",
                    value = product.unitPrice,
                    suffix = "原币",
                    error = unitPriceError,
                    onValueChange = { onChange(product.copy(unitPrice = it)) },
                    modifier = modifier,
                )
            },
            second = { modifier ->
                NumberField(
                    label = "产品 ${index + 1} 数量",
                    value = product.quantity,
                    suffix = "个",
                    error = quantityError,
                    onValueChange = { onChange(product.copy(quantity = it)) },
                    modifier = modifier,
                )
            },
        )
    }
}

@Composable
private fun ConfirmButton(onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        Text("确定", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ResultCard(result: CalculationResult?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("计算结果", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            if (result == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF0F4FC),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("尚未计算", color = Ink, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("填写参数后点击“确定”查看支付金额。", color = MutedInk)
                    }
                }
            } else {
                Text("需要支付金额", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "¥ ${formatAmount(result.total)}",
                    color = BrandBlue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(12.dp))
                ResultLine("运费", result.freight)
                ResultLine("税金", result.tax)
                ResultLine("贴标费用", result.labeling)
                ResultLine("优惠", -result.discount)
            }
        }
    }
}

@Composable
private fun ResultLine(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MutedInk)
        Text(
            text = if (amount < 0) "- ¥ ${formatAmount(-amount)}" else "¥ ${formatAmount(amount)}",
            color = Ink,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun validateAndCalculate(
    state: CalculatorState,
    onErrors: (Map<String, String>) -> Unit,
): CalculationResult? {
    val errors = mutableMapOf<String, String>()

    fun read(key: String, value: String, label: String, integer: Boolean = false, maximum: Double? = null): Double {
        val error = parseNumber(value, label, integer, maximum)
        if (error != null) errors[key] = error
        return value.toDoubleOrNull() ?: 0.0
    }

    val parsedFreightUnitPrice = read("freightUnitPrice", state.freightUnitPrice, "运费单价")
    val parsedVolume = read("volume", state.volume, "货物体积")
    val parsedDeclarationRate = read("declarationRate", state.declarationRate, "报税比例", maximum = 100.0) / 100
    val parsedTaxRate = read("taxRate", state.taxRate, "税率", maximum = 100.0) / 100
    val parsedExchangeRate = read("exchangeRate", state.exchangeRate, "汇率")
    val parsedProducts = state.products.mapIndexed { index, product ->
        ProductValues(
            unitPrice = read("product-$index-unitPrice", product.unitPrice, "第 ${index + 1} 个产品单价"),
            quantity = read("product-$index-quantity", product.quantity, "第 ${index + 1} 个产品数量", integer = true),
        )
    }
    val parsedSmallLabelPrice = read("smallLabelPrice", state.smallLabelPrice, "小标单价")
    val parsedLargeLabelPrice = read("largeLabelPrice", state.largeLabelPrice, "大标单价")
    val parsedBoxCount = read("boxCount", state.boxCount, "箱数", integer = true)
    val parsedDiscountUnitPrice = read("discountUnitPrice", state.discountUnitPrice, "优惠单价")

    onErrors(errors)
    if (errors.isNotEmpty()) return null

    return calculate(
        CalculatorInput(
            freightUnitPrice = parsedFreightUnitPrice,
            volume = parsedVolume,
            declarationRate = parsedDeclarationRate,
            taxRate = parsedTaxRate,
            exchangeRate = parsedExchangeRate,
            products = parsedProducts,
            smallLabelPrice = parsedSmallLabelPrice,
            largeLabelPrice = parsedLargeLabelPrice,
            boxCount = parsedBoxCount,
            discountUnitPrice = parsedDiscountUnitPrice,
        ),
    )
}
