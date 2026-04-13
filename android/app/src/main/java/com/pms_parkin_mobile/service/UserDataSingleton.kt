package com.pms_parkin_mobile.service

import android.content.Context
import android.content.SharedPreferences
import com.pms_parkin_mobile.dto.LobbyOpenData
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class UserDataSingleton private constructor() {
    private var Dong: String? = null
    private var Ho: String? = null
    private var UserName: String? = null
    private var Cel: String? = null
    private var ID: String? = null
    var isIsDriver: Boolean = false
        private set
    var userId: String? = null
        get() = field ?: sharedPreferences?.getString("user_id", null)
        set(value) {
            field = value
            save("user_id", value)
        }
    private var OpenMINOR: ArrayList<LobbyOpenData>?
    var bigDataSend: Boolean? = null
        get() = field == null || field == true
        set(bigDataSend) {
            field = bigDataSend == null || bigDataSend
        }

    private var sharedPreferences: SharedPreferences? = null

    init {
        OpenMINOR = ArrayList<LobbyOpenData>()
    }

    /** App.init() 에서 호출 — 프로세스 재시작 후에도 데이터 복원  */
    fun init(context: Context) {
        sharedPreferences = context.getApplicationContext()
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        if (sharedPreferences == null) return
        Dong = sharedPreferences!!.getString("dong", null)
        Ho = sharedPreferences!!.getString("ho", null)
        UserName = sharedPreferences!!.getString("user_name", null)
        Cel = sharedPreferences!!.getString("cel", null)
        ID = sharedPreferences!!.getString("id", null)
        this.isIsDriver = sharedPreferences!!.getBoolean("is_driver", false)
        userId = sharedPreferences!!.getString("user_id", null)

        val jsonStr = sharedPreferences!!.getString("open_minor", null)
        if (jsonStr != null) {
            try {
                OpenMINOR = ArrayList<LobbyOpenData>()
                val arr = JSONArray(jsonStr)
                for (i in 0..<arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val data = LobbyOpenData()
                    data.minor = obj.optString("minor")
                    data.rssi = obj.optString("rssi")
                    data.id = obj.optString("id")
                    data.dong = obj.optString("dong")
                    data.ho = obj.optString("ho")
                    OpenMINOR!!.add(data)
                }
            } catch (e: JSONException) {
                OpenMINOR = ArrayList<LobbyOpenData>()
            }
        }
    }

    // ======== Getters ========
    fun getDong(): String? {
        return Dong
    }

    fun getHo(): String? {
        return Ho
    }

    fun getUserName(): String? {
        return UserName
    }

    fun getCel(): String? {
        return Cel
    }

    fun getID(): String? {
        return ID
    }

//    fun getUserId(): String? {
//        return userId
//    }

    fun getOpenMINOR(): ArrayList<LobbyOpenData> {
        return OpenMINOR!!
    }

    // ======== Setters (SharedPreferences 동시 저장) ========
    fun setDong(dong: String?) {
        this.Dong = dong
        save("dong", dong)
    }

    fun setHo(ho: String?) {
        this.Ho = ho
        save("ho", ho)
    }

    fun setUserName(userName: String?) {
        this.UserName = userName
        save("user_name", userName)
    }

    fun setCel(cel: String?) {
        this.Cel = cel
        save("cel", cel)
    }

    fun setID(id: String?) {
        this.ID = id
        save("id", id)
    }

    fun setIsDriver(isDriver: Boolean) {
        this.isIsDriver = isDriver
        if (sharedPreferences != null) {
            sharedPreferences!!.edit().putBoolean("is_driver", isDriver).apply()
        }
    }

//    fun setUserId(userId: String?) {
//        this.userId = userId
//        save("user_id", userId)
//    }

    fun setOpenMINOR(openMINOR: ArrayList<LobbyOpenData>?) {
        this.OpenMINOR = openMINOR
        if (sharedPreferences == null || openMINOR == null) return
        try {
            val arr = JSONArray()
            for (data in openMINOR) {
                val obj = JSONObject()
                obj.put("minor", data.minor)
                obj.put("rssi", data.rssi)
                obj.put("id", data.id)
                obj.put("dong", data.dong)
                obj.put("ho", data.ho)
                arr.put(obj)
            }
            sharedPreferences!!.edit().putString("open_minor", arr.toString()).apply()
        } catch (ignore: JSONException) {
        }
    }

    private fun save(key: String?, value: String?) {
        if (sharedPreferences != null) {
            sharedPreferences!!.edit().putString(key, value).apply()
        }
    }

    companion object {
        private const val PREF_NAME = "user_data_pref"

        val instance: UserDataSingleton = UserDataSingleton()
    }
}
