package com.pms_parkin_mobile.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.pms_parkin_mobile.service.BluetoothService

class BleScanReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BleScanReceiver"

        // 시스템이 전달하는 ScanResult 리스트 키값
        private const val EXTRA_LIST_OF_SCAN_RESULTS = BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT
        private const val EXTRA_ERROR_CODE = BluetoothLeScanner.EXTRA_ERROR_CODE

        const val ACTION_BLE_SCAN_RESULT = "com.pms_parkin_mobile.BLE_SCAN_RESULT"
        const val ACTION_ALARM = "com.pms_parkin_mobile.BLUETOOTH_ALARM"

        private const val ALARM_REQUEST_CODE = 9001
        private const val INTERVAL_8_MIN_MS = 8 * 60 * 1_000L

        /**
         * 외부(App/Service)에서 생존 알람을 등록할 때 사용
         */
        fun startAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // 이미 등록된 알람이 있는지 확인
            val existing = buildAlarmPendingIntent(context, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (existing != null) return

            val pi = buildAlarmPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) ?: return

            // Doze 모드에서도 동작하도록 setInexactRepeating 대신 유연하게 설정 가능하나, 8분 주기는 시스템 부하 고려하여 유지
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + INTERVAL_8_MIN_MS,
                INTERVAL_8_MIN_MS,
                pi
            )
            Log.i("TEST", "⏰ [Alarm] 8분 주기 생존 알람 등록 완료")
        }

        private fun buildAlarmPendingIntent(context: Context, flags: Int): PendingIntent? {
            val intent = Intent(context, BleScanReceiver::class.java).apply {
                action = ACTION_ALARM
            }
            return PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        when (action) {
            // ✅ 1. PendingIntent 스캔 결과 수신 (주로 화면 꺼짐 상태)
            ACTION_BLE_SCAN_RESULT -> {
                handleScanResult(context, intent)
            }

            // ✅ 2. 부팅 완료 또는 앱 업데이트
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i("TEST", "🚀 [System] 부팅/업데이트 완료 → 워치독 가동")
                startAlarm(context)
                restartService(context)
            }

            // ✅ 3. 8분 주기 워치독 알람 수신
            ACTION_ALARM -> {
                val instance = BluetoothService.runningInstance
                val isScanning = BluetoothService.isBeaconScanning()

                Log.d("TEST", "📡 [Watchdog] 알람 수신 (Service=${if(instance != null) "Running" else "Stopped"}, Scanning=$isScanning)")

                if (instance == null || !isScanning) {
                    Log.w("TEST", "⚠️ [Watchdog] 서비스 이상 감지 → 강제 재시작")
                    restartService(context)
                } else {
                    // 서비스는 떠있으나 스캔이 멈춘 경우를 대비해 Reset 호출
                    Log.d("TEST", "✅ [Watchdog] 상태 양호 → 스캔 세션 Refresh")
                    instance.resetAndRestartScanning()
                }
            }
        }
    }

    /**
     * PendingIntent를 통해 전달된 BLE 데이터를 파싱하여 서비스로 전달
     */
    private fun handleScanResult(context: Context, intent: Intent) {
        // 오류 코드 확인
        val errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, -1)
        if (errorCode != -1) {
            Log.e("TEST", "❌ [PI-Result] 스캔 오류 발생: errorCode=$errorCode")
            BluetoothService.runningInstance?.onPendingIntentError(errorCode) ?: restartService(context)
            return
        }

        // 결과 리스트 추출
        val results: List<ScanResult>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_LIST_OF_SCAN_RESULTS, ScanResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_LIST_OF_SCAN_RESULTS)
        }

        if (results.isNullOrEmpty()) return

        val service = BluetoothService.runningInstance
        if (service == null) {
            Log.w("TEST", "⚠️ [PI-Result] 수신 성공했으나 서비스 인스턴스 없음 → 재시작")
            restartService(context)
            return
        }

        // 서비스 내부의 파싱 로직으로 데이터 전달
        Log.d("TEST", "📩 [PI-Result] 스캔 결과 ${results.size}건 전달")
        for (result in results) {
            service.onPendingIntentResult(result)
        }
    }

    /**
     * BluetoothService를 포그라운드로 시작 (실패 시 WorkManager 폴백)
     */
    private fun restartService(context: Context) {
        val serviceIntent = Intent(context, BluetoothService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.i("TEST", "✅ [Restart] BluetoothService 시작 요청 성공")
        } catch (e: Exception) {
            Log.e("TEST", "❌ [Restart] 일반 시작 실패 (백그라운드 제한) → WorkManager 기동")
            scheduleWorkManagerFallback(context)
        }
    }

    private fun scheduleWorkManagerFallback(context: Context) {
        try {
            val request = androidx.work.OneTimeWorkRequestBuilder<com.pms_parkin_mobile.worker.BluetoothServiceRestartWorker>()
                .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "ble_restart_watchdog",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    request
                )
        } catch (e: Exception) {
            Log.e(TAG, "❌ [WorkManager] 예약 최종 실패: ${e.message}")
        }
    }
}