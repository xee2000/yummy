package com.pms_parkin_mobile.module

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.pms_parkin_mobile.service.App
import com.pms_parkin_mobile.service.BluetoothService
import com.pms_parkin_mobile.service.PassiveParkingService
import com.pms_parkin_mobile.service.SensorService
import com.pms_parkin_mobile.util.PermissionManager

class AndroidModule(context: ReactApplicationContext) : ReactContextBaseJavaModule(context) {

    private val handler = Handler(Looper.getMainLooper())

    override fun getName(): String {
        return "AndroidModule"
    }

    @ReactMethod
    fun StartApplication() {
        Log.d("AndroidModule", "StartApplication called")
        val ctx: Context = reactApplicationContext
        ctx.startService(Intent(ctx, UserIntent::class.java))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, BluetoothService::class.java))
            ctx.startService(Intent(ctx, SensorService::class.java))
        } else {
            ctx.startService(Intent(ctx, BluetoothService::class.java))
            ctx.startService(Intent(ctx, SensorService::class.java))
        }
    }

    @ReactMethod
    fun startUserIntentService(userData: String) {
        Log.d("TEST", "user data : $userData")
        val intent = Intent(reactApplicationContext, UserIntent::class.java).apply {
            putExtra("user", userData)
        }
        reactApplicationContext.startService(intent)
    }

    @ReactMethod
    fun AppRestartIntent(userData: String) {
        Log.d("TEST", "user data : $userData")
        val intent = Intent(reactApplicationContext, UserIntent::class.java).apply {
            putExtra("user", userData)
        }
        reactApplicationContext.startService(intent)
    }

    /* =========================
       ✅ 센서 테스트 관련
       ========================= */


    @ReactMethod
    fun ServiceCheck(promise: Promise) {
        val ctx: Context = reactApplicationContext

        var bleRunning = BluetoothService.runningInstance != null
        val serviceFlag = App.instance.isServiceFlag

        if (serviceFlag && !bleRunning) {
            Log.d("SERVICE_CHECK", "serviceFlag=true 이지만 서비스 미실행 → 재시작")
            val restartIntent = Intent(ctx, BluetoothService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(ctx, restartIntent)
            } else {
                ctx.startService(restartIntent)
            }
            bleRunning = true
        }

        val openLobbyFlag = App.instance.isPassOpenLobbyFlag
        Log.d("SERVICE_CHECK", "bleRunning=$bleRunning serviceFlag=$serviceFlag lobbyFlag=$openLobbyFlag")

        val map = Arguments.createMap().apply {
            putBoolean("Ble", bleRunning)
            putBoolean("Lobby", openLobbyFlag)
        }
        promise.resolve(map)
    }

    @ReactMethod
    fun ServiceFlag(flag: Boolean) {
        val ctx: Context = reactApplicationContext

        if (flag) {
            Log.d("SERVICE_FLAG", "서비스 시작 요청")
            App.instance.isServiceFlag = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(ctx, Intent(ctx, BluetoothService::class.java))
                ctx.startService(Intent(ctx, SensorService::class.java))
            } else {
                ctx.startService(Intent(ctx, BluetoothService::class.java))
                ctx.startService(Intent(ctx, SensorService::class.java))
            }
        } else {
            App.instance.isServiceFlag = false
            App.instance.isExplicitlyStopped = true
            Log.d("SERVICE_FLAG", "서비스 중지 요청")
            ctx.stopService(Intent(ctx, BluetoothService::class.java))
            ctx.stopService(Intent(ctx, SensorService::class.java))
        }
    }

    @ReactMethod
    fun requestAllPrimaryPermissions() {
        val activity = getCurrentActivity() ?: return
        if (PermissionManager.hasAllPrimaryPermissions(activity)) return
        PermissionManager.requestRuntimePermissions(activity)
    }

    @ReactMethod
    fun hasAllPrimaryPermissions(promise: Promise) {
        // 1. getCurrentActivity()를 호출하고, null이면 즉시 false를 반환합니다.
        val activity = getCurrentActivity()

        if (activity == null) {
            Log.d("AndroidModule", "hasAllPrimaryPermissions: Activity is null")
            promise.resolve(false)
            return
        }

        // 2. 여기서 activity는 절대 null이 아님이 보장되지만,
        // 컴파일러에게 명확히 알리기 위해 'activity!!' 또는 타입을 명시합니다.
        try {
            val ok = PermissionManager.hasAllPrimaryPermissions(activity)
            promise.resolve(ok)
        } catch (e: Exception) {
            promise.reject("PERM_CHECK_ERROR", e.message)
        }
    }

    @ReactMethod
    fun passiveParking(parkingCar: String) {
        val app = App.instance
        app.isPassiveCheck = true
        app.parkingCar = parkingCar

        // 💡 데이터 초기화: 이전 수집 기록 삭제
        synchronized(app.mAccelBeaconMap) {
            app.mAccelBeaconMap.clear()
        }
        app.resetDelayList()

        // 자동 주차위치 서비스(WholeTimer) 완전 중지 및 초기화
        BluetoothService.getInstance()?.beaconTimers?.cancelWholeTimer()
        SensorService.getInstance()?.resetSensorState()

        Log.d("Passive", "🚀 수동 주차 모드 시작: 차량번호 $parkingCar")
    }

    @ReactMethod
    fun passiveParkingEnd(promise: Promise) {
        try {
            App.instance.isPassiveCheck = false
            App.instance.isParkingStartFlag = true

            val ctx: Context = reactApplicationContext
            // 💡 이제 결과값은 AccelBeacon 객체가 아니라 String(비컨 ID)입니다.
            val finalBeaconId = PassiveParkingService.getInstance(ctx).parkingEnd()

            if (finalBeaconId == null) {
                promise.resolve(null)
                return
            }

            // JS로 전달할 맵 구성
            val map = Arguments.createMap().apply {
                // finalBeaconId 자체가 ID이므로 그대로 넣습니다.
                putString("beaconId", finalBeaconId)
                // 삼각측량 결과에서는 단일 RSSI 의미가 없으므로 0이나 적당한 값을 넣거나 제외합니다.
                putInt("rssi", 0)
            }

            Log.d("Passive" ,"수동주차위치 결과 map : $map")
            promise.resolve(map)

            // 데이터 초기화
            App.instance.mAccelBeaconMap.clear()
            App.instance.resetDelayList()

            // 자동 주차위치 서비스(WholeTimer) 초기화 후 재시작
            BluetoothService.getInstance()?.beaconTimers?.startWholeTimer()
            Log.d("Passive", "✅ 수동 주차 완료 → WholeTimer 재시작")
        } catch (e: Exception) {
            promise.reject("PASSIVE_PARKING_END_ERROR", e)
        }
    }
    @ReactMethod
    fun CheckPermissionsStatus(promise: Promise) {
        try {
            val ctx: Context = reactApplicationContext
            val allGranted = PermissionManager.hasAllPermissions(ctx)
            promise.resolve(allGranted)
        } catch (e: Exception) {
            promise.reject("CHECK_PERMISSION_ERROR", e.message)
        }
    }

    @ReactMethod
    fun passOpenLobbyFlag(flag: Boolean) {
        Log.d("AndroidModule", "passOpenLobbyFlag : $flag → 스캔 재시작")
        App.instance.isPassOpenLobbyFlag = flag
        // 플래그 변경 시 스캔 재시작 (멈춘 상태에서 복구 포함)
        handler.post {
            val service = BluetoothService.getInstance()
            if (service != null) {
                service.resetAndRestartScanning()
            } else {
                // 서비스 자체가 죽어있으면 강제 재시작
                val ctx: Context = reactApplicationContext
                val intent = Intent(ctx, BluetoothService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    androidx.core.content.ContextCompat.startForegroundService(ctx, intent)
                } else {
                    ctx.startService(intent)
                }
            }
        }
    }

    @ReactMethod
    fun PermissionCheck(promise: Promise) {
        val ctx: Context = reactApplicationContext
        val a = getCurrentActivity()

        if (a == null) {
            promise.reject("ERROR", "Activity is null")
            return
        }

        if (!PermissionManager.hasAllPermissions(ctx)) {
            val perms = PermissionManager.runtimePermissions
            val paa = a as? PermissionAwareActivity

            paa?.requestPermissions(perms, PermissionManager.REQUEST_CODE_ALL, object : PermissionListener {
                override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
                    if (!PermissionManager.hasAllPermissions(ctx)) {
                        promise.resolve(false)
                        return true
                    }
                    if (!PermissionManager.isIgnoringBatteryOptimizations(ctx)) {
                        Log.d("AndroidModule", "배터리 최적화 제외 설정이 필요합니다.")
                        PermissionManager.requestIgnoreBatteryOptimizations(ctx)
                        promise.resolve(false)
                        return true
                    }
                    if (!PermissionManager.isBluetoothEnabled(ctx)) {
                        Log.d("AndroidModule", "블루투스 활성화가 필요합니다.")
                        PermissionManager.requestBluetoothEnable(a)
                        promise.resolve(false)
                    } else {
                        promise.resolve(true)
                    }
                    return true
                }
            })
            return
        }

        if (!PermissionManager.isIgnoringBatteryOptimizations(ctx)) {
            Log.d("AndroidModule", "배터리 최적화 제외 설정이 필요합니다.")
            PermissionManager.requestIgnoreBatteryOptimizations(ctx)
            promise.resolve(false)
            return
        }

        if (!PermissionManager.isBluetoothEnabled(ctx)) {
            Log.d("AndroidModule", "블루투스 활성화가 필요합니다.")
            PermissionManager.requestBluetoothEnable(a)
            promise.resolve(false)
        } else {
            promise.resolve(true)
        }
    }

    companion object {
        private const val TAG_TEST = "SensorTest"
    }
}