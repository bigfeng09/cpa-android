# CPA Android App 开发说明

参考设计稿：`output/ui/cpa-android-ui-design-v2-stats.png`

目标后端站点示例：`http://your-host:8318/`

已识别接口：

- `GET /api/v1/auth/session`
- `GET /api/v1/status`
- `GET /api/v1/usage`
- `GET /api/v1/models/used`

## 1. App 目标

做一个 CPA Android 管理与统计 App，把局域网内 CPA Usage Keeper 的 Web 统计能力移动端化。用户打开 App 后可以连接自己的 Usage Keeper 服务，查看请求统计、模型用量、凭证状态、余额/成本估算、事件日志，并进行基础设置。

第一版不需要重做后端，优先作为 Usage Keeper 的移动端监控面板。核心目标是：

- 快速确认服务是否在线、会话是否有效、统计数据是否正常更新。
- 在手机上查看今日/本周/本月请求量、成功率、耗时、Token、成本估算。
- 查看不同模型的请求量、Token 用量、费用占比、成功率。
- 查看凭证/Key 状态、可用性、余额或额度信息。
- 查看最近日志和异常，便于发现 Key 失效、请求失败、接口不可达等问题。

## 2. 页面结构

按设计稿 v2 做 6 个主要页面，底部导航建议保留 4 个一级入口，部分页面作为二级页面进入。

推荐信息架构：

1. 连接/登录页
   - 首次打开显示。
   - 未连接、会话失效、服务不可达时回到此页或展示重连状态。

2. 首页 Dashboard
   - 底部导航第 1 项：`首页`。
   - 展示服务状态和关键指标摘要。

3. 统计总览
   - 底部导航第 2 项：`统计`。
   - 展示请求趋势、成功率、Token、耗时、成本。

4. 模型/凭证
   - 底部导航第 3 项：`模型` 或 `资源`。
   - 上方用 Tab 切换 `模型统计` / `凭证状态`。

5. 余额/成本
   - 可以作为首页或统计页的二级页面，也可以并入 `统计` Tab。
   - 如果后端已有价格配置、成本估算、余额信息，第一版可以独立做；否则先显示成本估算。

6. 日志/设置
   - 底部导航第 4 项：`日志` 或 `设置`。
   - 上方用 Tab 切换 `事件日志` / `连接设置` / `数据维护`。

建议第一版底部导航：`首页`、`统计`、`模型`、`日志`。`凭证` 放在模型页 Tab，`余额` 放在统计页二级入口或卡片详情。

## 3. 每个页面功能点

### 3.1 连接/登录页

用途：配置 Usage Keeper 地址，检查服务连接，建立/验证会话。

功能点：

- 输入服务地址，例如 `http://your-host:8318`。
- 一键使用默认局域网地址。
- 点击 `测试连接`：请求 `/api/v1/status`，展示在线/离线、延迟、版本或服务名。
- 点击 `登录/进入`：请求 `/api/v1/auth/session` 验证当前会话。
- 支持记住服务地址。
- 如果后端需要 Cookie、Token 或 Basic Auth，提供对应输入项；如果当前接口无鉴权，先隐藏高级登录项。
- 连接失败时给出明确错误：地址不可达、超时、接口返回异常、会话无效。

页面状态：

- 未配置：显示地址输入和默认地址按钮。
- 连接中：按钮 loading，禁止重复点击。
- 连接成功：展示服务在线并进入首页。
- 连接失败：保留输入内容，展示错误原因和重试按钮。

### 3.2 首页 Dashboard

用途：用户打开 App 后快速看到系统是否正常。

功能点：

- 顶部显示当前服务地址、在线状态、最后刷新时间。
- 核心指标卡片：
  - 今日请求数
  - 成功率
  - 平均响应耗时
  - 今日 Token 总量
  - 今日成本估算
- 服务状态卡片：
  - Usage Keeper 是否在线
  - CPA 服务/代理是否在线，如果 `/status` 能提供
  - 当前活跃模型数量
  - 可用凭证数量 / 异常凭证数量
- 最近异常：显示最近 3-5 条失败日志或异常事件。
- 快捷操作：刷新、查看统计、查看日志、导出数据入口。

交互：

