package com.artillery.fehelper.amz

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artillery.fehelper.common.Border
import com.artillery.fehelper.common.BrandBlue
import com.artillery.fehelper.common.Ink
import com.artillery.fehelper.common.MutedInk
import com.artillery.fehelper.common.NumberField
import com.artillery.fehelper.common.PageBackground
import com.artillery.fehelper.common.PageTitleBar
import com.artillery.fehelper.common.SectionCard
import com.artillery.state.StateViewModel
import com.artillery.state.collectAsState
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
    val result: CalculationResult? = null,
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
    if (maximum != null && number > maximum) return "${label}应在 0 到 ${formatAmount(value = maximum)} 之间"
    return null
}

private class AmzCalculatorViewModel : StateViewModel<CalculatorState>(initialState = CalculatorState()) {
    fun updateState(reducer: CalculatorState.() -> CalculatorState) = setState(reducer)

    fun addProduct() {
        setState {
            copy(
                products = products + ProductInput(unitPrice = "0", quantity = "0"),
                result = null,
            )
        }
    }

    fun removeProduct(index: Int) {
        setState {
            copy(
                products = products.filterIndexed { productIndex, _ -> productIndex != index },
                errors = emptyMap(),
                result = null,
            )
        }
    }

    fun confirm() {
        setState {
            var errors = emptyMap<String, String>()
            val result = validateAndCalculate(state = this) { errors = it }
            copy(errors = errors, result = result)
        }
    }
}

