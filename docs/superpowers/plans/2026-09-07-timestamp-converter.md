# 时间（戳）转换 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 FeHelperKMP 中增加以 `Asia/Shanghai` 为基准的 Unix 时间戳双向转换和固定 UTC 偏移世界时钟工具。

**Architecture:** 使用 `kotlinx-datetime` 在 `commonMain` 提供无平台差异的时间解析、格式化和偏移换算；Compose 屏幕只持有输入/结果/错误状态，并复用现有页面标题栏、卡片和主题。`App.kt` 注册一个新的 Navigation 目的地和首页工具入口。

**Tech Stack:** Kotlin Multiplatform、Compose Multiplatform Material 3、Navigation 3、`kotlinx-datetime`。

## Global Constraints

- 本地时间固定按 `Asia/Shanghai` 解析和展示。
- 世界时钟固定展示 `GMT-12` 至 `GMT+12`，不引入任意 IANA 时区选择。
- 输入错误在当前区块就近显示，不清空其他区块结果。
- 复用现有白底、细边框、圆角卡片和主题色；控件最小高度保持 48dp。
- 不新增测试，不主动执行 Gradle 构建；只运行 `git diff --check` 和静态源码检查。
- 不实现 Windows FILETIME、历史时间查询或持久化。

---

### Task 1: Add cross-platform time conversion primitives

**Files:**
- Modify: `gradle/libs.versions.toml` (add the `kotlinx-datetime` version and library alias)
- Modify: `shared/build.gradle.kts` (add the common dependency)
- Create: `shared/src/commonMain/kotlin/com/artillery/fehelper/time/TimestampConverter.kt`

**Interfaces:**
- Produces `TimestampUnit`, `TimestampConversion`, `WorldClock`, `nowSnapshot()`, `timestampToLocalTime()`, `localTimeToTimestamp()`, and `worldClocks()` for the Compose screen.

- [x] **Step 1: Add the dependency aliases**

Add `kotlinx-datetime = "0.7.1"` under `[versions]` and `kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }` under `[libraries]`; add `implementation(libs.kotlinx.datetime)` to `commonMain.dependencies`.

- [x] **Step 2: Implement the pure conversion API**

Create a focused common source file with:

```kotlin
internal enum class TimestampUnit(val label: String) { SECONDS("秒"), MILLISECONDS("毫秒") }
internal data class TimestampConversion(val localTime: String?, val seconds: String?, val milliseconds: String?, val error: String?)
internal data class TimeSnapshot(val localTime: String, val seconds: String, val milliseconds: String)
internal data class WorldClock(val offsetHours: Int, val label: String, val localTime: String)

internal fun timestampToLocalTime(value: String, unit: TimestampUnit): TimestampConversion
internal fun localTimeToTimestamp(value: String): TimestampConversion
internal fun nowSnapshot(now: Instant = Clock.System.now()): TimeSnapshot
internal fun worldClocks(now: Instant = Clock.System.now()): List<WorldClock>
```

Parse only decimal `Long` values for timestamps, catch invalid `Instant` ranges, accept `yyyy`, `yyyy-MM`, `yyyy-MM-dd`, `yyyy-MM-dd HH`, `yyyy-MM-dd HH:mm`, and `yyyy-MM-dd HH:mm:ss` local inputs (missing components default to `1/1/00:00:00`), normalize space or `T` separators, and format every result as `yyyy-MM-dd HH:mm:ss` in `Asia/Shanghai`. Use `Instant.toEpochMilliseconds()` for milliseconds and `Instant.epochSeconds` (floor-second semantics) for negative values so pre-epoch values remain correct. Generate offsets `-12..12`, label zero as `GMT+0`, and derive each clock from the same `Instant` with `UtcOffset(hours = offset)`.

- [x] **Step 3: Static-check the new API surface**

Run `rg -n "TimestampUnit|TimestampConversion|TimeSnapshot|WorldClock|timestampToLocalTime|localTimeToTimestamp|nowSnapshot|worldClocks" shared/src/commonMain/kotlin/com/artillery/fehelper/time/TimestampConverter.kt` and confirm every declared symbol has one definition and no platform import.

