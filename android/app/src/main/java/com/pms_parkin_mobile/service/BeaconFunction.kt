package com.pms_parkin_mobile.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pms_parkin_mobile.R
import com.pms_parkin_mobile.api.RestController
import com.pms_parkin_mobile.dto.LobbyOpenData

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
        val userId = userData.getID()
        val lobbyOpenData = userData.getOpenMINOR()

        if (lobbyOpenData == null) {
            RestController.instance.Message("id : " + userId + "는 현재 lobby data없음")
            return
        }

        val minorHex = String.format("%04X", minor)

        for (data in lobbyOpenData) {
            val lobbyMinor = data.minor          // DB Hex 문자열 (ex: "0721")
            val targetRssi = data.rssi?.toDoubleOrNull() ?: -100.0

            RestController.instance.openLobbyinit("갖고있는 Minor : $lobbyMinor" + " 스캔된 Minor : " + minorHex)

            // DB Hex vs 스캔 Hex 직접 비교 (대소문자 무시)
            if (rssi >= targetRssi && lobbyMinor?.uppercase() == minorHex) {
                val newdata = LobbyOpenData().apply {
                    // decimal 문자열 전송 → 서버: parseInt → "%04X" → DB 조회
                    this.minor = minor.toString()
                    this.rssi = rssi.toString()
                    dong = userData.getDong()
                    ho = userData.getHo()
                    id = userData.getID()
                }

                RestController.instance.Message("api전송하려는 데이터 : $newdata")
                RestController.instance.openLobby(newdata) { returnCode, message ->
                    RestController.instance.Message("서버 응답 returnCode:  " + returnCode + " message : " + message)
                    if (returnCode == 0) {
                        App.instance.context?.let { ctx ->
                            try {
                                val userName = UserDataSingleton.instance.getUserName() ?: "입주민"
                                showNotification(ctx, userName)
                                Log.d("TEST", "🔔 서버 성공 응답 확인 후 알림 표시")
                                RestController.instance.Message("공동현관문 비컨 응답 ok : " + UserDataSingleton.instance.getUserName())
                            } catch (e: Exception) {
                                Log.e("TEST", "❌ 알림 표시 실패: ${e.message}")
                                RestController.instance.Message("공동현관문 비컨 응답 false : " + e.message)
                            }
                        }
                    } else {
                        Log.w("TEST", "⚠️ 서버 응답 실패 returnCode: $returnCode, message: $message")
                        RestController.instance.Message("서버 실패 → code:$returnCode, msg:$message")
                    }
                }
            } else if(rssi < targetRssi){
                RestController.instance.Message("스캔중인 비컨의 rssi신호 약함 : " + rssi)
//                RestController.instance.openLobbyRssiFail(userId)
            } else if(lobbyMinor?.uppercase() != minorHex){
                RestController.instance.Message("minor값 불일치 스캔된 minor: " + minorHex)

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