package com.pms_parkin_mobile.receiver

import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.pms_parkin_mobile.service.BluetoothService

/**
 * PendingIntent 기반 BLE 스캔 결과 수신기.
 * Android O+ 에서 BluetoothService가 PendingIntent로 스캔을 등록하면
 * OS가 앱 프로세스 상태에 무관하게 이 리시버에 결과를 직접 전달한다.
 */
class BleScanReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BleScanReceiver"
        private const val EXTRA_LIST_OF_SCAN_RESULTS = "android.bluetooth.le.extra.LIST_SCAN_RESULT"
        private const val EXTRA_ERROR_CODE = "android.bluetooth.le.extra.ERROR_CODE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        // 스캔 오류 처리
        val errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, -1)
        if (errorCode != -1) {
            Log.e(TAG, "PendingIntent 스캔 오류: errorCode=$errorCode")
            val instance = BluetoothService.runningInstance
            if (instance != null) {
                instance.onPendingIntentError(errorCode)
            } else {
                restartService(context)
            }
            return
        }

        // 스캔 결과 파싱
        @Suppress("UNCHECKED_CAST")
        val results: List<ScanResult>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_LIST_OF_SCAN_RESULTS, ScanResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_LIST_OF_SCAN_RESULTS)
        }

        if (results.isNullOrEmpty()) return

        val instance = BluetoothService.runningInstance
        if (instance == null) {
            Log.w(TAG, "BluetoothService 인스턴스 없음 → 서비스 재시작")
            restartService(context)
            return
        }

        for (result in results) {
            instance.onPendingIntentResult(result)
        }
    }

    private fun restartService(context: Context) {
        try {
            val serviceIntent = Intent(context, BluetoothService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "서비스 재시작 실패: ${e.message}")
        }
    }
}