- 下拉刷新：同时刷新 `/status`、`/usage`、`/models/used`。
- 指标卡片点击进入对应详情，例如点击成功率进入统计总览，点击模型数量进入模型页。
- 数据为空时显示空状态，不显示假数据。

### 3.3 统计总览页

用途：查看请求、Token、成本和耗时趋势。

功能点：

- 时间范围切换：`今日`、`7天`、`30天`、`自定义`。
- 请求统计：
  - 总请求数
  - 成功请求数
  - 失败请求数
  - 成功率
- Token 统计：
  - 输入 Token
  - 输出 Token
  - 总 Token
- 耗时统计：
  - 平均响应耗时
  - P95/P99 耗时，如果后端有
- 成本估算：
  - 总成本
  - 输入成本
  - 输出成本
  - 币种或单位
- 趋势图：
  - 请求量趋势
  - Token 趋势
  - 成本趋势
  - 失败率或成功率趋势

交互：

- 切换时间范围后重新请求 `/usage`。
- 图表支持点击/长按查看某一天或某小时的数值。
- 支持手动刷新。
- 如果后端 `/usage` 当前只返回聚合数据，第一版先做卡片和列表，趋势图等接口扩展后再补。

### 3.4 模型/凭证页

用途：查看模型使用分布和 Key/凭证健康状态。

Tab 1：模型统计

- 模型列表字段：
  - 模型名称，例如 `gpt-4.1`、`claude-*` 等
  - 请求数
  - 成功率
  - 输入 Token
  - 输出 Token
  - 成本估算
  - 平均耗时
  - 最近使用时间
- 模型详情：
  - 当前模型总请求数
  - Token 输入/输出分布
  - 失败原因 Top N，如果后端有日志聚合
  - 成本占比

Tab 2：凭证状态

- 凭证列表字段：
  - Key 名称或脱敏标识，例如 `sk-****abcd`
  - Provider，例如 OpenAI、Anthropic、Gemini、OpenRouter
  - 状态：正常、异常、限流、余额不足、禁用、未知
  - 最近检查时间
  - 最近错误
  - 今日请求数
  - 今日失败数
  - 余额/额度，如果后端有
- 操作：
  - 手动刷新状态
  - 复制脱敏标识
  - 查看该凭证相关日志

注意：移动端不要展示完整 API Key。即使后端返回完整 Key，App 也必须只显示脱敏值。

### 3.5 余额/成本页

用途：查看花费估算、价格配置和额度风险。

功能点：

- 总成本估算：今日、本月、全部。
- 按模型拆分成本。
- 按 Provider 或 Key 拆分成本。
- 显示价格配置版本或最近更新时间，如果后端提供。
- 显示余额/额度：
  - 当前余额
  - 已用额度
  - 预计可用天数
  - 低余额提醒

MVP 处理：如果后端暂时没有余额接口，只显示基于 Token 和价格配置计算出来的 `估算成本`，并在 UI 上标注为估算。

### 3.6 日志/设置页

用途：查看最近事件、失败请求，并维护连接配置。

Tab 1：事件日志

- 日志列表字段：
  - 时间
  - 等级：info、warn、error
  - 类型：请求、认证、Key、模型、导入/导出、价格配置
  - 模型
  - Key 脱敏标识
  - 状态码
  - 耗时
  - 错误信息
- 筛选：
  - 全部 / 错误 / 警告
  - 按模型筛选
  - 按 Key 筛选
  - 按时间范围筛选
- 操作：
  - 下拉刷新
  - 点击日志查看详情
  - 复制错误信息

Tab 2：连接设置

- 服务地址编辑。
- 请求超时时间。
- 自动刷新间隔：关闭、15 秒、30 秒、60 秒、5 分钟。
- 仅 Wi-Fi 自动刷新开关。
- 清除本地缓存。

Tab 3：数据维护

- 导出统计数据入口，如果后端支持。
- 导入统计数据入口，如果后端支持。
- 价格配置查看/刷新入口，如果后端支持。
- 第一版可以先做成只读入口或跳转 Web 页面。

## 4. 后端接口对接建议

### 4.1 基础配置

App 不要写死具体内网地址，只使用可编辑的示例地址。用户必须可以改服务地址。

统一封装 API Client：

