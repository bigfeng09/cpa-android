# CPA Usage Android

这是按 `output/ui/cpa-android-developer-spec.md` 启动的 Android MVP。当前版本是原生 Android debug APK，用于在手机上连接 CPA Usage Keeper，并查看带时间范围的数据统计、模型、凭证、日志和配额管理页。

## 已实现

- 登录页：填写 `CPA Usage Keeper` 的服务地址和密码，地址支持 `http://` 与 `https://`，可测试 `/api/v1/status`。
- 登录页中的密码会作为 `CPA Usage Keeper` 统计接口的 `Authorization: Bearer <密码>` 发送；本地保存时会优先使用 Android Keystore 加密。
- `CPA Usage Keeper` 密码和 `CLI Proxy API` Management Key 完全分开保存，登录不会再覆盖 Management Key；旧版本自动复制的同值 Key 会在升级后清理一次。
- 支持 `http://` 局域网地址，也支持 `https://` 反代链接。
- 全局统计范围改为简洁下拉菜单：`4h`、`8h`、`24h`、`7天`、`30天`、`全部`、`自定义`。
- 自定义范围使用原生日期选择器选择开始/结束日期，按开始日期 00:00 到结束日期次日 00:00 过滤。
- 首页：按当前范围展示服务在线状态、会话状态、请求数、成功率、Token、平均耗时和请求健康时间线。
- 统计页：按当前范围展示成本趋势、成本估算、Token 拆分和价格配置；成本可在“全部模型”和任一单个模型之间筛选。
- 价格配置：自动读取网页端 `GET /api/v1/pricing`；手机端通过模型下拉框共用一套输入/输出/缓存价格输入区，保存后立即重算当前范围成本。
- 模型页：按当前范围展示模型请求量、成功率、Token、输入/输出、平均耗时、最近使用时间。
- 凭证页：按当前范围展示脱敏 Key、请求数、成功率、Token、失败数、健康状态估算。
- 日志页：按当前范围展示最近事件、仅错误筛选。
- `CLI Proxy API` 配置不在登录页填写，而是在进入 App 后到 `设置` 页面填写。
- 账号页：已改为纯原生 Android 页面，不再使用 WebView 嵌套。默认账号页地址使用占位示例 `https://your-domain.example/management.html#/quota`，可以在设置页自行修改。
- 在 `设置` 页面填写 `CLI Proxy API` 链接和 `管理 Key` 后，进入 `账号` 页面点击刷新，App 会调用对应的 `/v0/management/auth-files` 和 `/v0/management/api-call` 读取 OAuth 配额；App 顶部刷新按钮也会触发原生刷新。
- 账号页只展示已启用的 Codex 账号；接口返回停用、禁用、暂停或删除状态的账号会被隐藏，并且不会执行额度刷新。
- 账号页现在按 Codex 账号分卡片展示：账号/文件名、套餐、续期时间、主动重置次数、5 小时限额、周/月限额、Code Review 限额、附加限额、重置次数有效期、错误信息。
- 每个 Codex 账号卡片都有独立的 `刷新额度` 和 `重置额度` 按钮；顶部有 `刷新全部凭证`。重置额度会调用 Codex WHAM reset consume 接口，随后自动重新刷新该账号。
- 设置页：修改统计服务地址、账号页地址和 Management Key。Management Key 使用密码输入框隐藏，退出会清除两套本地凭据。
- 网络：允许局域网 HTTP 明文访问，同时可访问 HTTPS 反代管理页。
- 图标：已切换为原生 Android adaptive icon，使用仓库内矢量资源生成，不依赖外部素材。

## 配置流程

1. 在登录页填写 `CPA Usage Keeper` 的服务地址和密码，进入 App。
2. 进入底部 `设置` 页面。
3. 在 `CLI Proxy API设置` 中填写链接和 `管理 Key / Management Key`。
4. 保存后进入底部 `账号` 页面。
5. 点击刷新，即可加载和刷新账号额度数据。

`CLI Proxy API` 地址支持以下任一种：

- `https://your-domain.example`
- `https://your-domain.example/management.html#/quota`
- `https://your-domain.example/v0/management`

## 已对接接口

- `GET /api/v1/auth/session`
- `GET /api/v1/status`
- `GET /api/v1/usage`
- `GET /api/v1/models/used`
- `GET /api/v1/pricing`

配额原生刷新使用管理中心接口：

- `GET /v0/management/auth-files`
- `POST /v0/management/api-call`

这些管理接口需要 Header：`Authorization: Bearer <管理 Key>`。

Codex 账号额度通过管理中心 `/api-call` 代理请求：

- `GET https://chatgpt.com/backend-api/wham/usage`
- `GET https://chatgpt.com/backend-api/wham/rate-limit-reset-credits`
- `POST https://chatgpt.com/backend-api/wham/rate-limit-reset-credits/consume`

App 会按网页端字段解析 `rate_limit.primary_window`、`rate_limit.secondary_window`、`code_review_rate_limit`、`additional_rate_limits`、`rate_limit_reset_credits`；如果后端返回字段变化，会在账号卡片展示错误或安全摘要，避免空白。

续期时间会优先按网页端字段解析 `chatgpt_subscription_active_until` / `chatgptSubscriptionActiveUntil` / `subscription_active_until` / `subscriptionActiveUntil`，并兼容 `active_until`、`billing_period_end`、`renewal_at`、`expires_at`、`expires`、`expiry` 等字段。显示格式使用网页端同类样式，例如 `2026/8/8 09:08:43`。

管理中心 `/api-call` 响应已兼容网页端的 `status_code` 字段，同时也兼容 `statusCode` / `status`。Codex 用量的 `used_percent` 如果返回 `0..1` 小数，App 会自动换算成 `0..100%` 进度。

当前 `/api/v1/usage` 返回体较大，App 首次刷新会等待该接口完成。后续建议后端增加分页/聚合接口，减少移动端解析压力。

成本估算优先使用手机端保存的价格表。刷新时会从 `/api/v1/pricing` 拉取网页端已有价格；手机修改价格后会本地覆盖，并用于后续成本计算。

当前范围统计是客户端根据 `/api/v1/usage` 里的 `details.timestamp` 过滤计算的，所以 `4h`、`8h`、`24h`、`7天`、`30天`、`全部` 和自定义日期范围会得到不同统计数据。后续如果后端支持 `/api/v1/usage?range=` 或 `/api/v1/usage?start=&end=`，可以把范围计算下沉到后端。

## 构建

```powershell
cd cpa-android
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Debug APK：

```text
cpa-android/app/build/outputs/apk/debug/app-debug.apk
```

## 验证记录

已在 Windows 本机执行：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon
```

结果：`BUILD SUCCESSFUL`；单元测试 `10/10` 通过，Android Lint `0` 项。

最近一次验证：2026-07-10，APK 路径仍为 `app/build/outputs/apk/debug/app-debug.apk`。

## 后续建议

- 后端补 `GET /api/v1/logs`，支持分页、level、model、credential、时间范围筛选。
- 后端补 `GET /api/v1/credentials/status`，返回 Key 健康状态、最近错误、余额/额度。
- 后端补 `/api/v1/usage?range=&groupBy=`，避免移动端拉取全量 details。
- 后端补成本/价格接口后，替换当前成本占位。
- Android 后续可切换到 Kotlin + Jetpack Compose + Retrofit 架构，当前 Java 原生版用于快速验证功能和 APK 安装体验。
