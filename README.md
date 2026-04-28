# AndroidBLELock

<p align="right">Language: <a href="#english">English</a> | <a href="README.zh-CN.md">中文</a></p>

<a name="english"></a>
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

