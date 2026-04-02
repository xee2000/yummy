package com.pms_parkin_mobile.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pms_parkin_mobile.R
import com.pms_parkin_mobile.dto.LobbyOpenData
import com.pms_parkin_mobile.dto.User
import com.pms_parkin_mobile.foreground.OpenLobbyAlarm
import com.pms_parkin_mobile.service.App
import com.pms_parkin_mobile.service.TotalCompressor.toGzipBytes
import com.pms_parkin_mobile.service.UserDataSingleton
import com.pms_parkin_mobile.data.SensorDataStore
import com.woorisystem.domain.ParkingTotal
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RestController private constructor() {
    private val retrofitAPI: RetrofitAPI = RetropitClient.apiService

    private var isRetryingNow = false
    private var networkCallback: NetworkCallback? = null
    private var networkLoadingTimer: CountDownTimer? = null

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getUserId(userId: Int?) {
        Log.d(TAG, "getUserId 전송 시작 - id: $userId")
        val call = retrofitAPI.getUserId(userId)

        call?.enqueue(object : Callback<User?> {
            override fun onResponse(call: Call<User?>, response: Response<User?>) {
                if (response.isSuccessful && response.body() != null) {
                    Log.d(TAG, "getUserId 조회 성공: ${response.body()}")
                } else {
                    Log.e(TAG, "getUserId 조회 실패 (코드): ${response.code()}")
                }
            }

            override fun onFailure(call: Call<User?>, t: Throwable) {
                Log.e(TAG, "getUserId 네트워크 오류: ${t.message}")
            }
        })
    }

    fun gyroinfo(userId: String?, errorcode: Int) {
        Log.d(TAG, "gyroinfo 전송 시작 - User: $userId, Code: $errorcode")
        val call = retrofitAPI.gyroinfo(userId, errorcode)

        call?.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "gyroinfo 전송 성공")
                } else {
                    Log.e(TAG, "gyroinfo 전송 실패 (코드): ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                Log.e(TAG, "gyroinfo 네트워크 오류: ${t.message}")
            }
        })
    }

    fun errorMessage(userId: String?, errorMessage: String?) {
        Log.d(TAG, "errorMessage 전송 시작 - User: $userId, Msg: $errorMessage")
        val call = retrofitAPI.errorMessage(userId, errorMessage)

        call?.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "서버 에러 로그 전송 성공 [User: $userId]")
                } else {
                    Log.e(TAG, "서버 에러 로그 전송 실패 (코드: ${response.code()})")
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                Log.e(TAG, "서버 에러 로그 전송 중 네트워크 오류: ${t.message}")
            }
        })
    }

    @Synchronized
    fun Parking(context: Context, parkingTotal: ParkingTotal) {
        Log.d("TEST", "🌀 [PARKING 진입]")

        if (isRetryingNow) {
            Log.d("TEST", "🚫 이미 재시도 중 - 무시")
            return
        }

        if (!isNetworkConnected(context)) {
            Log.d("TEST", "📴 네트워크 없음 - 연결 감지 대기 중")
            waitForNetwork(context, parkingTotal)
            return
        }

        sendParking(context, parkingTotal)
    }

    fun openLobby(lobbyOpenData: LobbyOpenData) {
        Log.d(TAG, "openLobby 요청 시작 - Minor: ${lobbyOpenData.minor}")
        val call = retrofitAPI.openLobby(
            lobbyOpenData.id,
            lobbyOpenData.dong,
            lobbyOpenData.ho,
            lobbyOpenData.minor,
            lobbyOpenData.rssi
        )

        call?.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "openLobby 성공 [Minor: ${lobbyOpenData.minor}]")
                    val context = App.instance?.context ?: return
                    val intent = Intent(context, OpenLobbyAlarm::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    Log.e(TAG, "openLobby 실패 (코드): ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                Log.e(TAG, "openLobby 네트워크 오류: ${t.message}")
            }
        })
    }

    fun openLobbyinit(id: String?) {
        Log.d(TAG, "openLobbyinit 요청 시작 - ID: $id")
        val call = retrofitAPI.openLobbyinit(id)

        call?.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "openLobbyinit 성공")
                } else {
                    Log.e(TAG, "openLobbyinit 실패 (코드): ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                Log.e(TAG, "openLobbyinit 네트워크 오류: ${t.message}")
            }
        })
    }

    fun openLobbyDataNull(id: String?) {
        Log.d(TAG, "openLobbyDataNull 요청 시작 - ID: $id")
        val call = retrofitAPI.openLobbyDataNull(id)

        call?.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "openLobbyDataNull 전송 성공")
                } else {
                    Log.e(TAG, "openLobbyDataNull 전송 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                Log.e(TAG, "openLobbyDataNull 네트워크 오류: ${t.message}")
            }
        })
    }

    fun openLobbyRssiFail(id: String?) {
        Log.d(TAG, "openLobbyRssiFail 요청 시작 - ID: $id")
        val call = retrofitAPI.openLobbyRssiFail(id)

        call?.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "openLobbyRssiFail 전송 성공")
                } else {
                    Log.e(TAG, "openLobbyRssiFail 전송 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                Log.e(TAG, "openLobbyRssiFail 네트워크 오류: ${t.message}")
            }
        })
    }

    fun Message(message: String?) {
        val userId = App.instance?.userId?.takeIf { it.isNotEmpty() }
            ?: UserDataSingleton.instance.userId
        Log.d(TAG, "GateInformation - message: $message")

        val call = retrofitAPI.Message(userId, message)
        call?.enqueue(object : Callback<Void?> {
            override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "GateInformation 전송 성공: $message")
                } else {
                    Log.e(TAG, "GateInformation 전송 실패 (코드): ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Void?>, t: Throwable) {
                Log.e(TAG, "GateInformation 네트워크 오류: ${t.message}")
            }
        })
    }

    private fun sendParking(context: Context, total: ParkingTotal) {
        isRetryingNow = true

        val userId = UserDataSingleton.instance.userId
        val dong = UserDataSingleton.instance.getDong()
        val ho = UserDataSingleton.instance.getHo()

        Log.d("TEST", "userId : " + userId)
        UserDataSingleton.instance.bigDataSend?.let {
            if (!it) {
                Log.d("TEST", "normalFlag : ${UserDataSingleton.instance.bigDataSend}")
                val call = retrofitAPI.Parking(total, userId, dong, ho)
                call?.enqueue(createParkingCallback(context, total))
            } else {
                Log.d("TEST", "bigFlag : ${UserDataSingleton.instance.bigDataSend}")
                val tempGyroList2 = total.gyroList2
                total.gyroList2 = null

                val file = toGzipBytes(total)
                total.file = file
                total.gyroList2 = tempGyroList2

                Log.d("TEST", "total2 : ${total.gyroList2}")
                val call = retrofitAPI.Parking(total, userId, dong, ho)
                call?.enqueue(createParkingCallback(context, total))
            }
        }
    }

    private fun createParkingCallback(context: Context, total: ParkingTotal) = object : Callback<Void?> {
        override fun onResponse(call: Call<Void?>, response: Response<Void?>) {
            isRetryingNow = false
            if (response.isSuccessful) {
                Log.d("TEST", "✅ Parking 요청 성공")
                networkLoadingTimer?.let {
                    Log.d("TEST", "⏳ 타이머 중단")
                    it.cancel()
                    networkLoadingTimer = null
                }
                DataConnectSuccessForeground(context)
                SensorDataStore.instance.reset()
            } else {
                Log.e("TEST", "❌ 응답 실패 - code: ${response.code()}")
                waitForNetwork(context, total)
            }
        }

        override fun onFailure(call: Call<Void?>, t: Throwable) {
            isRetryingNow = false
            Log.e("TEST", "🔥 요청 실패 - 에러: ${t.message}")
            Log.e("TEST", "🔥 요청 실패 URL = ${call.request().url}")
            Log.e("TEST", "🔥 Throwable = $t")
            Log.e("TEST", "🔥 Class = ${t.javaClass.name}")
            Log.e("TEST", "🔥 Cause = ${t.cause}")
            Log.e("TEST", "🔥 Stack = ", t)
            parkingApiTimer(context, total)
        }
    }

    private fun waitForNetwork(context: Context, total: ParkingTotal) {
        if (networkCallback != null) {
            Log.d("TEST", "⏳ 이미 네트워크 콜백 등록됨 - 중복 방지")
            return
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            Log.e("TEST", "❌ ConnectivityManager 가 null - 타이머 방식으로 대체")
            parkingApiTimer(context, total)
            return
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("TEST", "🌐 네트워크 복구 감지 - Parking 재전송 시도")
                try {
                    cm.unregisterNetworkCallback(this)
                } catch (e: Exception) {
                    Log.w("TEST", "unregisterNetworkCallback 실패/중복 해제 가능", e)
                }
                networkCallback = null

                Handler(Looper.getMainLooper()).post {
                    if (!isRetryingNow) {
                        sendParking(context, total)
                    } else {
                        Log.d("TEST", "이미 재시도 중이어서 중복 전송은 하지 않음")
                    }
                }
            }

            override fun onLost(network: Network) {
                Log.d("TEST", "📴 네트워크 다시 끊김")
            }
        }

        cm.registerNetworkCallback(request, networkCallback!!)
        Log.d("TEST", "📡 네트워크 감지 콜백 등록 완료")
    }

    private fun DataConnectSuccessForeground(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "parking_success_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "주차 알림", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("주차위치 확인")
            .setContentText("성공적으로 주차가 완료되었습니다.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(1004, builder.build())
    }

    private fun parkingApiTimer(context: Context, total: ParkingTotal) {
        if (networkLoadingTimer != null) {
            Log.d("TEST", "⏳ 타이머 이미 동작 중 - 중복 방지")
            return
        }

        networkLoadingTimer = object : CountDownTimer(Long.MAX_VALUE, 5000) {
            override fun onTick(millisUntilFinished: Long) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNetwork = cm?.activeNetworkInfo
                val isConnected = activeNetwork?.isConnected == true

                Log.d("TEST", "🕒 5초마다 네트워크 체크")

                if (isConnected) {
                    Log.d("TEST", "🌐 네트워크 연결됨 - API 전송 및 타이머 종료")
                    sendParking(context, total)
                    cancel()
                    networkLoadingTimer = null
                } else {
                    Log.d("TEST", "📴 아직 네트워크 연결 안 됨")
                }
            }

            override fun onFinish() {}
        }

        networkLoadingTimer?.start()
    }

    companion object {
        private const val TAG = "RestController"

        @Volatile
        private var INSTANCE: RestController? = null

        @JvmStatic
        val instance: RestController
            get() = INSTANCE ?: synchronized(this) {
                INSTANCE ?: RestController().also { INSTANCE = it }
            }
    }
}
