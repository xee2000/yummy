package com.pms_parkin_mobile.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.pms_parkin_mobile.dto.AccelBeacon
import com.pms_parkin_mobile.dto.Beacon
import com.pms_parkin_mobile.dto.User
import java.util.LinkedList
import java.util.Queue

class App private constructor() {

    private var sharedPreferences: SharedPreferences? = null
    var context: Context? = null
        private set

    // ======== 기본 사용자 정보 (메모리 전용) ========
    var id: Int = 0
        private set
    var userId: String? = null
        private set
    var userPwd: String? = null
        private set
    var phone: String? = null
        private set
    var dong: String? = null
        private set
    var ho: String? = null
        private set
    var buildingId: Int = 0
        private set
    var latitude: Double = 0.0
        private set
    var longitude: Double = 0.0
        private set

    // ======== SharedPreferences 연동 프로퍼티 (코틀린 스타일 커스텀 Get/Set) ========

    var userName: String?
        get() = sharedPreferences?.getString("user_name", null)
        set(value) {
            sharedPreferences?.edit()?.putString("user_name", value)?.apply()
        }

    var isSensorStatus: Boolean
        get() = sharedPreferences?.getBoolean("sensor_status", false) ?: false
        set(value) {
            sharedPreferences?.edit()?.putBoolean("sensor_status", value)?.apply()
        }

    var isTestFlag: Boolean
        get() = sharedPreferences?.getBoolean("test_flag", false) ?: false
        set(value) {
            sharedPreferences?.edit()?.putBoolean("test_flag", value)?.apply()
        }

    var isServiceFlag: Boolean
        get() = sharedPreferences?.getBoolean("service_flag", false) ?: false
        set(value) {
            Log.d("SERVICE_FLAG", "App setServiceFlag : $value")
            sharedPreferences?.edit()?.putBoolean("service_flag", value)?.apply()
        }

    var isTestCompleteFlag: Boolean
        get() = sharedPreferences?.getBoolean("test_complete_flag", false) ?: false
        set(value) {
            sharedPreferences?.edit()?.putBoolean("test_complete_flag", value)?.apply()
        }

    var beaconMajor: Int
        get() = sharedPreferences?.getInt("beacon_major", 0) ?: 0
        set(value) {
            sharedPreferences?.edit()?.putInt("beacon_major", value)?.apply()
        }

    var beaconMinor: Int
        get() = sharedPreferences?.getInt("beacon_minor", 0) ?: 0
        set(value) {
            sharedPreferences?.edit()?.putInt("beacon_minor", value)?.apply()
        }

    var isPassOpenLobbyFlag: Boolean
        get() = sharedPreferences?.getBoolean("pass_open_lobby_flag", false) ?: false
        set(value) {
            sharedPreferences?.edit()?.putBoolean("pass_open_lobby_flag", value)?.apply()
        }

    var time: String?
        get() = sharedPreferences?.getString("time", "0000-00-00 00:00")
        set(value) {
            sharedPreferences?.edit()?.putString("time", value)?.apply()
        }

    var isPassiveCheck: Boolean
        get() = sharedPreferences?.getBoolean("passive_check", false) ?: false
        set(value) {
            sharedPreferences?.edit()?.putBoolean("passive_check", value)?.apply()
        }

    var passiveParkingEndTime: Long
        get() = sharedPreferences?.getLong("passive_parking_end_time", 0L) ?: 0L
        set(value) {
            sharedPreferences?.edit()?.putLong("passive_parking_end_time", value)?.apply()
        }

    var sensorTestMode: String?
        get() = sharedPreferences?.getString("sensor_test_mode", "테스트")
        set(value) {
            sharedPreferences?.edit()?.putString("sensor_test_mode", value)?.apply()
        }

    // ======== 일반 상태 변수들 (메모리 전용) ========
    var isExplicitlyStopped: Boolean = false
    var isStayTestResult: Boolean = false
    var isLeftTestResult: Boolean = false
    var isRightTestResult: Boolean = false
    var parkingCar: String? = ""
    var isTestStartFlag: Boolean = false
    var parkingBeaconId: AccelBeacon? = null
    var isSensorTestAllPassed: Boolean = false
    var isParkingStartFlag: Boolean = false

    var mCollectAccelBeaconStart: Boolean = false
    var mSaveCountRoll: Int = 0
    var mSaveCountPitch: Int = 0
    var mSaveCountYaw: Int = 0
    var mCollectStartCalcBeacon: Int = 0
    var mAfterStart: Boolean = false
    var mAfterGyroCount: Int = 0
    var mAfterStartCount: Int = 0
    var mAfterAccelCount: Int = 0
    var mPreState: String? = null
    var mAccelSequence: Int = 0
    var mParingStateValue: String? = "non-paring"
    var mAccelCount: Int = 0
    var mBeaconSequence: Int = 0
    var mWholeTimerDelay: Int = 0
    var sAVE_DELAY: Int = 0

