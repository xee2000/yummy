package com.pms_parkin_mobile.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pms_parkin_mobile.R
import com.pms_parkin_mobile.service.BeaconProcessor
import com.pms_parkin_mobile.service.BeaconTimerManager
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothService : Service() {

    companion object {
        private const val TAG = "BluetoothService"
        private const val CHANNEL_ID = "ble_scan_channel"
        const val FOREGROUND_NOTIFICATION_ID = 821

//        const val UUID_DONGTAN = "20151005-8864-5654-4159-013500201901"
        const val UUID_BANSEOK = "20151005-8864-5654-3020-013900202001"
//        const val UUID_PRIMO = "20151005-8864-5654-4623-010400240401"

        private const val BLE_DOORPHONE_SERVICE_UUID = "0000f1ae-0000-1000-8000-00805f9b34fb"

        private const val BEACON_MANUFACTURER_ID = 0x4C
        private const val BEACON_SUBTYPE = 0x02
        private const val BEACON_SUBTYPE_LENGTH = 0x15

        // Android throttle 방지: 30초 내 최대 4회
        private const val SCAN_THROTTLE_WINDOW_MS = 30_000L
        private const val SCAN_THROTTLE_MAX_COUNT = 4

        // 30초마다 헬스 체크
        private const val SCAN_HEALTH_CHECK_INTERVAL_MS = 30_000L

        // 60초 동안 결과 없으면 silent dead 판단
        private const val SCAN_DEAD_TIMEOUT_MS = 60_000L

        private const val RSSI_EMA_ALPHA = 0.3
        private const val RSSI_EMA_TIMEOUT_MS = 5_000L

        private const val ACTION_NOTIFICATION_DISMISSED = "com.pms_parkin_mobile.NOTIFICATION_DISMISSED"

        @Volatile
        var runningInstance: BluetoothService? = null
            private set

        @Volatile
        private var sendDetectBeaconToFlutter = false

        fun getInstance(): BluetoothService? = runningInstance

        fun isBeaconScanning(): Boolean = runningInstance?.isStartScanning == true

        fun getBluetoothState(context: Context): String {
            val manager = context.getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
            return when (manager?.adapter?.state) {
                BluetoothAdapter.STATE_ON -> "PoweredOn"
                BluetoothAdapter.STATE_OFF -> "PoweredOff"
                else -> "Unsupported"
            }
        }

        fun setSendDetectBeaconToFlutter(value: Boolean) {
            sendDetectBeaconToFlutter = value
        }

        fun getSendDetectBeaconToFlutter(): Boolean = sendDetectBeaconToFlutter

        @Volatile
        private var lastBeaconDetectedAt: Long = 0L

        fun updateLastBeaconDetectedAt() {
            lastBeaconDetectedAt = System.currentTimeMillis()
        }

        fun getLastBeaconDetectedAt(): String {
            if (lastBeaconDetectedAt == 0L) return "never"
            return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREA)
                .format(java.util.Date(lastBeaconDetectedAt))
        }

        fun getMinutesSinceLastBeacon(): Long {
            if (lastBeaconDetectedAt == 0L) return -1L
            return (System.currentTimeMillis() - lastBeaconDetectedAt) / 60_000L
        }
    }

    // -------------------------------------------------------------------------
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var leScanner: BluetoothLeScanner? = null
    private var scanCallback: BLEScanCallback? = null
    private var scanPendingIntent: PendingIntent? = null
    private var bluetoothReceiver: BLEBroadcastReceiver? = null
    private var screenReceiver: ScreenOnReceiver? = null
    private var notificationDismissReceiver: NotificationDismissReceiver? = null

    private val scanFilters = mutableListOf<ScanFilter>()
    private lateinit var scanSettings: ScanSettings

    @Volatile
    var isStartScanning = false
        private set

    // 마지막 스캔 결과 수신 시각 (silent dead 감지용)
    @Volatile
    var lastScanResultAt = 0L
        private set

    private val scanStartTimestamps = ArrayDeque<Long>()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var beaconProcessor: BeaconProcessor
    val parkingState: ParkingStateManager get() = beaconProcessor.state
    val beaconTimers: BeaconTimerManager get() = beaconProcessor.timers
    val beaconProcessorForTimer: BeaconProcessor get() = beaconProcessor

    private val rssiEmaMap = ConcurrentHashMap<String, Double>()
    private val rssiLastSeenMap = ConcurrentHashMap<String, Long>()

    private fun clearRssiEma() {
        rssiEmaMap.clear()
        rssiLastSeenMap.clear()
    }

    // =========================================================================
    // 헬스 체크 Runnable
    // 30초마다: isStartScanning=false → 재시작 / silent dead → 재생성 / 정상 → 재예약
    // =========================================================================
    private val scanHealthCheckRunnable = Runnable {
        val now = System.currentTimeMillis()
        val deadSince = if (lastScanResultAt > 0) now - lastScanResultAt else -1L
        val isSilentDead = isStartScanning && deadSince >= SCAN_DEAD_TIMEOUT_MS

        when {
            !isStartScanning -> {
                Timber.w("$TAG 헬스 체크: 스캔 미실행 → 재시작")
                if (bluetoothAdapter.isEnabled) startBluetoothScanning()
            }
            isSilentDead -> {
                Timber.w("$TAG 헬스 체크: silent dead 감지 (${deadSince / 1000}초 무응답) → 재생성")
                resetAndRestartScanning()
            }
            else -> {
                Timber.d(
                    "$TAG 헬스 체크: 정상 스캔 중 (마지막 결과 ${
                        if (lastScanResultAt > 0) "${(now - lastScanResultAt) / 1000}초 전" else "아직 없음"
                    })"
                )
                scheduleHealthCheck()
            }
        }
    }

    // =========================================================================
    // Service Lifecycle
    // =========================================================================
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        runningInstance = this
        Timber.i("$TAG: onCreate")

        bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        beaconProcessor = BeaconProcessor(applicationContext)

        createNotificationChannel()

        // 서비스 시작 시 Foreground 알림 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                buildNotification(bluetoothAdapter.isEnabled),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE // 매니페스트와 일치시킴
            )
        } else {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                buildNotification(bluetoothAdapter.isEnabled)
            )
        }

        // ✅ 리시버 등록 (Android 14 대응 플래그 추가)
        // 시스템 브로드캐스트(ACTION_STATE_CHANGED 등)는 기본적으로 시스템이 쏘는 것이지만,
        // Target SDK 34 이상에서는 명시적인 플래그를 요구합니다.

        bluetoothReceiver = BLEBroadcastReceiver()
        registerReceiver(
            bluetoothReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            RECEIVER_NOT_EXPORTED // 앱 내부용으로 제한 (보안 강화)
        )

        screenReceiver = ScreenOnReceiver()
        registerReceiver(
            screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON),
            RECEIVER_NOT_EXPORTED
        )

        notificationDismissReceiver = NotificationDismissReceiver()
        registerReceiver(
            notificationDismissReceiver,
            IntentFilter(ACTION_NOTIFICATION_DISMISSED),
            RECEIVER_NOT_EXPORTED
        )

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("$TAG: onStartCommand isStartScanning=$isStartScanning")

        // 알림이 제거된 경우 즉시 복구 (Android 14+ 에서 foreground 알림이 스와이프로 제거될 수 있음)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                buildNotification(bluetoothAdapter.isEnabled),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification(bluetoothAdapter.isEnabled))
        }

        if (bluetoothAdapter.isEnabled) {
            val now = System.currentTimeMillis()
            val deadSince = if (lastScanResultAt > 0) now - lastScanResultAt else -1L
            val isSilentDead = isStartScanning && deadSince >= SCAN_DEAD_TIMEOUT_MS

            when {
                isSilentDead -> {
                    Timber.w("$TAG: onStartCommand silent dead → 재생성")
                    resetAndRestartScanning()
                }
                isStartScanning -> {
                    Timber.i("$TAG: 이미 스캔 중 → 세션 유지")
                }
                else -> {
                    setupScanComponents()
                    startBluetoothScanning()
                }
            }
        }

        scheduleWatchdog()
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Timber.d("$TAG: onTaskRemoved → 서비스 재시작 예약")
        scheduleServiceRestart()
    }

    override fun onDestroy() {
        Timber.i("$TAG: onDestroy")

        handler.removeCallbacksAndMessages(null)
        stopBluetoothScanning()
        clearRssiEma()

        try { unregisterReceiver(bluetoothReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(notificationDismissReceiver) } catch (_: Exception) {}

        runningInstance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    // =========================================================================
    // 스캔 구성
    // =========================================================================
    private fun setupScanComponents() {
        scanFilters.clear()

        // iBeacon prefix 필터 (0x02 0x15)
        scanFilters.add(
            ScanFilter.Builder()
                .setManufacturerData(
                    BEACON_MANUFACTURER_ID,
                    byteArrayOf(BEACON_SUBTYPE.toByte(), BEACON_SUBTYPE_LENGTH.toByte()),
                    byteArrayOf(0xFF.toByte(), 0xFF.toByte())
                )
                .build()
        )

        scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .build()

        if (!isStartScanning) {
            leScanner = bluetoothAdapter.bluetoothLeScanner
            scanCallback = BLEScanCallback()
        }

        Timber.i("$TAG: setupScanComponents 완료 filters=${scanFilters.size}")
    }

    // =========================================================================
    // 스캔 시작 / 중지
    // =========================================================================
    fun startBluetoothScanning() {
        if (isStartScanning) return

        if (leScanner == null || scanCallback == null) {
            setupScanComponents()
        }
        if (leScanner == null) return

        // Throttle 체크
        val now = System.currentTimeMillis()
        scanStartTimestamps.removeAll { now - it > SCAN_THROTTLE_WINDOW_MS }
        if (scanStartTimestamps.size >= SCAN_THROTTLE_MAX_COUNT) {
            Timber.w("$TAG: BLE scan throttle 방지 - 1초 후 재시도")
            handler.postDelayed({ startBluetoothScanning() }, 1_000)
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // PendingIntent 기반 스캔: 앱이 죽어도 BleScanReceiver가 OS에서 직접 결과 수신
                val scanIntent = Intent(applicationContext, com.pms_parkin_mobile.receiver.BleScanReceiver::class.java)
                    .setAction("com.pms_parkin_mobile.BLE_SCAN_RESULT")
                val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                scanPendingIntent = PendingIntent.getBroadcast(applicationContext, 0, scanIntent, piFlags)
                leScanner!!.startScan(scanFilters, scanSettings, scanPendingIntent!!)
                Timber.i("$TAG: startScan(PendingIntent) 성공")
            } else {
                leScanner!!.startScan(scanFilters, scanSettings, scanCallback)
                Timber.i("$TAG: startScan(Callback) 성공")
            }

            isStartScanning = true
            scanStartTimestamps.addLast(now)
            scheduleHealthCheck()

        } catch (e: Exception) {
            Timber.e("$TAG: startScan 실패: ${e.message}")
        }
    }

    fun stopBluetoothScanning() {
        handler.removeCallbacks(scanHealthCheckRunnable)
        if (!isStartScanning) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && scanPendingIntent != null) {
                leScanner?.stopScan(scanPendingIntent)
            } else {
                leScanner?.stopScan(scanCallback)
            }
            Timber.i("$TAG: stopScan 성공")
        } catch (e: Exception) {
            Timber.e("$TAG: stopScan 실패: ${e.message}")
        } finally {
            isStartScanning = false
            scanPendingIntent = null
            lastScanResultAt = 0L
            clearRssiEma()
        }
    }

    fun resetAndRestartScanning() {
        Timber.i("$TAG: leScanner 재생성 시작")
        handler.removeCallbacks(scanHealthCheckRunnable)
        try {
            if (isStartScanning) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && scanPendingIntent != null) {
                        leScanner?.stopScan(scanPendingIntent)
                    } else {
                        leScanner?.stopScan(scanCallback)
                    }
                } catch (_: Exception) {}
                isStartScanning = false
            }
            leScanner = null
            scanCallback = null
            scanPendingIntent = null
            lastScanResultAt = 0L
            clearRssiEma()

            if (!bluetoothAdapter.isEnabled) {
                Timber.w("$TAG: BT 꺼짐 → 재생성 취소")
                return
            }
            leScanner = bluetoothAdapter.bluetoothLeScanner
            if (leScanner == null) {
                Timber.e("$TAG: leScanner 획득 실패")
                return
            }
            scanCallback = BLEScanCallback()

            handler.postDelayed({ startBluetoothScanning() }, 300)
            Timber.i("$TAG: leScanner 재생성 완료")
        } catch (e: Exception) {
            Timber.e("$TAG: resetAndRestartScanning 실패: ${e.message}")
        }
    }

    private fun scheduleHealthCheck() {
        handler.removeCallbacks(scanHealthCheckRunnable)
        handler.postDelayed(scanHealthCheckRunnable, SCAN_HEALTH_CHECK_INTERVAL_MS)
    }

    // =========================================================================
    // PendingIntent 경로 (BleScanReceiver → 여기로)
    // =========================================================================
    fun onPendingIntentResult(result: ScanResult) {
        lastScanResultAt = System.currentTimeMillis()
        updateLastBeaconDetectedAt()
        parseScanResult(result)
    }

    fun onPendingIntentError(errorCode: Int) {
        Timber.e("$TAG: PendingIntent 스캔 오류 errorCode=$errorCode → 재생성")
        isStartScanning = false
        handler.postDelayed({ resetAndRestartScanning() }, 2_000)
    }

    // =========================================================================
    // AlarmManager Watchdog / 서비스 재시작
    // =========================================================================
    private fun scheduleWatchdog() {
        val intent = Intent(applicationContext, BluetoothService::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(applicationContext, 2, intent, flags)
        } else {
            PendingIntent.getService(applicationContext, 2, intent, flags)
        }
        (getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
            ?.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60_000L,
                pi
            )
    }

    fun scheduleServiceRestart() {
        val intent = Intent(applicationContext, BluetoothService::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(applicationContext, 1, intent, flags)
        } else {
            PendingIntent.getService(applicationContext, 1, intent, flags)
        }
        (getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
            ?.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1_000,
                pi
            )
    }

    // =========================================================================
    // 비콘 파싱 및 처리
    // =========================================================================
    private fun parseScanResult(result: ScanResult) {
        val rssi = result.rssi.toDouble()
        if (rssi == 127.0) return

        val scanRecord: ScanRecord = result.scanRecord ?: return
        val bytes = scanRecord.getManufacturerSpecificData(BEACON_MANUFACTURER_ID) ?: return
        if (bytes.size < 23) return

        if ((bytes[0].toInt() and 0xFF) != BEACON_SUBTYPE ||
            (bytes[1].toInt() and 0xFF) != BEACON_SUBTYPE_LENGTH) return

        // UUID 파싱 시 하이픈 형식으로 조합
        val uuidRaw = bytesToHex(bytes, 2, 16, upper = false)
        val uuid = "${uuidRaw.substring(0,8)}-${uuidRaw.substring(8,12)}-${uuidRaw.substring(12,16)}-${uuidRaw.substring(16,20)}-${uuidRaw.substring(20)}"

        if (uuid != UUID_BANSEOK) return

        val major = bytesToHex(bytes, 18, 2, upper = true).toInt(16)
        val minor = bytesToHex(bytes, 20, 2, upper = true).toInt(16)

        onBeaconDetected(uuid, major, minor, rssi)
    }

    private fun onBeaconDetected(uuid: String, major: Int, minor: Int, rssi: Double) {
        val app = App.instance
        val emaKey = when (major) {
            1, 4, 5 -> "$major-$minor"
            else -> "$major"
        }

        // 1. RSSI 보정 (EMA 필터 적용)
        val smoothedRssi = smoothRssi(emaKey, rssi)
        Timber.v("$TAG EMA key=$emaKey raw=${"%.1f".format(rssi)} smoothed=${"%.1f".format(smoothedRssi)}")

        // 2. 신호 세기 컷오프 (-90dBm 이하는 무시)
        if (smoothedRssi < -90) {
            if (major == 4) beaconProcessor.keepParkingServiceAlive()
            return
        }


        // 4. 자동 주차 로직 (isPassiveCheck 가 false 일 때만 실행)
        when (major) {
            1 -> BeaconFunction().OnlyOpenLobby(minor, smoothedRssi)
            4 -> {
                if (app.isPassiveCheck) {
                    handlePassiveParkingData(major, minor, smoothedRssi)
                    return // 수동 주차 데이터 수집 시 자동 로직은 타지 않음
                }

                Timber.i("$TAG: 4번비컨 주차시작 비컨 주입확인")
                beaconProcessor.processStayParking(minor, smoothedRssi)
            }
            5 -> {
                Timber.i("$TAG: 5번비컨 주차구역 변경 비컨 주입확인")
                beaconProcessor.processChangeParking(major, minor, smoothedRssi)
            }
        }
    }

    // =========================================================================
    // UUID / Hex 유틸
    // =========================================================================
    private fun bytesToHex(bytes: ByteArray, offset: Int, length: Int, upper: Boolean): String {
        val sb = StringBuilder()
        for (i in offset until offset + length) {
            sb.append(
                if (upper) "%02X".format(bytes[i].toInt() and 0xFF)
                else "%02x".format(bytes[i].toInt() and 0xFF)
            )
        }
        return sb.toString()
    }

    // =========================================================================
    // RSSI EMA 필터
    // =========================================================================
    private fun smoothRssi(key: String, rawRssi: Double): Double {
        val now = System.currentTimeMillis()
        val prev = rssiEmaMap[key]
        val lastSeen = rssiLastSeenMap[key]

        val isReset = prev == null || lastSeen == null || (now - lastSeen) > RSSI_EMA_TIMEOUT_MS
        val smoothed = if (isReset) {
            if (lastSeen != null) Timber.d("$TAG EMA reset key=$key (gap=${now - lastSeen}ms)")
            rawRssi
        } else {
            RSSI_EMA_ALPHA * rawRssi + (1.0 - RSSI_EMA_ALPHA) * prev
        }

        rssiEmaMap[key] = smoothed
        rssiLastSeenMap[key] = now
        return smoothed
    }

    // =========================================================================
    // Foreground 알림
    // =========================================================================
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "BLE 스캔 서비스", NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                ?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isBluetoothOn: Boolean): Notification {
        val text = if (isBluetoothOn) "비콘 스캔 중입니다." else "블루투스가 꺼져 있습니다."
        val dismissIntent = Intent(ACTION_NOTIFICATION_DISMISSED).setPackage(packageName)
        val dismissPiFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val dismissPi = PendingIntent.getBroadcast(this, 99, dismissIntent, dismissPiFlags)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("스마트파킹")
            .setContentText(text)
            .setSmallIcon(R.drawable.logo)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setDeleteIntent(dismissPi)
            .build()
    }

    private fun updateNotification(isBluetoothOn: Boolean) {
        (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
            ?.notify(FOREGROUND_NOTIFICATION_ID, buildNotification(isBluetoothOn))
    }

    // =========================================================================
    // Inner Class: BLEScanCallback (Android < O 용)
    // =========================================================================
    private inner class BLEScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            lastScanResultAt = System.currentTimeMillis()
            updateLastBeaconDetectedAt()
            parseScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            if (results.isNotEmpty()) {
                lastScanResultAt = System.currentTimeMillis()
                updateLastBeaconDetectedAt()
            }
            results.forEach { parseScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("$TAG: onScanFailed errorCode=$errorCode")
            isStartScanning = false

            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> {
                    isStartScanning = true
                    scheduleHealthCheck()
                    Timber.w("$TAG: 이미 스캔 중 (ALREADY_STARTED) → 무시")
                }
                SCAN_FAILED_INTERNAL_ERROR, SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                    Timber.w("$TAG: leScanner 오류 (code=$errorCode) → 재생성 후 재시도")
                    handler.postDelayed({ resetAndRestartScanning() }, 5_000L)
                }
                else -> {
                    Timber.w("$TAG: scan 오류 (code=$errorCode) → 3초 후 재시도")
                    handler.postDelayed({
                        if (!isStartScanning) startBluetoothScanning()
                    }, 3_000L)
                }
            }
        }
    }

    // =========================================================================
    // Inner Class: BLEBroadcastReceiver (Bluetooth On/Off 감지)
    // =========================================================================
    private inner class BLEBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    Timber.d("$TAG: Bluetooth On → 스캔 재시작")
                    updateNotification(true)
                    setupScanComponents()
                    startBluetoothScanning()
                }
                BluetoothAdapter.STATE_OFF -> {
                    Timber.d("$TAG: Bluetooth Off → 스캔 중지")
                    updateNotification(false)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && scanPendingIntent != null) {
                            leScanner?.stopScan(scanPendingIntent)
                        } else {
                            leScanner?.stopScan(scanCallback)
                        }
                    } catch (_: Exception) {}
                    leScanner = null
                    scanCallback = null
                    scanPendingIntent = null
                    isStartScanning = false
                    lastScanResultAt = 0L
                }
            }
        }
    }

    // =========================================================================
    // Inner Class: ScreenOnReceiver (화면 켜짐 시 스캔 복구)
    // =========================================================================
    private inner class ScreenOnReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_SCREEN_ON) return
            Timber.d("$TAG: 화면 켜짐 → 스캔 상태 확인")
            if (!isStartScanning && bluetoothAdapter.isEnabled) {
                Timber.w("$TAG: 화면 켜짐 후 스캔 미실행 → 재생성 후 재시작")
                resetAndRestartScanning()
            }
        }
    }

    // =========================================================================
    // Inner Class: NotificationDismissReceiver (알림 제거 시 즉시 복구)
    // =========================================================================
    private inner class NotificationDismissReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_NOTIFICATION_DISMISSED) return
            Timber.w("$TAG: 알림이 제거됨 → 알림 즉시 복구")
            startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification(bluetoothAdapter.isEnabled))
            if (!isStartScanning && bluetoothAdapter.isEnabled) {
                Timber.w("$TAG: 알림 복구 시 스캔 미실행 감지 → 재시작")
                startBluetoothScanning()
            }
        }
    }
    private fun handlePassiveParkingData(major: Int, minor: Int, rssi: Double) {
        val app = App.instance
        val id = if (minor > 32768) minor - 32768 else minor
        val hexId = "%04X".format(id)
        val currentDelay = app.mWholeTimerDelay

        synchronized(app.mAccelBeaconMap) {
            val accelBeacon = app.mAccelBeaconMap.getOrPut(hexId) {
                com.pms_parkin_mobile.dto.AccelBeacon().apply {
                    this.beaconId = hexId
                    this.minor = minor.toString()
                    this.delayList = LinkedHashSet<String?>() as LinkedHashSet<String?>?
                }
            }

            // "RSSI_Delay" 형식으로 기록 저장
            val record = "${rssi.toInt()}_$currentDelay"
            accelBeacon.delayList?.add(record)

            Log.d("Passive", "📍 [수동주차 수집] $hexId -> $record (현재 수집량: ${accelBeacon.delayList?.size})")
        }
    }

}