- `baseUrl`: 用户输入的服务地址。
- `timeout`: 默认 10 秒。
- `headers`: 按后端要求加入 Cookie、Authorization 或其他认证头。
- `response parser`: 对后端字段做兼容解析，避免字段缺失导致崩溃。

### 4.2 接口用途

`GET /api/v1/auth/session`

- 用途：检查当前会话是否有效。
- 调用时机：登录页点击进入、App 启动恢复会话、收到 401/403 后重试验证。
- 期望返回：是否已登录、用户信息、会话过期时间、权限。
- 如果接口返回未登录：停留在连接/登录页。

`GET /api/v1/status`

- 用途：检查 Usage Keeper 和相关服务状态。
- 调用时机：首页加载、下拉刷新、后台定时刷新。
- 期望返回：服务在线状态、版本、启动时间、数据库状态、统计更新时间、Key 状态摘要。

`GET /api/v1/usage`

- 用途：获取请求、Token、成本、耗时统计。
- 调用时机：首页、统计页、余额/成本页。
- 建议参数：
  - `range=today|7d|30d|custom`
  - `from=YYYY-MM-DDTHH:mm:ssZ`
  - `to=YYYY-MM-DDTHH:mm:ssZ`
  - `groupBy=hour|day|model|credential|provider`
- 如果当前后端还不支持这些参数，App 先按现有返回渲染，后端后续补参数化查询。

`GET /api/v1/models/used`

- 用途：获取已经使用过的模型及统计摘要。
- 调用时机：模型页、首页模型摘要。
- 建议支持参数：
  - `range=today|7d|30d|custom`
  - `sort=requests|tokens|cost|lastUsedAt|latency`
  - `order=asc|desc`

### 4.3 建议后端补充接口

如果现有接口无法覆盖设计稿，建议后端补以下接口。App 第一版可以先判断 404 并隐藏对应功能。

`GET /api/v1/credentials/status`

- 返回 Key/凭证状态、最近错误、余额/额度。

`GET /api/v1/logs`

- 返回事件日志，支持分页和筛选。
- 参数建议：`level`、`model`、`credentialId`、`from`、`to`、`page`、`pageSize`。

`GET /api/v1/costs`

- 返回成本估算、按模型/Key/Provider 拆分。

`GET /api/v1/prices`

- 返回模型价格配置和更新时间。

`POST /api/v1/export`

- 导出统计数据。

`POST /api/v1/import`

- 导入统计数据。

## 5. 数据字段建议

以下是 App 侧建议统一的数据模型。后端字段不一致时，在 Repository 层做映射，不要让 UI 直接依赖原始 JSON。

### 5.1 SessionInfo

```kotlin
data class SessionInfo(
    val authenticated: Boolean,
    val username: String?,
    val role: String?,
    val expiresAt: Instant?,
    val permissions: List<String> = emptyList()
)
```

### 5.2 ServiceStatus

```kotlin
data class ServiceStatus(
    val online: Boolean,
    val serviceName: String?,
    val version: String?,
    val uptimeSeconds: Long?,
    val databaseOk: Boolean?,
    val lastUpdatedAt: Instant?,
    val activeModelCount: Int?,
    val credentialTotal: Int?,
    val credentialHealthy: Int?,
    val credentialError: Int?
)
```

### 5.3 UsageSummary

```kotlin
data class UsageSummary(
    val range: String,
    val requestTotal: Long,
    val requestSuccess: Long,
    val requestFailed: Long,
    val successRate: Double,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val estimatedCost: BigDecimal?,
    val currency: String?,
    val avgLatencyMs: Long?,
    val p95LatencyMs: Long?,
    val p99LatencyMs: Long?,
    val series: List<UsagePoint> = emptyList()
)

data class UsagePoint(
    val time: Instant,
    val requestTotal: Long,
    val requestSuccess: Long,
    val requestFailed: Long,
    val inputTokens: Long,
    val outputTokens: Long,
    val estimatedCost: BigDecimal?
)
```

### 5.4 ModelUsage

```kotlin
data class ModelUsage(
    val model: String,
    val provider: String?,
    val requestTotal: Long,
    val successRate: Double?,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val estimatedCost: BigDecimal?,
    val avgLatencyMs: Long?,
    val lastUsedAt: Instant?
)
```

### 5.5 CredentialStatus

