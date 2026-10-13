# FeHelperKMP

面向 Web 和 Desktop 的开发者工具集合项目，工具方向参考 [FeHelper](https://fehelper.com/)。本项目独立开发，与 FeHelper 官方项目不存在从属或官方关联关系。

## 当前状态

- 已实现“AMZ 水票计算”，支持根据运费、税金、贴标费用和优惠规则计算支付金额。
- 已实现“JSON 格式化”，支持格式化、排序、解码和表格视图。
- 已实现“时间（戳）转换”，支持本地时间、Unix 秒/毫秒时间戳和世界时钟。
- 已实现“2FA 动态口令”，支持 Base32 密钥和 `otpauth` URI 生成 TOTP。
- 已实现“WebSocket 工具”，支持连接测试、消息收发和通信结果查看。
- 已实现“随机密码生成器”，支持字符类型、长度、数量和批量复制。
- 已实现“贷款利率计算器”，支持利率计算和还款明细查看。
- 已实现“颜色转换工具”，支持 HEX、RGB、HSL、HSV 和透明度转换。
- 已实现“UUID / ID 生成器”，支持 UUID v4、Snowflake ID 生成与解析、NanoID 生成。
- 已实现“存储单位转换”，支持 B、KB、MB、GB、TB 之间的 1024 进位换算。
- 首页作为统一工具入口，支持关键词搜索以及列表、网格两种展示方式。
- 各工具页面复用统一主题、表单组件和返回导航，并针对 Web/Desktop 窗口尺寸提供响应式布局。

## 开发计划

后续会继续参考 FeHelper 的工具方向，按实际需求逐项扩展开发和日常工作工具。具体工具顺序和发布时间以代码实现为准。

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
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/time`](./shared/src/commonMain/kotlin/com/artillery/fehelper/time)：时间与 Unix 时间戳转换功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/totp`](./shared/src/commonMain/kotlin/com/artillery/fehelper/totp)：TOTP 动态口令功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/websocket`](./shared/src/commonMain/kotlin/com/artillery/fehelper/websocket)：WebSocket 连接和消息测试功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/password`](./shared/src/commonMain/kotlin/com/artillery/fehelper/password)：随机密码生成及平台安全随机数实现。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/loan`](./shared/src/commonMain/kotlin/com/artillery/fehelper/loan)：贷款利率和还款计算功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/color`](./shared/src/commonMain/kotlin/com/artillery/fehelper/color)：颜色格式转换功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/id`](./shared/src/commonMain/kotlin/com/artillery/fehelper/id)：UUID v4、Snowflake ID 和 NanoID 功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/storage`](./shared/src/commonMain/kotlin/com/artillery/fehelper/storage)：存储单位转换功能。
- [`shared/src/commonMain/kotlin/com/artillery/fehelper/common`](./shared/src/commonMain/kotlin/com/artillery/fehelper/common)：共享主题、表单组件和工具入口组件。
- [`core-state`](./core-state)：跨页面复用的 Compose 状态和 `StateViewModel` 基础实现。
- [`desktopApp`](./desktopApp)：Desktop（JVM）应用。
- [`webApp`](./webApp)：Web 应用及浏览器入口。

## 验证状态

- JVM 编译：`./gradlew :shared:compileKotlinJvm`
- Kotlin/JS 编译：`./gradlew :shared:compileKotlinJs`
- 当前已完成源码级编译验证；未在 README 中宣称已完成真实设备、浏览器截图或后端联调验证。

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
