package com.blelock.app

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class NativeBridge(private val context: Context, private val webView: WebView) {

    private val ui   = Handler(Looper.getMainLooper())
    private val http = OkHttpClient()

    // ── BLE state ─────────────────────────────────────────────────────
    private val btAdapter: BluetoothAdapter? by lazy {
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }
    private var scanner:  BluetoothLeScanner? = null
    private var scanCb:   ScanCallback?       = null
    private var scanCbId: String?             = null

    private var gatt:           BluetoothGatt?                 = null
    private var writeCh:        BluetoothGattCharacteristic?   = null
    private var connectCbId:    String? = null
    private var discoverCbId:   String? = null
    private var writeCbId:      String? = null
    private var pendingSvcUuid   = ""
    private var pendingWriteUuid = ""
    private var pendingReadUuid  = ""

    // ══════════════════════════════════════════════════════════════════
    //  HTTP POST（支持任意请求头，包括 Cookie）
    // ══════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun httpPost(url: String, headersJson: String, body: String, cbId: String) {
        val req = try {
            val builder = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            JSONObject(headersJson).keys().forEach { k ->
                builder.addHeader(k, JSONObject(headersJson).getString(k))
            }
            builder.build()
        } catch (e: Exception) {
            jsCall("window.JSBridge.onHttpResult('$cbId', 0, ${quo(e.message ?: "bad request")})")
            return
        }

        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                jsCall("window.JSBridge.onHttpResult('$cbId', 0, ${quo(e.message ?: "network error")})")
            }
            override fun onResponse(call: Call, response: Response) {
                val respBody = response.body?.string() ?: ""
                jsCall("window.JSBridge.onHttpResult('$cbId', ${response.code}, ${quo(respBody)})")
            }
        })
    }

    // ══════════════════════════════════════════════════════════════════
    //  BLE 扫描（按设备名精确匹配）
    // ══════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun bleScan(targetId: String, cbId: String) {
        val adapter = btAdapter ?: run {
            bleFail(cbId, "蓝牙不可用")
            return
        }
        val bleScanner = adapter.bluetoothLeScanner ?: run {
            bleFail(cbId, "BLE 扫描器不可用")
            return
        }
        scanner = bleScanner
        scanCbId = cbId

        scanCb = object : ScanCallback() {
            override fun onScanResult(type: Int, result: ScanResult) {
                val raw  = result.device.name ?: return
                val norm = normalize(raw)
                if (norm == targetId.uppercase()) {
                    stopScan()
                    bleOk(cbId, "${result.device.address}|$raw")
                }
            }
            override fun onScanFailed(code: Int) {
                stopScan()
                bleFail(cbId, "扫描失败，错误码 $code")
            }
        }
        scanner!!.startScan(
            null,
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
            scanCb!!
        )
        ui.postDelayed({
            if (scanCbId == cbId) { stopScan(); bleFail(cbId, "扫描超时，未发现目标设备") }
        }, 12_000)
    }

    private fun stopScan() {
        scanCb?.let { scanner?.stopScan(it) }
        scanCb = null; scanCbId = null
    }

    // ══════════════════════════════════════════════════════════════════
    //  BLE 连接
    // ══════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun bleConnect(address: String, cbId: String) {
        connectCbId = cbId
        val remote = btAdapter?.getRemoteDevice(address)
        if (remote == null) {
            bleFail(cbId, "蓝牙不可用")
            return
        }
        gatt = remote.connectGatt(context, false, gattCb, BluetoothDevice.TRANSPORT_LE)
    }

    // ══════════════════════════════════════════════════════════════════
    //  BLE 发现服务 + 订阅通知
    // ══════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun bleDiscoverSubscribe(serviceUuid: String, writeUuid: String, readUuid: String, cbId: String) {
        val g = gatt
        if (g == null) {
            bleFail(cbId, "未连接")
            return
        }
        discoverCbId     = cbId
        pendingSvcUuid   = serviceUuid
        pendingWriteUuid = writeUuid
        pendingReadUuid  = readUuid
        if (!g.discoverServices()) bleFail(cbId, "服务发现启动失败")
    }

    // ══════════════════════════════════════════════════════════════════
    //  BLE 写特征（无响应模式，20 字节分片由 JS 负责）
    // ══════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun bleWrite(hexData: String, cbId: String) {
        val ch = writeCh
        if (ch == null) {
            bleFail(cbId, "未找到写特征")
            return
        }
        val g = gatt
        if (g == null) {
            bleFail(cbId, "未连接")
            return
        }
        writeCbId = cbId
        val bytes = hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        @Suppress("DEPRECATION")
        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        @Suppress("DEPRECATION")
        g.writeCharacteristic(ch)
    }

    // ══════════════════════════════════════════════════════════════════
    //  文件下载：将文本内容保存到 Downloads 文件夹
    // ══════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun downloadFile(filename: String, content: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+：通过 MediaStore 写入公共 Downloads，无需权限
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("无法创建下载条目")
                resolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                ui.post { Toast.makeText(context, "已保存到下载文件夹：$filename", Toast.LENGTH_LONG).show() }
            } else {
                // Android 8-9：写入应用专属外部存储（无需权限）
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                dir.mkdirs()
                File(dir, filename).writeText(content, Charsets.UTF_8)
                ui.post { Toast.makeText(context, "已保存：${File(dir, filename).absolutePath}", Toast.LENGTH_LONG).show() }
            }
        } catch (e: Exception) {
            ui.post { Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show() }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  BLE 断开
    // ══════════════════════════════════════════════════════════════════

    @JavascriptInterface
    fun bleDisconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt    = null
        writeCh = null
    }

    // ══════════════════════════════════════════════════════════════════
    //  GATT 回调
    // ══════════════════════════════════════════════════════════════════

    private val gattCb = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            val cbId = connectCbId ?: return
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                bleOk(cbId, "")
            } else {
                connectCbId = null
                bleFail(cbId, "连接失败，status=$status")
                g.close(); gatt = null
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val cbId = discoverCbId ?: return
            discoverCbId = null
            if (status != BluetoothGatt.GATT_SUCCESS) {
                bleFail(cbId, "服务发现失败，status=$status")
                return
            }

            val svc = g.getService(UUID.fromString(pendingSvcUuid))
            if (svc == null) {
                bleFail(cbId, "服务未找到: $pendingSvcUuid")
                return
            }

            val writable = svc.getCharacteristic(UUID.fromString(pendingWriteUuid))
            if (writable == null) {
                bleFail(cbId, "写特征未找到: $pendingWriteUuid")
                return
            }
            writeCh = writable

            val readCh = svc.getCharacteristic(UUID.fromString(pendingReadUuid))
            if (readCh == null) {
                bleFail(cbId, "读特征未找到: $pendingReadUuid")
                return
            }

            g.setCharacteristicNotification(readCh, true)
            @Suppress("DEPRECATION")
            readCh.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                ?.let { desc ->
                    @Suppress("DEPRECATION")
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(desc)
                }
            bleOk(cbId, "")
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicWrite(
            g: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int
        ) {
            val cbId = writeCbId ?: return
            writeCbId = null
            if (status == BluetoothGatt.GATT_SUCCESS) bleOk(cbId, "")
            else bleFail(cbId, "写入失败，status=$status")
        }

        // Android < 13
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, ch: BluetoothGattCharacteristic) {
            fireNotify(ch.value)
        }

        // Android 13+
        override fun onCharacteristicChanged(
            g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray
        ) {
            fireNotify(value)
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  内部工具
    // ══════════════════════════════════════════════════════════════════

    private fun fireNotify(value: ByteArray) {
        val hex = value.joinToString("") { "%02x".format(it) }
        jsCall("window.JSBridge.onBleNotify('$hex')")
    }

    private fun normalize(name: String) =
        name.uppercase().replace("BLDLOCK", "BLELOCK").replace("CLELOCK", "BLELOCK")

    private fun bleOk(cbId: String, data: String) {
        jsCall("window.JSBridge.onBleResult('$cbId', 'ok', ${quo(data)})")
    }

    private fun bleFail(cbId: String, msg: String) {
        jsCall("window.JSBridge.onBleResult('$cbId', 'error', ${quo(msg)})")
    }

    private fun quo(s: String) = JSONObject.quote(s)

    private fun jsCall(script: String) {
        ui.post { webView.evaluateJavascript(script, null) }
    }

    fun cleanup() {
        stopScan()
        bleDisconnect()
        http.dispatcher.executorService.shutdown()
    }
}
