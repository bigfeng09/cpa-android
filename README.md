# CPA Android App

这是 `CPA Usage Android` 的公开仓库首页说明。

## 使用说明

`v0.4.0` 首次打开采用两步引导，完成后进入 `总览 / 账号 / 用量 / 更多` 四个底部入口。已保存的连接、登录状态和加密凭据会在正常重启及同签名 APK 覆盖升级后恢复。

请特别注意，App 内有两类不同的配置：

### 1. 登录页面填写的是 `CPA Usage Keeper` 相关信息

- `服务地址`：填写你的 `CPA Usage Keeper` 地址。
  - 例如：`http://你的主机:8318`
  - 或：`https://你的域名`
- `密码`：填写 `CPA Usage Keeper` 统计接口使用的密码。点击下一步时 App 会先验证 `/api/v1/status`，验证成功后才保存并继续。

这一步的作用是让 App 能登录并读取：

- `/api/v1/status`
- `/api/v1/usage`
- `/api/v1/models/used`
- `/api/v1/pricing`

App 中保存模型价格时，会通过 `/api/v1/pricing/{model}` 写回 CPA Usage Keeper。网页端或 Android 端修改后，在另一端刷新即可读取同一份服务端价格。

服务地址、登录状态、CPA Usage Keeper 密码和 Management Key 会保存在应用私有存储中，其中密码和 Key 使用 Android Keystore 加密。正常关闭或强制停止后再次打开会自动恢复；主动退出、清除应用数据或卸载应用会删除这些信息。安装新版 APK 时必须使用与当前版本相同的签名，Android 才能直接升级并保留数据。

### 2. 第二步配置 `CLI Proxy API` 链接和管理密码

第一步验证成功后：

1. 在第二步引导中填写：
   - `CLI Proxy API` 链接
   - `管理 Key / Management Key`
2. 保存后进入 `总览`，再打开底部 `账号` 页面读取和刷新额度。
3. 也可以暂时跳过，稍后到 `更多 > 设置` 中补充或修改。

`CLI Proxy API` 地址支持填写以下任一种：

- 域名根地址，例如：`https://your-domain.example`
- 账号页面地址，例如：`https://your-domain.example/management.html#/quota`
- 管理 API 地址，例如：`https://your-domain.example/v0/management`

## 下载 APK

- Release 页面：<https://github.com/bigfeng09/cpa-android/releases>
- 最新发布版：<https://github.com/bigfeng09/cpa-android/releases/latest>

## 数据加载

- 启动和刷新时先加载会话、服务状态、模型和价格，再后台读取体积较大的 `/api/v1/usage`。
- 上次成功总览会轻量缓存在本机；离线或慢网络下会明确标记为“缓存”，不会冒充当前在线状态。
- Android 和网页端共用服务端价格表，任一端保存后，另一端刷新即可同步。

## 项目目录

- Android 项目：`cpa-android`
- 设计与开发说明：`output/ui`

更详细的 Android 项目说明见：[cpa-android/README.md](./cpa-android/README.md)
