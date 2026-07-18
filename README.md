# CPA Android App

这是 `CPA Usage Android` 的公开仓库首页说明。

## 使用说明

请特别注意，App 内有两类不同的配置，填写位置不同：

### 1. 登录页面填写的是 `CPA Usage Keeper` 相关信息

- `服务地址`：填写你的 `CPA Usage Keeper` 地址。
  - 例如：`http://你的主机:8318`
  - 或：`https://你的域名`
- `密码`：填写 `CPA Usage Keeper` 统计接口使用的密码。

这一步的作用是让 App 能登录并读取：

- `/api/v1/status`
- `/api/v1/usage`
- `/api/v1/models/used`
- `/api/v1/pricing`

App 中保存模型价格时，会通过 `/api/v1/pricing/{model}` 写回 CPA Usage Keeper。网页端或 Android 端修改后，在另一端刷新即可读取同一份服务端价格。

服务地址、登录状态、CPA Usage Keeper 密码和 Management Key 会保存在应用私有存储中，其中密码和 Key 使用 Android Keystore 加密。正常关闭或强制停止后再次打开会自动恢复；主动退出、清除应用数据或卸载应用会删除这些信息。安装新版 APK 时必须使用与当前版本相同的签名，Android 才能直接升级并保留数据。

### 2. `CLI Proxy API` 的链接和管理密码，需要在进入 App 后到 `设置` 页面填写

进入 App 后：

1. 打开底部 `设置` 页面。
2. 在 `CLI Proxy API设置` 中填写：
   - `CLI Proxy API` 链接
   - `管理 Key / Management Key`
3. 保存后，点击底部 `账号` 页面。
4. 在 `账号` 页面点击刷新，即可读取和刷新账号额度数据。

`CLI Proxy API` 地址支持填写以下任一种：

- 域名根地址，例如：`https://your-domain.example`
- 账号页面地址，例如：`https://your-domain.example/management.html#/quota`
- 管理 API 地址，例如：`https://your-domain.example/v0/management`

## 下载 APK

- Release 页面：<https://github.com/bigfeng09/cpa-android/releases/tag/v0.3.1>
- 直接下载：<https://github.com/bigfeng09/cpa-android/releases/download/v0.3.1/cpa-android-v0.3.1-debug.apk>

## 项目目录

- Android 项目：`cpa-android`
- 设计与开发说明：`output/ui`

更详细的 Android 项目说明见：[cpa-android/README.md](./cpa-android/README.md)
