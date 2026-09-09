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

## Compose 状态使用

- 状态必须按控件实际需要的最小颗粒度订阅，并在最接近消费控件的位置声明。优先使用 `viewModel.collectAsState(State::property)`；除非当前 UI 区域确实需要完整状态，否则不得使用 `val state by viewModel.collectAsState()` 后再从 `state` 读取单个属性。
- 一个 Compose UI 区域需要多个相关状态值时，应在 State 中通过派生属性封装为一个不可变模型，再订阅并向控件传递这一个模型；避免分别订阅或向控件传递多个零散状态参数。
- 遵循“状态下沉、事件上升”的单向数据流：控件不得定义或接收 `ViewModel` 参数，只接收状态值（或封装状态模型）和事件回调；`ViewModel` 由页面或容器持有，控件只能上报事件，不得接收 reducer、调用业务状态的 `copy`，也不得执行错误清理等事件处理逻辑，所有状态变更统一在 `ViewModel` 内处理。

正确：

```kotlin
val result by viewModel.collectAsState(CalculatorState::result)
ResultColumn(result = result)
```

错误：

```kotlin
val state by viewModel.collectAsState()
// ...
ResultColumn(result = state.result)
```
