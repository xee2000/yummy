package com.pms_parkin_mobile.service

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pms_parkin_mobile.R
import com.pms_parkin_mobile.api.RestController
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothService : Service() {

    companion object {
        private const val TAG = "BluetoothService"
        private const val CHANNEL_ID = "ble_scan_channel"
        const val FOREGROUND_NOTIFICATION_ID = 821

        //동탄
//        const val UUID = "20151005-8864-5654-4159-013500201901"
        //광교
        const val UUID = "20151005-8864-5654-4111-710200210801"
        //반석
//        const val UUID = "20151005-8864-5654-3020-013900202001"
        private const val BEACON_MANUFACTURER_ID = 0x4C
        private const val BEACON_SUBTYPE = 0x02
        private const val BEACON_SUBTYPE_LENGTH = 0x15

        private const val SCAN_THROTTLE_WINDOW_MS = 30_000L
        private const val SCAN_THROTTLE_MAX_COUNT = 4
        private const val SCAN_HEALTH_CHECK_INTERVAL_MS = 30_000L
        private const val SCAN_DEAD_TIMEOUT_MS = 90_000L

        private const val RSSI_EMA_ALPHA = 0.3
        private const val RSSI_EMA_TIMEOUT_MS = 5_000L

        @Volatile var runningInstance: BluetoothService? = null
            private set

        fun getInstance(): BluetoothService? = runningInstance
        fun isBeaconScanning(): Boolean = runningInstance?.isStartScanning == true

        private fun testLog(msg: String) {
            val time = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.KOREA).format(java.util.Date())
            Log.d("BLE_SVC", "[$time] $msg")
        }
    }

    lateinit var beaconProcessor: BeaconProcessor
        private set

    val parkingState: ParkingStateManager get() = beaconProcessor.state
    val beaconTimers: BeaconTimerManager get() = beaconProcessor.timers
    val beaconProcessorForTimer: BeaconProcessor get() = beaconProcessor

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var leScanner: BluetoothLeScanner? = null
    private var scanCallback: BLEScanCallback? = null
    private var scanPendingIntent: PendingIntent? = null
    private var bluetoothReceiver: BLEBroadcastReceiver? = null
    private var screenReceiver: ScreenStateReceiver? = null

    @Volatile private var isScreenOff = false
    @Volatile var isStartScanning = false
        private set

    @Volatile var lastScanResultAt = 0L
    private val scanStartTimestamps = ArrayDeque<Long>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val rssiEmaMap = ConcurrentHashMap<String, Double>()
    private val rssiLastSeenMap = ConcurrentHashMap<String, Long>()

    // =========================================================================
    // 외부(BleScanReceiver) 호출 인터페이스
    // =========================================================================

    fun onPendingIntentResult(result: ScanResult) {
        lastScanResultAt = System.currentTimeMillis()
        parseScanResult(result)
    }

    fun onPendingIntentError(errorCode: Int) {
        testLog("❌ [PI-Error] $errorCode → 스캔 재시작")
        resetAndRestartScanning()
    }

    // =========================================================================
    // 라이프사이클
    // =========================================================================

    private val scanHealthCheckRunnable = object : Runnable {
        override fun run() {
            checkScanHealth()
            mainHandler.postDelayed(this, SCAN_HEALTH_CHECK_INTERVAL_MS)
        }
    }

    private fun checkScanHealth() {
        val now = System.currentTimeMillis()
        val diff = if (lastScanResultAt > 0) now - lastScanResultAt else -1L
        if (!bluetoothAdapter.isEnabled) return

        if (isStartScanning && diff >= SCAN_DEAD_TIMEOUT_MS) {
            testLog("💀 [Health] Silent Dead (90초 무반응) → 복구")
            resetAndRestartScanning()
        } else if (!isStartScanning) {
            startBluetoothScanning()
        }
    }

    override fun onCreate() {
        super.onCreate()
        runningInstance = this
        bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        beaconProcessor = BeaconProcessor(applicationContext)

        setupReceivers()
        startForegroundCompat()
        mainHandler.post(scanHealthCheckRunnable)
    }

    private fun setupReceivers() {
        bluetoothReceiver = BLEBroadcastReceiver()
        screenReceiver = ScreenStateReceiver()

        val btFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, btFilter, RECEIVER_EXPORTED)
            registerReceiver(screenReceiver, screenFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, btFilter)
            registerReceiver(screenReceiver, screenFilter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 앱 스와이프 후 재시작될 때 포그라운드 알림을 다시 올림
        startForegroundCompat()
        if (bluetoothAdapter.isEnabled && !isStartScanning) {
            startBluetoothScanning()
        }
        scheduleWatchdog()
        return START_STICKY
    }

    // =========================================================================
    // 스캔 제어
    // =========================================================================

    private fun startBluetoothScanning() {
        if (isStartScanning) return
        val now = System.currentTimeMillis()

        scanStartTimestamps.removeAll { now - it > SCAN_THROTTLE_WINDOW_MS }
        if (scanStartTimestamps.size >= SCAN_THROTTLE_MAX_COUNT) return

        leScanner = bluetoothAdapter.bluetoothLeScanner ?: return

        val settings = ScanSettings.Builder()
            .setScanMode(if (isScreenOff) ScanSettings.SCAN_MODE_BALANCED else ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        try {
            if (isScreenOff && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scanPendingIntent = buildScanPendingIntent()
                // ✅ 핵심 수정: 빈 필터 → buildScanFilters() 적용
                // 필터 없는 스캔은 화면 꺼진 상태에서 OS가 즉시 차단함 (API 26+)
                leScanner?.startScan(buildScanFilters(), settings, scanPendingIntent!!)
                testLog("▶️ [Start] PendingIntent 모드 (Screen Off, 필터 적용)")
            } else {
                scanCallback = BLEScanCallback()
                // ✅ Callback 모드도 동일하게 필터 적용
                leScanner?.startScan(buildScanFilters(), settings, scanCallback!!)
                testLog("▶️ [Start] ScanCallback 모드 (필터 적용)")
            }
            isStartScanning = true
            scanStartTimestamps.addLast(now)
        } catch (e: Exception) {
            testLog("❌ [Start] 실패: ${e.message}")
        }
    }

    /**
     * ✅ Apple iBeacon 제조사 데이터 필터
     * Android 8.1(API 27)+ 에서 화면 꺼진 상태의 필터 없는 스캔은 OS가 강제 중단함
     * 제조사 ID(0x4C) + iBeacon subtype(0x02, 0x15) 로 필터링
     */
    private fun buildScanFilters(): List<ScanFilter> {
        return listOf(
            ScanFilter.Builder()
                .setManufacturerData(
                    BEACON_MANUFACTURER_ID,
                    byteArrayOf(BEACON_SUBTYPE.toByte(), BEACON_SUBTYPE_LENGTH.toByte()),
                    byteArrayOf(0xFF.toByte(), 0xFF.toByte())
                )
                .build()
        )
    }

    private fun stopBluetoothScanning() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                scanPendingIntent?.let { leScanner?.stopScan(it) }
            }
            scanCallback?.let { leScanner?.stopScan(it) }
        } catch (_: Exception) {}
        scanPendingIntent = null
        scanCallback = null
        isStartScanning = false
    }

    fun resetAndRestartScanning() {
        stopBluetoothScanning()
        // 화면 꺼진 상태에서 postDelayed는 실행 보장 안 됨 → 즉시 재시작
        if (isScreenOff) {
            startBluetoothScanning()
        } else {
            mainHandler.postDelayed({ startBluetoothScanning() }, 500)
        }
    }

    private fun handleScreenState(off: Boolean) {
        if (isScreenOff == off) return
        isScreenOff = off
        testLog("📱 [Screen] ${if (off) "OFF" else "ON"}")

        resetAndRestartScanning()
    }

    // =========================================================================
    // 유틸리티
    // =========================================================================

    private fun buildScanPendingIntent(): PendingIntent {
        val intent = Intent(this, com.pms_parkin_mobile.receiver.BleScanReceiver::class.java).apply {
            action = "com.pms_parkin_mobile.BLE_SCAN_RESULT"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(this, 7001, intent, flags)
    }

    private fun scheduleWatchdog() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, BluetoothService::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 100, intent, flags)
        } else {
            PendingIntent.getService(this, 100, intent, flags)
        }

        val triggerAt = SystemClock.elapsedRealtime() + 45_000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        } else {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        }
    }

    private fun startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, "BLE Service", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(chan)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("스마트파킹")
            .setContentText("백그라운드 스캔 실행 중")
            .setSmallIcon(R.drawable.logo)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    // =========================================================================
    // 데이터 처리
    // =========================================================================

    private fun parseScanResult(result: ScanResult) {
        lastScanResultAt = System.currentTimeMillis()
        val scanRecord = result.scanRecord ?: return
        val rssi = result.rssi
        val allManufacturerData = scanRecord.manufacturerSpecificData
//        Log.d("BLE_DEBUG", "제조사 데이터 개수: ${allManufacturerData?.size()}, RSSI: $rssi")
        for (i in 0 until (allManufacturerData?.size() ?: 0)) {
            val key = allManufacturerData!!.keyAt(i)
            val value = allManufacturerData.valueAt(i)
//            Log.d("BLE_DEBUG", "  키: 0x${key.toString(16).uppercase()}, 값: ${value.joinToString("") { "%02X".format(it) }}")
        }
        val bytes = scanRecord.getManufacturerSpecificData(BEACON_MANUFACTURER_ID) ?: return

        if (bytes.size < 23 || (bytes[0].toInt() and 0xFF) != BEACON_SUBTYPE) {
//            Log.w(TAG, "⚠️ [Unknown Beacon] iBeacon 형식이 아님 (Size: ${bytes.size})")
            return
        }

        val uuidRaw = bytesToHex(bytes, 2, 16)
        val normalizedTargetUuid = UUID.replace("-", "").lowercase()
        if (!uuidRaw.contains(normalizedTargetUuid)) return

        val buf = java.nio.ByteBuffer.wrap(bytes)
        val major = buf.getShort(18).toInt() and 0xFFFF
        val minor = buf.getShort(20).toInt() and 0xFFFF

        Log.i(TAG, "✅ [Match] Major: $major, Minor: $minor (${String.format("%04X", minor)}), RSSI: $rssi")
        processBeacon(major, minor, rssi.toDouble())
    }

    private fun processBeacon(major: Int, minor: Int, rssi: Double) {
        val emaKey = if (major == 1 || major >= 4) "$major-$minor" else "$major"
        val smoothedRssi = smoothRssi(emaKey, rssi)

        if (smoothedRssi < -90) {
            Log.d(TAG, "📉 [LowSignal] Major: $major, Minor: $minor, RSSI: ${"%.1f".format(smoothedRssi)}")
            if (major == 4) beaconProcessor.keepParkingServiceAlive()
            return
        }

        when (major) {
            1 -> {
                if(!App.instance.isPassOpenLobbyFlag) return;
                Log.i(TAG, "🏢 [Lobby] 로비 감지 (Minor: $minor, SmoothRSSI: ${"%.1f".format(smoothedRssi)})")
                RestController.instance.Message("로비비컨 감지 : " + minor)
                BeaconFunction.getInstance().OnlyOpenLobby(minor, smoothedRssi)
            }
            4 -> {
                Log.i(TAG, "🚗 [Stay] 주차면 감지 (Minor: $minor, SmoothRSSI: ${"%.1f".format(smoothedRssi)})")
//                beaconProcessor.processStayParking(minor, smoothedRssi)
            }
            5 -> {
                Log.i(TAG, "🔄 [Change] 주차면 변화 감지 (Major: 5, Minor: $minor)")
//                beaconProcessor.processChangeParking(major, minor, smoothedRssi)
            }
            else -> Log.d(TAG, "❓ [Other] 정의되지 않은 Major: $major")
        }
    }

    private fun smoothRssi(key: String, rawRssi: Double): Double {
        val now = System.currentTimeMillis()
        val prev = rssiEmaMap[key] ?: rawRssi
        val lastSeen = rssiLastSeenMap[key] ?: 0L

        val smoothed = if (now - lastSeen > RSSI_EMA_TIMEOUT_MS) rawRssi
        else RSSI_EMA_ALPHA * rawRssi + (1.0 - RSSI_EMA_ALPHA) * prev

        rssiEmaMap[key] = smoothed
        rssiLastSeenMap[key] = now
        return smoothed
    }

    private fun bytesToHex(bytes: ByteArray, offset: Int, length: Int): String {
        return bytes.sliceArray(offset until offset + length).joinToString("") { "%02x".format(it) }
    }

    // =========================================================================
    // 내부 클래스
    // =========================================================================

    private inner class BLEScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = parseScanResult(result)
        override fun onBatchScanResults(results: List<ScanResult>) = results.forEach { parseScanResult(it) }
    }

    private inner class ScreenStateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> handleScreenState(true)
                Intent.ACTION_SCREEN_ON -> handleScreenState(false)
            }
        }
    }

    private inner class BLEBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                if (state == BluetoothAdapter.STATE_ON) resetAndRestartScanning()
                else if (state == BluetoothAdapter.STATE_OFF) stopBluetoothScanning()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        testLog("🔄 [TaskRemoved] 앱 스와이프 감지 → 포그라운드 알림 즉시 복구")
        // stopWithTask=false 이므로 서비스는 살아있음, 알림만 다시 올림
        startForegroundCompat()

        // 일부 기기에서 onTaskRemoved 후 서비스가 죽는 경우를 대비한 알람 백업
        val intent = Intent(this, BluetoothService::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(this, 9002, intent, flags)
        } else {
            PendingIntent.getService(this, 9002, intent, flags)
        }

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = SystemClock.elapsedRealtime() + 1_000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        } else {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        stopBluetoothScanning()
        try {
            unregisterReceiver(bluetoothReceiver)
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        runningInstance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null
}