@Composable
internal fun AmzCalculatorScreen(onBack: () -> Unit) {
    val viewModel: AmzCalculatorViewModel = viewModel(initializer = { AmzCalculatorViewModel() })
    val state by viewModel.collectAsState()

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
                            modifier = Modifier.fillMaxWidth(),
                            onClick = viewModel::confirm,
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
                        style = MaterialTheme.typography.bodyLarge.copy(color = MutedInk),
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                CalculatorFields(
                                    state = state,
                                    wide = wide,
                                    onStateChange = viewModel::updateState,
                                    onAddProduct = viewModel::addProduct,
                                    onRemoveProduct = viewModel::removeProduct,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                ResultColumn(result = state.result)
                            }
                        }
                    } else {
                        CalculatorFields(
                            state = state,
                            wide = wide,
                            onStateChange = viewModel::updateState,
                            onAddProduct = viewModel::addProduct,
                            onRemoveProduct = viewModel::removeProduct,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ResultColumn(result = state.result)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultColumn(result: CalculationResult?) {
    ResultCard(result = result)
    Spacer(modifier = Modifier.height(16.dp))
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
            Text(text = "计算规则", style = MaterialTheme.typography.labelLarge.copy(color = Ink))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = """
                    运费 = 运费单价 × 货物体积
                    税金 = 产品申报总额 × 报税比例 × 税率 × 汇率
                    产品申报总额 = 各产品（单价 × 数量）之和
                    贴标费用 = 产品总数 × 小标单价 + 箱数 × 大标单价
                    优惠 = 优惠单价 × 货物体积
                    支付金额 = 运费 + 税金 + 贴标费用 - 优惠
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall.copy(color = MutedInk),
            )
        }
    }
}

@Composable
private fun CalculatorFields(
    state: CalculatorState,
    wide: Boolean,
    onStateChange: (CalculatorState.() -> CalculatorState) -> Unit,
    onAddProduct: () -> Unit,
    onRemoveProduct: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionCard(title = "运费", description = "按货物总体积计算") {
            FieldPair(
                wide = wide,
                first = { modifier ->
                    NumberField(
                        modifier = modifier,
                        label = "运费单价",
                        value = state.freightUnitPrice,
                        suffix = "元/立方",
                        error = state.errors["freightUnitPrice"],
                        onValueChange = { value -> onStateChange { copy(freightUnitPrice = value, errors = errors - "freightUnitPrice", result = null) } },
                    )
                },
                second = { modifier ->
                    NumberField(
                        modifier = modifier,
                        label = "货物体积",
                        value = state.volume,
                        suffix = "立方",
                        error = state.errors["volume"],
                        onValueChange = { value -> onStateChange { copy(volume = value, errors = errors - "volume", result = null) } },
                    )
                },
            )
        }
        SectionCard(title = "税金", description = "报税比例和税率请填写百分数") {
            FieldPair(
                wide = wide,
                first = { modifier ->
                    NumberField(
                        modifier = modifier,
                        label = "报税比例",
                        value = state.declarationRate,
                        suffix = "%",
                        error = state.errors["declarationRate"],
                        onValueChange = { value -> onStateChange { copy(declarationRate = value, errors = errors - "declarationRate", result = null) } },
                    )
                },
                second = { modifier ->
                    NumberField(
                        modifier = modifier,
                        label = "税率",
                        value = state.taxRate,
                        suffix = "%",
                        error = state.errors["taxRate"],
                        onValueChange = { value -> onStateChange { copy(taxRate = value, errors = errors - "taxRate", result = null) } },
                    )
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            NumberField(
                label = "汇率",
                value = state.exchangeRate,
                suffix = "兑换比例",
                error = state.errors["exchangeRate"],
                onValueChange = { value -> onStateChange { copy(exchangeRate = value, errors = errors - "exchangeRate", result = null) } },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "产品明细", style = LocalTextStyle.current.copy(color = Ink, fontWeight = FontWeight.SemiBold))
                    Text(text = "按亚马逊上架价格填写报关单价", style = MaterialTheme.typography.bodySmall.copy(color = MutedInk))
                }
                Text(
                    text = "添加产品",
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClick = onAddProduct)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.labelLarge.copy(color = BrandBlue),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            state.products.forEachIndexed { index, product ->
                ProductRow(
                    index = index,
                    product = product,
                    wide = wide,
                    unitPriceError = state.errors["product-$index-unitPrice"],
                    quantityError = state.errors["product-$index-quantity"],
                    onChange = { updated ->
                        onStateChange {
                            copy(
                                products = products.mapIndexed { productIndex, current ->
                                    if (productIndex == index) updated else current
                                },
                                errors = errors - "product-$index-unitPrice" - "product-$index-quantity",
                                result = null,
                            )
                        }
                    },
                    onRemove = { onRemoveProduct(index) },
                    canRemove = state.products.size > 1,
                )
                if (index != state.products.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Border)
                }
            }
        }
        SectionCard(title = "贴标费用", description = "小标按产品数量计算，大标按箱数计算") {
            FieldPair(
                wide = wide,
                first = { modifier ->
                    NumberField(
                        modifier = modifier,
                        label = "小标单价",
                        value = state.smallLabelPrice,
                        suffix = "元/个",
                        error = state.errors["smallLabelPrice"],
                        onValueChange = { value -> onStateChange { copy(smallLabelPrice = value, errors = errors - "smallLabelPrice", result = null) } },
                    )
                },
                second = { modifier ->
                    NumberField(
                        modifier = modifier,
                        label = "大标单价",
                        value = state.largeLabelPrice,
                        suffix = "元/箱",
                        error = state.errors["largeLabelPrice"],
                        onValueChange = { value -> onStateChange { copy(largeLabelPrice = value, errors = errors - "largeLabelPrice", result = null) } },
                    )
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
            NumberField(
                label = "箱数",
                value = state.boxCount,
                suffix = "箱",
                error = state.errors["boxCount"],
                onValueChange = { value -> onStateChange { copy(boxCount = value, errors = errors - "boxCount", result = null) } },
            )
        }
        SectionCard(title = "优惠", description = "按货物体积抵扣，默认无优惠") {
            NumberField(
                label = "优惠单价",
                value = state.discountUnitPrice,
                suffix = "元/立方",
                error = state.errors["discountUnitPrice"],
                onValueChange = { value -> onStateChange { copy(discountUnitPrice = value, errors = errors - "discountUnitPrice", result = null) } },
            )
        }
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
            Text(text = "产品 ${index + 1}", style = LocalTextStyle.current.copy(color = Ink, fontWeight = FontWeight.Medium))
            Text(
                text = "移除",
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(enabled = canRemove, role = Role.Button, onClick = onRemove)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                style = MaterialTheme.typography.labelLarge.copy(color = if (canRemove) BrandBlue else MutedInk),
            )
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
private fun ConfirmButton(modifier: Modifier, onClick: () -> Unit) {
    Text(
        text = "确定",
        modifier = modifier
            .height(52.dp)
            .background(BrandBlue, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        style = MaterialTheme.typography.labelLarge.copy(
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        ),
    )
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
            Text(
                text = "计算结果",
                style = TextStyle(color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (result == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF0F4FC),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "尚未计算", style = LocalTextStyle.current.copy(color = Ink, fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "填写参数后点击“确定”查看支付金额。", style = LocalTextStyle.current.copy(color = MutedInk))
                    }
                }
            } else {
                Text(text = "需要支付金额", style = MaterialTheme.typography.bodyMedium.copy(color = MutedInk))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "¥ ${formatAmount(value = result.total)}",
                    style = TextStyle(color = BrandBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Border)
                Spacer(modifier = Modifier.height(12.dp))
                ResultLine(label = "运费", amount = result.freight)
                ResultLine(label = "税金", amount = result.tax)
                ResultLine(label = "贴标费用", amount = result.labeling)
                ResultLine(label = "优惠", amount = -result.discount)
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
        Text(text = label, style = LocalTextStyle.current.copy(color = MutedInk))
        Text(
            text = if (amount < 0) "- ¥ ${formatAmount(value = -amount)}" else "¥ ${formatAmount(value = amount)}",
            style = LocalTextStyle.current.copy(color = Ink, fontWeight = FontWeight.Medium),
        )
    }
}

private fun validateAndCalculate(
    state: CalculatorState,
    onErrors: (Map<String, String>) -> Unit,
): CalculationResult? {
    val errors = mutableMapOf<String, String>()

    fun read(key: String, value: String, label: String, integer: Boolean = false, maximum: Double? = null): Double {
        val error = parseNumber(value = value, label = label, integer = integer, maximum = maximum)
        if (error != null) errors[key] = error
        return value.toDoubleOrNull() ?: 0.0
    }

    val parsedFreightUnitPrice = read(key = "freightUnitPrice", value = state.freightUnitPrice, label = "运费单价")
    val parsedVolume = read(key = "volume", value = state.volume, label = "货物体积")
    val parsedDeclarationRate = read(key = "declarationRate", value = state.declarationRate, label = "报税比例", maximum = 100.0) / 100
    val parsedTaxRate = read(key = "taxRate", value = state.taxRate, label = "税率", maximum = 100.0) / 100
    val parsedExchangeRate = read(key = "exchangeRate", value = state.exchangeRate, label = "汇率")
    val parsedProducts = state.products.mapIndexed { index, product ->
        ProductValues(
            unitPrice = read(key = "product-$index-unitPrice", value = product.unitPrice, label = "第 ${index + 1} 个产品单价"),
            quantity = read(key = "product-$index-quantity", value = product.quantity, label = "第 ${index + 1} 个产品数量", integer = true),
        )
    }
    val parsedSmallLabelPrice = read(key = "smallLabelPrice", value = state.smallLabelPrice, label = "小标单价")
    val parsedLargeLabelPrice = read(key = "largeLabelPrice", value = state.largeLabelPrice, label = "大标单价")
    val parsedBoxCount = read(key = "boxCount", value = state.boxCount, label = "箱数", integer = true)
    val parsedDiscountUnitPrice = read(key = "discountUnitPrice", value = state.discountUnitPrice, label = "优惠单价")

    onErrors(errors)
    if (errors.isNotEmpty()) return null

    return calculate(
        input = CalculatorInput(
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