### Task 2: Build the Compose timestamp converter screen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/artillery/fehelper/time/TimestampConverterScreen.kt`

**Interfaces:**
- Consumes the conversion API from Task 1 and `onBack: () -> Unit`.
- Produces `TimestampConverterScreen(onBack: () -> Unit)` for `App.kt`.

- [x] **Step 1: Add state and one-second refresh**

Use `remember` for timestamp input, local input, selected units, conversion results/errors, selected world-clock offset, current `Instant`, and the realtime running state. Use `LaunchedEffect(isRunning)` to refresh once immediately and then every second while running; pausing cancels the loop and freezes the snapshot, resuming re-reads `Clock.System.now()`, and leaving the page cancels the effect.

- [x] **Step 2: Compose the responsive page shell**

Match existing screens with `BoxWithConstraints`, `PageBackground`, `safeContentPadding`, `Scaffold`, `PageTitleBar`, a max content width of 1200dp, and a vertically scrolling column. Use 16dp gutters below 900dp and 32dp above it.

- [x] **Step 3: Compose the realtime card and conversion cards**

Render the current `Asia/Shanghai` time, seconds, and milliseconds in a three-column row that wraps to one column on narrow widths; wrap each value in `SelectionContainer` so mouse selection and copy work. Render timestamp-to-local and local-to-timestamp forms with visible labels, `OutlinedTextField`, `FilterChip` unit selectors, `Button` actions, inline error text, and read-only result fields. Keep action controls at least 48dp high.

- [x] **Step 4: Compose the world-clock grid**

Render the 25 `WorldClock` entries in two columns on wide screens and one column on narrow screens. Make each entry a semantic clickable `Surface`, apply the existing theme colors plus `primaryContainer` for the selected offset, and show the GMT label and local time without nesting another card inside `SectionCard`.

- [x] **Step 5: Static-check screen references**

Run `rg -n "TimestampConverterScreen|timestampToLocalTime|localTimeToTimestamp|worldClocks|LaunchedEffect|delay" shared/src/commonMain/kotlin/com/artillery/fehelper/time/TimestampConverterScreen.kt` and `git diff --check`.

### Task 3: Register the tool in navigation and catalog

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/artillery/fehelper/App.kt`

**Interfaces:**
- Adds the `TimestampConverter` tool definition, `Destination.TIMESTAMP_CONVERTER`, click routing, and `entry` rendering.

- [x] **Step 1: Add the tool metadata and destination**

Add the constant `timestamp-converter`, a catalog item titled `时间（戳）转换` with description `本地时间与 Unix 时间戳互转，支持秒、毫秒和世界时钟`, and a destination enum value.

- [x] **Step 2: Wire navigation**

Route the tool id in `HomeScreen` click handling and add a `NavDisplay` entry that renders `TimestampConverterScreen(onBack = navigateBack)`.

- [x] **Step 3: Static-check integration**

Run `rg -n "timestamp-converter|时间（戳）转换|TIMESTAMP_CONVERTER|TimestampConverterScreen" shared/src/commonMain/kotlin/com/artillery/fehelper/App.kt` and `git diff --check`.

### Task 4: Final static verification and ponytail review

**Files:**
- Review all changed files from Tasks 1-3.

- [x] **Step 1: Check worktree and diff scope**

Run `git status --short`, `git diff --stat`, `git diff --check`, and inspect the complete diff. Confirm no unrelated files, generated artifacts, tests, or build output were added.

- [x] **Step 2: Verify required behavior statically**

Search for `Asia/Shanghai`, both timestamp units, `GMT-12`, `GMT+12`, `Clock.System.now`, `delay(1000)`, and inline error rendering. Confirm all are present in the intended files.

- [x] **Step 3: Apply strict ponytail review**

Review for duplicate helpers, unnecessary abstractions, unused imports, extra dependencies, duplicated conversion logic, missing input validation, and controls below 48dp. Remove any confirmed redundancy without broad refactoring.

- [x] **Step 4: Report validation boundary**

Report static checks as completed; explicitly state that Gradle build, tests, browser rendering, and runtime cross-platform verification were not run because the project rules prohibit them without explicit authorization.
