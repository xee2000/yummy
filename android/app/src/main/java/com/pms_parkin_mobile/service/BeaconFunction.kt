package com.pms_parkin_mobile.service

import android.util.Log
import com.pms_parkin_mobile.api.RestController
import com.pms_parkin_mobile.dto.LobbyOpenData
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pms_parkin_mobile.R

class BeaconFunction {

    // ✅ 알림 전용 상수 및 함수 추가
    companion object {
        private const val CHANNEL_ID = "lobby_open_channel"
        private const val CHANNEL_NAME = "공동현관 알림"
        private const val NOTIFICATION_ID = 13

        @Volatile
        private var _instance: BeaconFunction? = null

        @JvmStatic
        fun getInstance(): BeaconFunction {
            return _instance ?: synchronized(this) {
                _instance ?: BeaconFunction().also { _instance = it }
            }
        }
    }

    /**
     * ✅ 서비스 없이 즉시 알림을 띄우는 전용 함수
     */
    private fun showNotification(context: Context, userName: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "공동현관문 열림 알림 채널"
            }
            manager.createNotificationChannel(channel)
        }

        // 2. 알림 빌드
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // 유효한 아이콘인지 확인 필요
            .setContentTitle("공동현관문 열림")
            .setContentText("${userName}님 공동현관문이 열렸습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // 터치 시 알림 삭제
            .build()

        // 3. 알림 표시
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun OnlyOpenLobby(minor: Int, rssi: Double) {
        Log.d("TEST", "minor : $minor")

        val userData = UserDataSingleton.instance
        val userId = userData.userId
        val lobbyOpenData = userData.getOpenMINOR()

        if (lobbyOpenData == null) {
            RestController.instance.openLobbyDataNull(userId)
            return
        }

        RestController.instance.openLobbyinit(userId)

        for (data in lobbyOpenData) {
            val lobbyMinor = data.minor
            val targetRssi = data.rssi?.toDoubleOrNull() ?: -100.0

            // RSSI 조건 충족 시
            if (rssi >= targetRssi) {
                // 1. 서버에 문열림 요청
                val newdata = LobbyOpenData().apply {
                    this.minor = lobbyMinor
                    this.rssi = rssi.toString()
                    dong = userData.getDong()
                    ho = userData.getHo()
                    id = userData.getID()
                }
                RestController.instance.openLobby(newdata)

                // 2. ✅ [수정] 서비스 시작 대신 직접 알림 호출
                App.instance.context?.let { ctx ->
                    try {
                        val userName = UserDataSingleton.instance.getUserName() ?: "입주민"
                        showNotification(ctx, userName)
                        Log.d("TEST", "🔔 RSSI 충족: 공동현관 알림 표시 완료 (Service 미사용)")
                    } catch (e: Exception) {
                        Log.e("TEST", "❌ 알림 표시 실패: ${e.message}")
                    }
                }

            } else {
                RestController.instance.openLobbyRssiFail(userId)
            }
        }
    }

    // AddAccelDelay 함수는 기존 로직 유지 (정렬 및 중복 제거 부분)
    fun AddAccelDelay(HexValue: String?, rssi: Double) {
        val app = App.instance ?: return
        val delay = app.mWholeTimerDelay
        val delayStr = delay.toString()
        val newEntry = "${rssi.toInt()}_$delayStr"

        val delayMap = app.mAccelBeaconDelayMap
        val set: LinkedHashSet<String> = (delayMap[HexValue] ?: LinkedHashSet<String>()) as LinkedHashSet<String>

        var toUpdate: String? = null

        for (entry in set) {
            val parts = entry.split("_", limit = 2)
            if (parts.size == 2 && parts[1] == delayStr) {
                val storedRssi = parts[0].toDoubleOrNull() ?: 0.0
                if (rssi > storedRssi) {
                    toUpdate = entry
                }
                break
            }
        }

        toUpdate?.let { set.remove(it) }
        set.add(newEntry)

        val sortedList = set.sortedBy { entry ->
            val parts = entry.split("_", limit = 2)
            if (parts.size == 2) {
                parts[1].toIntOrNull() ?: 0
            } else {
                0
            }
        }

        val sortedSet = LinkedHashSet<String>(sortedList)
        delayMap[HexValue as String] = sortedSet as LinkedHashSet<String?>? as LinkedHashSet<String>
    }
}