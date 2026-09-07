# 项目规则

- 除非用户明确要求，否则不得主动编写、补充或生成单元测试。
- 除非用户明确要求，否则不得主动执行构建。
- 除非用户明确要求，否则后续任务直接执行，不得主动生成规格文档或实现计划文档。
- 进行任何代码审核时，必须使用 `$ponytail:ponytail` 严格模式。
- Compose 中交互命令不使用 `Button` 或 `TextButton` 包裹 `Text`，直接使用带有 `Modifier` 点击行为、布局和样式的 `Text` 组件。
- Compose 中 `Text` 的颜色、字号、字重等视觉属性统一通过 `TextStyle` 设置；优先使用 `style` 或 `TextStyle.copy`，除非 `TextStyle` 无法满足需求。
- Compose 函数的参数列表中，`modifier: Modifier` 一律放在第一个位置；函数内部调用组件时使用 `modifier = modifier` 的显式参数形式。
- Compose 的 `Column`、`Row` 等布局组件中，如果子项之间的间距是规律的，优先使用 `verticalArrangement = Arrangement.spacedBy(...)` 或 `horizontalArrangement = Arrangement.spacedBy(...)`，不要用多个等间距 `Spacer` 实现。
- Kotlin 普通函数和 Compose 函数的调用一律优先使用命名参数，调用方使用 `参数名 = 值` 传递参数；Compose 组件的 `text`、`style`、`modifier` 等参数同样使用显式命名方式。
