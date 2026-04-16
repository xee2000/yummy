package com.pms_parkin_mobile.module

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.pms_parkin_mobile.dto.LobbyOpenData
import com.pms_parkin_mobile.service.UserDataSingleton
import org.json.JSONException
import org.json.JSONObject

class UserIntent : IntentService("UserIntent") {
    override fun onHandleIntent(intent: Intent?) {
        Log.d("UserIntent", "Service started and running in the background.")


        if (intent != null) {
            try {
                val data = intent.getStringExtra("user")
                if (data != null) {
                    Log.d("UserIntent", "Received data: " + data)

                    // 1. JSON 객체 생성
                    val root = JSONObject(data)

                    // 2. 동탄은 result 하위 중첩, 광교는 루트 — 둘 다 대응
                    val payload = if (root.has("result")) root.getJSONObject("result") else root

                    // 3. 싱글톤 인스턴스 가져오기 및 데이터 세팅
                    val userStore = UserDataSingleton.instance

                    // 키 없거나 빈 값이면 null 반환하는 헬퍼
                    fun resolve(vararg keys: String): String? {
                        for (key in keys) {
                            val v = payload.optString(key, null)
                                ?: root.optString(key, null)
                            if (!v.isNullOrEmpty()) return v
                        }
                        return null
                    }

                    userStore.setDong(resolve("dong"))
                    userStore.setHo(resolve("ho"))
                    userStore.setUserName(resolve("name"))
                    userStore.setCel(resolve("cel"))
                    val resolvedUserId = resolve("userId", "id")
                    userStore.setID(resolve("userId"))
                    userStore.setArea(resolve("area"))
                    userStore.setUUID(resolve("uuid"))

                    Log.d("UserIntent", "dong=${userStore.getDong()} ho=${userStore.getHo()} userId=$resolvedUserId area=${userStore.getArea()}")

                    // 4. minorList 파싱 — payload 또는 root 어디에 있든 처리
                    val minorArray = when {
                        payload.has("minorList") -> payload.getJSONArray("minorList")
                        root.has("minorList")    -> root.getJSONArray("minorList")
                        else                     -> null
                    }
                    if (minorArray != null) {
                        val minorList = ArrayList<LobbyOpenData>()
                        for (i in 0..<minorArray.length()) {
                            val item = minorArray.getJSONObject(i)
                            val lobbyData = LobbyOpenData()
                            lobbyData.minor = item.optString("minor")
                            lobbyData.rssi   = item.optString("rssi")
                            lobbyData.id     = resolvedUserId
                            lobbyData.dong   = userStore.getDong()
                            lobbyData.ho     = userStore.getHo()
                            minorList.add(lobbyData)
                        }
                        Log.d("Ble", "minorList : $minorList")
                        userStore.setOpenMINOR(minorList)
                    }
                } else {
                    Log.e("UserIntent", "Received data is null")
                }
            } catch (e: JSONException) {
                Log.e("UserIntent", "JSON Parsing Error", e)
            }
        }
    }
}