    // ======== 컬렉션(리스트, 맵, 큐 등) ========
    var mBeaconArrayList: ArrayList<Beacon?>
    var mAccelBeaconArrayList: ArrayList<AccelBeacon?>
    var mAccelBeaconDelayArray: ArrayList<Int?>

    var mAccelBeaconMap: HashMap<String, AccelBeacon>
    var mAccelBeaconDelayMap: HashMap<String, LinkedHashSet<String>>

    var rOLL_QUEUE: Queue<Double>
    var pITCH_QUEUE: Queue<Double>
    var yAW_QUEUE: Queue<Double>

    private var saveDelayList: LinkedHashSet<String>

    val isPassiveCooldownActive: Boolean
        get() {
            val endTime = passiveParkingEndTime
            if (endTime == 0L) return false
            return (System.currentTimeMillis() - endTime) < PASSIVE_COOLDOWN_MS
        }

    // ======== 초기화 블록 ========
    init {
        mBeaconArrayList = ArrayList()
        mAccelBeaconArrayList = ArrayList()
        mAccelBeaconDelayArray = ArrayList()

        mAccelBeaconMap = HashMap()
        mAccelBeaconDelayMap = HashMap()

        rOLL_QUEUE = LinkedList()
        pITCH_QUEUE = LinkedList()
        yAW_QUEUE = LinkedList()

        saveDelayList = LinkedHashSet()

        rOLL_QUEUE.clear()
        pITCH_QUEUE.clear()
        yAW_QUEUE.clear()
    }

    // ======== 함수 영역 ========

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        this.context = context.applicationContext
        loadUserInfo()
        UserDataSingleton.instance.init(context)
    }

    fun setDelayList(value: String): LinkedHashSet<String> {
        saveDelayList.add(value)
        return saveDelayList
    }

    fun resetDelayList() {
        saveDelayList.clear()
    }

    fun setUser(user: User) {
        sharedPreferences?.edit()?.apply {
            // 💡 문제의 get...() 완전히 삭제 및 코틀린 프로퍼티 적용 완료
            putString("user_id", user.userId)
            putString("user_name", user.userName)
            putString("user_phone", user.phone)
            putInt("id", user.id)
            putString("user_pwd", user.userPwd)
            putString("dong", user.dong)
            putString("ho", user.ho)
            putInt("building_id", user.buildingId)
            putFloat("latitude", user.latitude.toFloat())
            putFloat("longitude", user.longitude.toFloat())
            apply()
        }

        // 💡 메모리 할당 부분도 get...() 삭제 완료
        this.id = user.id
        this.userId = user.userId
        this.userPwd = user.userPwd
        this.phone = user.phone
        this.userName = user.userName
        this.dong = user.dong
        this.ho = user.ho
        this.buildingId = user.buildingId
        this.latitude = user.latitude
        this.longitude = user.longitude
    }

    private fun loadUserInfo() {
        val prefs = sharedPreferences ?: return
        id = prefs.getInt("id", -1)
        userId = prefs.getString("user_id", null)
        userPwd = prefs.getString("user_pwd", null)
        phone = prefs.getString("user_phone", null)
        userName = prefs.getString("user_name", null)
        dong = prefs.getString("dong", null)
        ho = prefs.getString("ho", null)
        buildingId = prefs.getInt("building_id", 0)
        latitude = prefs.getFloat("latitude", 0f).toDouble()
        longitude = prefs.getFloat("longitude", 0f).toDouble()
    }

    fun clearUserInfo() {
        sharedPreferences?.edit()?.clear()?.apply()

        id = 0
        userId = null
        userPwd = null
        phone = null
        userName = null
        dong = null
        ho = null
        buildingId = 0
        latitude = 0.0
        longitude = 0.0
    }

    fun updateSensorTestAllPassed() {
        if (isStayTestResult && isLeftTestResult && isRightTestResult) {
            isSensorTestAllPassed = true
        }
    }

    // ======== 싱글톤 객체 (공유 인스턴스) ========
    companion object {
        private const val PREF_NAME = "user_pref"
        private const val PASSIVE_COOLDOWN_MS = 10 * 60 * 1000L

        @Volatile
        private var _instance: App? = null

        @JvmStatic
        val instance: App
            get() = _instance ?: synchronized(this) {
                _instance ?: App().also { _instance = it }
            }
    }
}