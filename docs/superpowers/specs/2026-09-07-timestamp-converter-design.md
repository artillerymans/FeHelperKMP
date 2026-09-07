# 时间（戳）转换设计

## 目标

为 FeHelperKMP 增加“时间（戳）转换”工具，提供本地化时间与 Unix 时间戳的双向转换，并以 `Asia/Shanghai` 作为本地时间基准，补充固定 UTC 偏移的世界时钟展示。

## 范围

- 实时显示 `Asia/Shanghai` 当前时间、Unix 秒和 Unix 毫秒，默认每秒刷新；支持手动暂停和恢复，暂停时冻结当前快照，恢复时立即校准到系统当前时间。
- Unix 时间戳转换为 `Asia/Shanghai` 本地时间，支持秒/毫秒单位切换。
- 支持 `yyyy`、`yyyy-MM`、`yyyy-MM-dd`、`yyyy-MM-dd HH`、`yyyy-MM-dd HH:mm` 和 `yyyy-MM-dd HH:mm:ss` 本地时间转换为 Unix 秒和 Unix 毫秒；缺少的月/日/时/分/秒按 `1/1/00:00:00` 补齐，输入固定按 `Asia/Shanghai` 解析。
- 当前时间卡片中的本地时间、Unix 秒和 Unix 毫秒支持鼠标选择和复制。
- 展示 `GMT-12` 至 `GMT+12` 的 25 个固定偏移时钟，以 `Asia/Shanghai` 的当前时刻为基准计算。
- 点击世界时钟条目后保留选中状态，便于识别当前查看的偏移。
- 对空输入、非法数字、`Long` 范围溢出和非法日期格式显示就近错误提示。

不包含 Windows FILETIME、任意 IANA 时区数据库选择、历史时间查询或数据持久化。

## 技术方案

- 在 `shared` 的 `commonMain` 增加 `kotlinx-datetime` 依赖，统一 JVM、JS 和 Wasm 的时间计算。
- 使用 `Clock.System.now()` 获取当前 `Instant`。
- 使用 `TimeZone.of("Asia/Shanghai")` 将时间转换为本地时间；世界时钟使用 `UtcOffset` 做固定偏移换算。
- 保持现有 Compose 页面结构，新增独立的时间转换屏幕与纯 Kotlin 转换工具，入口继续由 `App.kt` 的 Navigation 3 管理。

## UI 结构

页面沿用现有白底、细边框、圆角卡片和 `PageTitleBar`。采用高密度工具台布局：

1. 顶部实时状态卡片，三项数据横向排列，窄屏自动换行；提供开始/暂停刷新按钮，并显示当前运行状态。
2. 宽屏时将两个转换卡片并排，窄屏纵向排列。
3. 世界时钟使用可滚动的双列网格，选中项使用主题色边框和背景强调。

输入控件有明确可见标签，按钮和选择器保持至少 48dp 高度，页面整体支持滚动和安全区域内边距。

## 数据流

```text
Clock.System.now()
  -> Asia/Shanghai 当前时间与 Unix 秒/毫秒
  -> 25 个固定 UTC 偏移的世界时钟

用户输入 Unix 数字 + 单位
  -> Instant.fromEpochSeconds/fromEpochMilliseconds
  -> Asia/Shanghai LocalDateTime

用户输入本地日期字符串
  -> LocalDateTime.parse
  -> Asia/Shanghai 时区 Instant
  -> Unix 秒/毫秒
```

## 错误与边界

- 时间戳仅接受十进制整数；秒/毫秒转换时捕获 `Long` 溢出和 `Instant` 范围错误。
- 日期输入必须符合上述支持的日期精度，界面提示格式要求；日期或时间组件超出有效范围时提示无效。
- 转换失败只更新当前转换区块的错误和结果，不清空其他区块。
- 当前时间刷新使用 Compose 协程定时器，由运行状态控制；暂停时取消刷新并保留当前快照，恢复时立即读取系统当前时间；离开页面时随组合生命周期取消。

## 验证边界

按项目规则本次不主动新增测试、不主动执行 Gradle 构建。实现后执行 `git diff --check`、源码检索和静态审查；构建、运行和跨平台手工验证留给明确授权后的下一步。
