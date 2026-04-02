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
                    val jsonObject = JSONObject(data)

                    // 2. 싱글톤 인스턴스 가져오기 및 데이터 세팅
                    val userStore = UserDataSingleton.instance

                    userStore.setDong(jsonObject.optString("dong"))
                    userStore.setHo(jsonObject.optString("ho"))
                    userStore.setUserName(jsonObject.optString("name"))
                    userStore.setCel(jsonObject.optString("cel"))
                    userStore.userId = jsonObject.optString("userId")
                    // 3. minorList 파싱 및 ArrayList 저장
                    if (jsonObject.has("minorList")) {
                        val jsonArray = jsonObject.getJSONArray("minorList")
                        val minorList = ArrayList<LobbyOpenData>()

                        for (i in 0..<jsonArray.length()) {
                            val item = jsonArray.getJSONObject(i)
                            val lobbyData = LobbyOpenData()
                            lobbyData.minor = item.optString("minor")
                            lobbyData.rssi = item.optString("rssi")
                            lobbyData.id = jsonObject.optString("userId")
                            lobbyData.dong = jsonObject.optString("dong")
                            lobbyData.ho = jsonObject.optString("ho")
                            minorList.add(lobbyData)
                        }
                        Log.d("Ble", "minorList : " + minorList)
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

