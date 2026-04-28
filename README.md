# AndroidBLELock

用蓝牙（BLE）离线开宿舍门锁的 Android 钥匙 APP。无需联网即可完成开锁，配置信息一次拉取后本地缓存。

## 功能

- **一键开锁** — 点击按钮自动扫描、连接门锁、完成认证，全程约 3–5 秒
- **离线工作** — 开锁流程完全走本地 BLE 通信，不依赖网络
- **自动关锁** — 开锁后倒计时（可配置），自动发送关锁指令
- **配置刷新** — 联网时可从校园后台一键拉取最新钥匙信息（蓝牙 ID、Key ID、有效期）
- **双芯片兼容** — 自动识别 TI / ZG 主板，尝试对应 BLE 服务 UUID

## 技术架构

```
WebView (HTML/CSS/JS)
    └── NativeBridge (Kotlin @JavascriptInterface)
            ├── BLE 扫描 / 连接 / GATT 读写
            └── OkHttp HTTP 请求（配置刷新）
```

UI 层为纯 HTML + JS，运行在 WebView 内；原生层通过 `AndroidBridge` 接口暴露蓝牙和网络能力。

## 环境要求

| 项目 | 要求 |
|------|------|
| Android | 8.0（API 26）及以上 |
| 蓝牙 | 支持 BLE（Bluetooth 4.0+）|
| 权限 | `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`（Android 12+），`ACCESS_FINE_LOCATION`（Android 11 及以下）|

## 构建与安装

```bash
# 克隆仓库
git clone <repo-url>
cd AndroidBLELock

# 用 Android Studio 打开，或命令行构建 debug APK
./gradlew assembleDebug

# APK 输出路径
app/build/outputs/apk/debug/app-debug.apk
```

将 APK 安装到手机后，首次启动会弹出蓝牙权限申请，允许即可。

## 使用方法

1. **首次使用**：填写账号，点击「刷新配置」，APP 自动登录并填入蓝牙 ID、Key ID、有效期等字段
2. **日常开锁**：直接点击中央锁形按钮，APP 自动扫描并连接门锁
3. **找不到设备**：点击「找不到设备？扫描全部」，从列表手动选择
4. **关锁延迟**：在「关锁延迟(秒)」字段修改自动关锁的等待时间（默认 10 秒）

配置信息会自动保存到本地（`localStorage`），下次启动无需重新填写。

## 开锁协议简述

```
手机 → 门锁  GetToken (0x0001)
门锁 → 手机  Token (0xF001)  — 16 字节随机数
手机 → 门锁  OpenLock (0x000D)  — MD5(keyId + token) + 时间戳 + 用户信息
门锁 → 手机  ACK (0xF00D)
手机 → 门锁  AutoClose (0x000D, action=2)  — 倒计时结束后
```

数据帧格式：`AA 11 77 DD` 魔数 + 数据长度 + 命令 ID + 序号 + 载荷 + CRC-16。

## 依赖

- `androidx.core:core-ktx:1.13.0`
- `androidx.appcompat:appcompat:1.7.0`
- `com.squareup.okhttp3:okhttp:4.12.0`
