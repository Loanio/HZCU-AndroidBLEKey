# AndroidBLELock

<p align="right">Language: [中文](#zh) | [English](#en)</p>

<a name="zh"></a>
## 简体中文

> 免责声明：本项目及相关内容仅供技术交流与学习参考，请遵守相关法律法规与服务条款。请勿用于违法违规用途，亦请勿利用本软件侵犯他人权益，因不当使用所造成的后果由使用者自行承担。

这个安卓 APP 可以避开烦人的微信小程序流程，避免每次开宿舍门都等待微信环境加载，也避免了每次打开小程序时无意义的握手，实现更优雅的开门体验。它可以把钥匙保存在本地，并且无需联网也可完成开锁，配置信息一次拉取后永久本地缓存。

### 功能

- **一键开锁** — 点击按钮后自动扫描、连接门锁并完成认证，全程约 3–5 秒。
- **离线工作** — 开锁流程完全通过本地 BLE 通信完成，不依赖网络。
- **自动关锁** — 开锁后倒计时（可配置），自动发送关锁指令。
- **配置刷新** — 联网时可从校园后台一键拉取最新钥匙信息（蓝牙 ID、Key ID、有效期）。

### 技术架构

UI 层为纯 HTML + JS，运行在 WebView 内；原生层通过 AndroidBridge 接口暴露蓝牙和网络能力。

### 环境要求

| 项目 | 要求 |
|------|------|
| Android | 8.0（API 26）及以上 |
| 蓝牙 | 支持 BLE（Bluetooth 4.0+） |
| 权限 | `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`（Android 12+），`ACCESS_FINE_LOCATION`（Android 11 及以下） |

### 使用方法

1. **安装并首次使用**：将 APK 安装到手机后，首次启动会弹出蓝牙权限申请，允许即可。然后填写账号（账号为自己注册原版小程序时候用的手机号），点击「刷新配置」，APP 自动登录并填入蓝牙 ID、Key ID、有效期等字段。
2. **日常开锁**：直接点击中央锁形按钮，APP 自动扫描并连接门锁。
3. **找不到设备**：点击「找不到设备？扫描全部」，从列表手动选择。
4. **关锁延迟**：在「关锁延迟(秒)」字段修改自动关锁的等待时间（默认 10 秒）。

配置信息会自动保存到本地（localStorage），下次启动无需重新填写。

### 依赖

- androidx.core:core-ktx:1.13.0
- androidx.appcompat:appcompat:1.7.0
- com.squareup.okhttp3:okhttp:4.12.0

---

<a name="en"></a>
## English

> Disclaimer: This project and its related content are for technical communication and learning purposes only. Please comply with applicable laws, regulations, and service terms. Do not use it for any unlawful or improper purpose, and do not use this software to infringe on the rights of others. Users are solely responsible for any consequences arising from improper use.

This Android app avoids the cumbersome WeChat mini-program flow, so you do not have to wait for the WeChat environment to load every time you open a door, nor perform meaningless handshakes each time the mini-program starts. It keeps the key locally and can unlock without an internet connection. Once configuration is fetched, it is cached locally permanently.

### Features

- **One-tap unlock** — Tap the button to automatically scan, connect to the lock, and complete authentication, typically within 3–5 seconds.
- **Offline operation** — The unlocking flow uses only local BLE communication and does not depend on network access.
- **Auto close** — After unlocking, a configurable countdown starts and the app automatically sends the close command.
- **Configuration refresh** — When online, the app can fetch the latest key information from the campus backend in one click, including Bluetooth ID, Key ID, and validity period.

### Architecture

The UI layer is pure HTML + JavaScript and runs inside a WebView. The native layer exposes Bluetooth and network capabilities through the `AndroidBridge` interface.

### Requirements

| Item | Requirement |
|------|------|
| Android | 8.0 (API 26) or later |
| Bluetooth | BLE supported (Bluetooth 4.0+) |
| Permissions | `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` (Android 12+), `ACCESS_FINE_LOCATION` (Android 11 and earlier) |

### Usage

1. **Install and first use**: After installing the APK on your phone, the first launch will request Bluetooth permissions; allow them. Then enter your account number (the phone number used to register the original mini-program), click "Refresh Config", and the app will log in automatically and fill in Bluetooth ID, Key ID, validity period, and other fields.
2. **Daily unlock**: Tap the central lock-shaped button, and the app will automatically scan and connect to the lock.
3. **Cannot find the device**: Tap "Can't find device? Scan all" and select the lock manually from the list.
4. **Close delay**: Change the auto-close wait time in the "Close delay (seconds)" field (default is 10 seconds).

Configuration is automatically saved locally in `localStorage`, so it does not need to be entered again the next time the app starts.

### Dependencies

- `androidx.core:core-ktx:1.13.0`
- `androidx.appcompat:appcompat:1.7.0`
- `com.squareup.okhttp3:okhttp:4.12.0`
