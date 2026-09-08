# core-state

`core-state` 是一个面向 Kotlin Multiplatform 的轻量 Compose UI 状态管理模块，包名为 `com.artillery.state`。

模块采用 MVI 风格：ViewModel 持有唯一状态，UI 通过状态流观察，状态修改通过 reducer 生成新状态。实现依赖 Lifecycle ViewModel、Compose Runtime 和 Kotlin Coroutines，可用于 JVM、JS、WasmJs 目标，也可以作为其他 KMP 项目的基础模块复用。

## 能力范围

- 使用 `StateFlow` 暴露只读状态。
- 使用 FIFO reducer 队列异步处理 `setState`。
- 使用 `withState` 在队列中读取已处理的最新状态。
- 使用 Compose `collectAsState` 订阅完整状态或状态片段。
- 支持属性引用、嵌套属性和派生属性选择器。
- 继承 Lifecycle `ViewModel`，销毁时自动停止队列并释放协程资源。

模块当前不负责状态持久化、事件总线、网络请求封装或副作用管理；这些能力应由宿主项目按平台和业务需要补充。

## 引入模块

在同一 Gradle 工程中添加：

```kotlin
commonMain.dependencies {
    implementation(project(":core-state"))
}
```

模块自身已经通过 `api` 暴露以下依赖：

- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`
- `org.jetbrains.compose.runtime:runtime`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`

## API

### `StateViewModel<S>`

包：`com.artillery.state.StateViewModel`

```kotlin
open class StateViewModel<S : Any>(
    initialState: S,
    coroutineContext: CoroutineContext = Dispatchers.Default,
) : ViewModel(
    viewModelScope = CoroutineScope(context = coroutineContext + Job()),
)
```

| API | 作用 |
| --- | --- |
| `state: StateFlow<S>` | 暴露当前状态流，只读访问。首次订阅会得到 `initialState`。 |
| `protected setState(reducer: S.() -> S)` | 将 reducer 放入异步 FIFO 队列，调用方不会等待 reducer 执行。 |
| `protected withState(action: (S) -> Unit)` | 将读取动作放入同一队列，保证它执行前先处理已经排队的 reducer。 |
| `coroutineContext` | 配置 reducer 队列使用的协程上下文；默认使用 `Dispatchers.Default`。 |

### Compose `collectAsState`

包：`com.artillery.state`

```kotlin
@Composable
fun <S : Any> StateViewModel<S>.collectAsState(): State<S>

@Composable
fun <S : Any, T> StateViewModel<S>.collectAsState(
    selector: (S) -> T,
): State<T>
```

- 无参数重载返回 Compose 的 `State<S>`，适合观察整个状态。
- 选择器重载返回 Compose 的 `State<T>`，适合通过 `by` 只观察页面需要的属性。
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

### 3. 在 Compose 中创建并观察 ViewModel

宿主模块需要引入 Lifecycle 的 Compose 集成：

```kotlin
commonMain.dependencies {
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.viewmodelNavigation3)
}
```

使用 Navigation3 时，在 `NavDisplay` 上统一添加官方 ViewModel decorator。它会为每个页面提供独立的 `LocalViewModelStoreOwner`，并在页面出栈时清理 ViewModel：

```kotlin
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator

NavDisplay(
    backStack = backStack,
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider,
)
```

页面直接通过 initializer 创建并观察 ViewModel：

```kotlin
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
private fun CounterScreen() {
    val viewModel: CounterViewModel = viewModel(initializer = { CounterViewModel() })
    val state by viewModel.collectAsState()
    val count by viewModel.collectAsState(selector = CounterState::count)

    Text(text = "Count is ${state.count}")
    Text(text = "Count is $count")
}
```

`viewModel()` 会复用当前页面 `LocalViewModelStoreOwner` 中的实例，并在页面出栈时触发 `StateViewModel.onCleared()`。非 Android 平台不能依赖无参构造反射，因此应传入 initializer；不要使用 `remember { CounterViewModel() }` 手动创建，也不要在每个页面重复创建 factory 或 owner。

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

`StateViewModel` 继承 `androidx.lifecycle.ViewModel`。通过 `ViewModelProvider` 或 Compose `viewModel()` 创建后，reducer 协程和队列会随 `ViewModelStoreOwner` 自动清理。

## 目录结构

```text
core-state/
├── build.gradle.kts
├── README.md
└── src/commonMain/kotlin/com/artillery/state/
    ├── ComposeState.kt
    └── StateViewModel.kt
```