```kotlin
data class CredentialStatus(
    val id: String,
    val name: String?,
    val maskedKey: String,
    val provider: String?,
    val status: CredentialHealth,
    val balance: BigDecimal?,
    val quotaUsed: BigDecimal?,
    val quotaLimit: BigDecimal?,
    val todayRequests: Long?,
    val todayFailures: Long?,
    val lastCheckedAt: Instant?,
    val lastError: String?
)

enum class CredentialHealth {
    HEALTHY, WARNING, ERROR, RATE_LIMITED, INSUFFICIENT_BALANCE, DISABLED, UNKNOWN
}
```

### 5.6 EventLog

```kotlin
data class EventLog(
    val id: String,
    val time: Instant,
    val level: LogLevel,
    val type: String?,
    val message: String,
    val model: String?,
    val provider: String?,
    val maskedCredential: String?,
    val statusCode: Int?,
    val latencyMs: Long?,
    val requestId: String?,
    val details: String?
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}
```

## 6. Android 技术实现建议

### 6.1 技术栈

推荐：

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Retrofit + OkHttp
- kotlinx.serialization 或 Moshi
- Room 或 DataStore
- Kotlin Coroutines + Flow
- Hilt 或 Koin 做依赖注入
- MPAndroidChart 或 Compose 图表库做趋势图

如果团队已有 XML/ViewBinding 体系，也可以用 XML，但新项目建议直接 Compose，页面状态和图表卡片更好维护。

### 6.2 分层

建议目录结构：

```text
app/
  data/
    api/
      UsageKeeperApi.kt
      ApiModels.kt
    mapper/
    repository/
      UsageRepository.kt
      StatusRepository.kt
      SettingsRepository.kt
    local/
      AppSettingsDataStore.kt
      CacheDatabase.kt
  domain/
    model/
    usecase/
  ui/
    connect/
    dashboard/
    stats/
    models/
    logs/
    settings/
    components/
  core/
    network/
    time/
    format/
```

### 6.3 状态管理

每个页面 ViewModel 输出一个 `UiState`：

- `Loading`
- `Content(data)`
- `Empty`
- `Error(message, canRetry)`

不要在 Composable 里直接调用 Retrofit。Composable 只消费 `StateFlow<UiState>`，触发 `refresh()`、`changeRange()`、`openDetail()` 等事件。

### 6.4 本地存储

DataStore 保存：

- 服务地址
- 登录方式和必要的会话信息
- 自动刷新间隔
- 仅 Wi-Fi 刷新开关
- 最近选择的时间范围

敏感信息保存：

- 如果需要保存 Token 或密码，使用 Android Keystore 加密后再存。
- 不保存完整 API Key，除非明确是用户自己输入且有加密存储需求。

Room 缓存：

- 首页摘要缓存
- 模型列表缓存
- 最近日志缓存

离线时可以展示最近一次成功数据，并明确标注 `上次更新：时间`。

### 6.5 网络与刷新

- 全局 OkHttp 日志只在 debug 包启用，release 禁用敏感日志。
- 所有请求设置超时，默认 10 秒。
- 首页首次进入并发请求：`status`、`usage`、`models/used`。
- 下拉刷新强制刷新所有当前页面需要的数据。
- 自动刷新只在 App 前台运行，后台不要持续轮询。
- 自动刷新失败不弹窗轰炸，只在顶部状态条或 Snackbar 提示。
- 收到 401/403：清会话并回到连接/登录页。

### 6.6 UI 实现重点

- 按设计稿 v2 的移动端密度实现，不要做成 Web 站缩小版。
- 关键指标卡片要能一眼扫到：请求数、成功率、Token、成本、耗时。
- 数字格式统一：
  - Token：`1.2K`、`35.6K`、`1.8M`
  - 成本：保留 2-4 位小数，例如 `$0.0321`
  - 成功率：`98.6%`
  - 耗时：`820ms` 或 `1.4s`
- 错误颜色只用于真正异常，不要全页大面积红色。
- 空数据、加载、失败、部分接口不可用都要有独立状态。

### 6.7 图表

MVP 不要把图表做复杂，先满足看趋势：

- 请求趋势：柱状图或折线图。
- Token 趋势：堆叠柱状图，输入/输出区分颜色。
- 成本趋势：折线图。
- 模型占比：横向条形图优先，比饼图更适合长模型名。

