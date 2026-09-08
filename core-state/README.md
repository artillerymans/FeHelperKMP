# core-state

`core-state` 是一个面向 Kotlin Multiplatform 的轻量 Compose UI 状态管理模块，包名为 `com.artillery.state`。

模块采用 MVI 风格：ViewModel 持有唯一状态，UI 通过状态流观察，状态修改通过 reducer 生成新状态。实现只依赖 Compose Runtime 和 Kotlin Coroutines，可用于 JVM、JS、WasmJs 目标，也可以作为其他 KMP 项目的基础模块复用。

## 能力范围

- 使用 `StateFlow` 暴露只读状态。
- 使用 FIFO reducer 队列异步处理 `setState`。
- 使用 `withState` 在队列中读取已处理的最新状态。
- 使用 Compose `collectAsState` 订阅完整状态或状态片段。
- 支持属性引用、嵌套属性和派生属性选择器。
- 通过 `clear()` 停止队列并释放协程资源。

模块当前不负责 ViewModel 生命周期自动绑定、持久化、事件总线、网络请求封装或副作用管理；这些能力应由宿主项目按平台和业务需要补充。

## 引入模块

在同一 Gradle 工程中添加：

```kotlin
commonMain.dependencies {
    implementation(project(":core-state"))
}
```

模块自身已经通过 `api` 暴露以下依赖：

- `org.jetbrains.compose.runtime:runtime`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`

## API

### `StateViewModel<S>`

包：`com.artillery.state.StateViewModel`

```kotlin
open class StateViewModel<S : Any>(
    initialState: S,
    coroutineContext: CoroutineContext = Dispatchers.Default,
)
```

| API | 作用 |
| --- | --- |
| `state: StateFlow<S>` | 暴露当前状态流，只读访问。首次订阅会得到 `initialState`。 |
| `protected setState(reducer: S.() -> S)` | 将 reducer 放入异步 FIFO 队列，调用方不会等待 reducer 执行。 |
| `protected withState(action: (S) -> Unit)` | 将读取动作放入同一队列，保证它执行前先处理已经排队的 reducer。 |
| `clear()` | 关闭队列、取消内部协程。清理后再次调用 `setState` 或 `withState` 会抛出 `IllegalStateException`。 |
| `coroutineContext` | 配置 reducer 队列使用的协程上下文；默认使用 `Dispatchers.Default`。 |

### Compose `collectAsState`

包：`com.artillery.state`

```kotlin
@Composable
fun <S : Any> StateViewModel<S>.collectAsState(): State<S>

@Composable
fun <S : Any, T> StateViewModel<S>.collectAsState(
    selector: (S) -> T,
): T
```

- 无参数重载返回 Compose 的 `State<S>`，适合观察整个状态。
- 选择器重载直接返回 `T`，适合只观察页面需要的属性。
- 选择器结果使用 Compose 状态的默认相等性策略，结果未变化时不会触发额外重组。

## 使用方法

### 1. 定义状态

状态建议使用不可变 `data class`。可以直接在状态中定义派生属性：

```kotlin
data class CounterState(
    val count: Int = 0,
) {
    val isEven: Boolean
        get() = count % 2 == 0
}
```

### 2. 创建 ViewModel

`setState` 和 `withState` 是 ViewModel 内部的状态读写入口：

```kotlin
class CounterViewModel(
    initialState: CounterState = CounterState(),
) : StateViewModel<CounterState>(initialState = initialState) {

    fun increment() {
        setState { copy(count = count + 1) }
    }

    fun printLatestState() {
        withState { currentState ->
            println("count=${currentState.count}, isEven=${currentState.isEven}")
        }
    }
}
```

每次 reducer 都应基于当前状态返回新对象，不要在状态对象内部原地修改可变集合或字段。

### 3. 在 Compose 中观察状态

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val state by viewModel.collectAsState()
    val count1 = viewModel.collectAsState(CounterState::count)
    val count2 = viewModel.collectAsState { it.count }

    Text(text = "Count is ${state.count}")
    Text(text = "Count is $count1")
    Text(text = "Count is $count2")
}
```

使用 `by` 解包完整状态时，需要在调用方导入：

```kotlin
import androidx.compose.runtime.getValue
```

### 4. 读取排队后的状态

`setState` 不会同步执行 reducer。`withState` 会进入同一个 FIFO 队列，因此会在它之前已经排队的 reducer 执行完成后读取状态：

```kotlin
fun setAndRetrieveState() {
    println("A")
    setState {
        println("B")
        copy(count = 1)
    }
    println("C")
    withState { currentState ->
        println("D: ${currentState.count}")
    }
    println("E")
}
```

在默认后台调度器下，通常会看到 `A`、`C`、`E` 先输出，再输出 reducer 和读取回调。模块的确定性保证是：队列中的 reducer 按提交顺序执行，并且 `withState` 不会越过它之前排队的 reducer；具体日志交错仍受传入 `CoroutineContext` 的调度策略影响。

## 生命周期

`StateViewModel` 不依赖 Android `ViewModel` 或 Lifecycle。宿主需要在不再使用实例时主动调用：

```kotlin
viewModel.clear()
```

如果宿主已有生命周期容器，应在容器销毁回调中调用 `clear()`。

## 目录结构

```text
core-state/
├── build.gradle.kts
└── src/commonMain/kotlin/com/artillery/state/
    ├── ComposeState.kt
    └── StateViewModel.kt
```
