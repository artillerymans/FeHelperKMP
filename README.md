# FeHelperKMP

面向 Web 和 Desktop 的前端工具集合项目，工具方向参考 [FeHelper](https://fehelper.com/)。本项目独立开发，与 FeHelper 官方项目不存在从属或官方关联关系。

## 当前状态

- 已实现“AMZ 水票计算”模块，可根据运费、税金、贴标费用和优惠规则计算需要支付的金额。
- 已实现“JSON 格式化”模块，支持格式化、排序、解码和表格视图。
- 计算参数均可手动输入，并提供默认值。
- 首页作为工具入口，支持搜索以及列表、网格两种展示方式。
- 更多前端工具会继续按计划逐项加入。

## 开发计划

后续会参考 FeHelper 的工具方向，逐项加入各类前端开发和日常工作工具。具体工具顺序和发布时间以实际开发进度为准。

## 技术栈

- Kotlin Multiplatform
- Compose Multiplatform
- Navigation 3
- Web：Kotlin/Wasm、Kotlin/JS
- Desktop：JVM

## 目录说明

- [`shared/src/commonMain/kotlin/com/artillery/fehelper`](./shared/src/commonMain/kotlin/com/artillery/fehelper)：应用首页和工具入口路由。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/amz`](./shared/src/commonMain/kotlin/com/artillery/fehelper/amz)：AMZ 水票计算功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/json`](./shared/src/commonMain/kotlin/com/artillery/fehelper/json)：JSON 格式化、排序、解码和表格视图功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/common`](./shared/src/commonMain/kotlin/com/artillery/fehelper/common)：共享主题、表单组件和工具入口组件。
- [`desktopApp`](./desktopApp)：Desktop（JVM）应用。
- [`webApp`](./webApp)：Web 应用及浏览器入口。

## 运行项目

使用 IDE 中的运行配置，或执行以下命令：

### Desktop

```bash
# 热重载
./gradlew :desktopApp:hotRun --auto

# 普通运行
./gradlew :desktopApp:run
```

### Web

```bash
# Kotlin/Wasm，适用于现代浏览器
./gradlew :webApp:wasmJsBrowserDevelopmentRun

# Kotlin/JS，兼容更多旧版浏览器
./gradlew :webApp:jsBrowserDevelopmentRun
```

## 参考项目

- [FeHelper](https://fehelper.com/)
- [Kotlin Multiplatform](https://www.jetbrains.com.cn/en-us/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/)
- [Kotlin/Wasm](https://kotl.in/wasm/)