图表数据缺失时不要造假数据。没有 series 就显示统计卡片和空状态。

## 7. 开发优先级 / MVP 阶段拆分

### 阶段 1：可连接、可看核心数据

目标：App 能连接用户输入的 Usage Keeper 地址，展示首页核心统计。

任务：

- 搭建 Android 项目基础架构。
- 实现服务地址配置和连接测试。
- 对接 `/api/v1/auth/session`。
- 对接 `/api/v1/status`。
- 对接 `/api/v1/usage`。
- 首页展示：在线状态、请求数、成功率、Token、成本、耗时、最后刷新时间。
- 支持下拉刷新和错误重试。

验收：

- 手机连接同一局域网后能打开 App 并看到真实统计。
- 断网、地址错误、服务关闭时不会崩溃，能显示明确错误。

### 阶段 2：模型统计和基础趋势

目标：能看模型维度统计和时间范围切换。

任务：

- 对接 `/api/v1/models/used`。
- 模型列表展示请求量、Token、成本、成功率、最近使用时间。
- 统计页支持 `今日`、`7天`、`30天`。
- 如果 `/usage` 支持 series，做趋势图；不支持则先做聚合卡片。
- 增加本地缓存，离线展示最近数据。

验收：

- 模型列表与 Web 统计站点数据大体一致。
- 时间范围切换后数据刷新正确。

### 阶段 3：凭证状态、日志、异常定位

目标：能发现 Key 异常和请求失败原因。

任务：

- 对接凭证状态接口；如果后端没有，需要补 `GET /api/v1/credentials/status`。
- 对接日志接口；如果后端没有，需要补 `GET /api/v1/logs`。
- 凭证页展示 Key 脱敏状态、Provider、余额/额度、最近错误。
- 日志页展示最近事件，支持错误筛选和详情。
- 首页展示最近异常摘要。

验收：

- Key 失效、限流、余额不足等问题能在 App 内看到。
- 日志详情能复制错误信息，便于排查。

### 阶段 4：余额/成本、导入导出、体验完善

目标：补齐 Usage Keeper Web 页面里偏管理的能力。

任务：

- 成本估算详情页。
- 价格配置查看。
- 导出/导入入口。
- 自动刷新设置。
- 深色模式适配。
- Release 包安全检查。

验收：

- 常用统计和管理动作无需打开 Web 页面。
- Release 包不泄漏完整 Key、Token、请求日志敏感内容。

## 8. 注意事项

1. 不要在 App 里硬编码真实 API Key、密码或完整凭证。
2. App 展示 Key 时必须脱敏，只显示前后少量字符或后 4 位。
3. 局域网地址只能作为用户自行输入的配置项，不要在源码中写死真实地址。
4. 接口字段要做兼容处理：字段缺失、类型变化、返回空数组都不能导致崩溃。
5. Web 站点已有能力不等于 App 端接口都已存在。缺接口时先隐藏功能或展示“后端暂未提供”，不要用假数据。
6. 成本统一标注为估算，除非后端明确提供官方账单余额。
7. 自动刷新要控制频率，避免手机前台长时间轮询打爆局域网服务。
8. Release 包关闭网络明文日志；调试日志不要输出完整请求头和敏感字段。
9. Android 9+ 默认限制明文 HTTP。如果用户继续使用 `http://your-host:8318` 这类局域网 HTTP，需要配置 Network Security Config 允许明文 HTTP；后续建议支持 HTTPS。
10. 图表和统计数字必须来自接口返回或本地缓存，不要在生产环境保留 mock 数据。
11. 首版优先保证数据准确和异常可见，动画和复杂视觉效果可以后置。
12. 对比 Web 统计站点做验收：同一时间范围内，请求数、Token、模型列表、成本估算应基本一致。

## 9. 建议交付物

程序员第一轮交付建议包含：

- Android debug APK。
- 接口字段对照表：实际后端 JSON 字段到 App 数据模型的映射。
- 已实现页面截图：首页、统计页、模型页、日志/设置页。
- 测试说明：连接成功、地址错误、服务离线、接口空数据、接口 500、会话失效。
- 如果发现后端缺接口，列出缺口和建议返回结构。
