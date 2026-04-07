package com.pms_parkin_mobile.worker

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.pms_parkin_mobile.R
import com.pms_parkin_mobile.service.BluetoothService
import timber.log.Timber

/**
 * BluetoothServiceRestartWorker
 *
 * ForegroundServiceStartNotAllowedException 발생 시
 * WorkManager를 통해 BluetoothService를 재시작할 때 사용.
 *
 * WorkManager는 Expedited 작업으로 포그라운드 서비스 시작 권한을 가지므로
 * 앱이 백그라운드 상태일 때도 서비스 시작이 가능하다.
 *
 * [수정 사항]
 * 1. import android.R → import com.pms_parkin_mobile.R
 *    android.R.drawable.ic_menu_mylocation 은 시스템 아이콘으로 WorkManager
 *    ForegroundInfo에 사용 시 일부 기기에서 크래시 발생 가능.
 *    앱 자체 아이콘(R.drawable.logo)으로 교체.
 *
 * 2. 알림 채널 ID "woorisystem_ble_channel" → "ble_scan_channel"
 *    BluetoothService에서 생성하는 채널 ID와 일치시켜야 알림이 정상 표시됨.
 *    채널 ID 불일치 시 Android O+ 에서 알림이 무음/미표시될 수 있음.
 *
 * 3. getForegroundInfo() 단순화
 *    서비스 인스턴스 유무에 따라 분기하던 로직 제거.
 *    WorkManager가 getForegroundInfo()를 호출하는 시점에는
 *    서비스가 아직 미기동 상태이므로 인스턴스 체크가 의미 없음.
 */
class BluetoothServiceRestartWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        // BluetoothService.CHANNEL_ID 와 반드시 일치
        private const val NOTIFICATION_CHANNEL_ID = "ble_scan_channel"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.Forest.i("BluetoothServiceRestartWorker: BluetoothService 재시작")
            val intent = Intent(context, BluetoothService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Result.success()
        } catch (e: Exception) {
            Timber.Forest.e("BluetoothServiceRestartWorker: 재시작 실패 → ${e.message}")
            Result.retry()
        }
    }

    // WorkManager Expedited 작업에는 getForegroundInfo() 구현 필요
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("스마트파킹")
            .setContentText("서비스 재시작 중...")
            .setSmallIcon(R.drawable.logo) // ✅ 수정: android.R → 앱 자체 아이콘
            .setOngoing(true)
            .setSilent(true)
            .build()

        return ForegroundInfo(BluetoothService.Companion.FOREGROUND_NOTIFICATION_ID, notification)
    }
}