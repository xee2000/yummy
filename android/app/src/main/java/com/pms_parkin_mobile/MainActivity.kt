package com.pms_parkin_mobile

import android.os.Build
import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import com.pms_parkin_mobile.util.PermissionManager

class MainActivity : ReactActivity() {

    override fun getMainComponentName(): String = "pms_parkin_mobile"

    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

    private var permissionFlowRunning = false
    private var askedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UI가 붙은 직후 한 틱 뒤에 실행: 최초 설치 직후에도 안정적으로 다이얼로그가 뜸
        window?.decorView?.post { startPermissionFlow() }
    }

    override fun onResume() {
        super.onResume()
        // 앱 재진입 시에도(설정 다녀온 뒤 등) 한 번만 시도
        startPermissionFlow()
    }

    private fun startPermissionFlow() {
        if (permissionFlowRunning || askedOnce) return
        permissionFlowRunning = true

        val ctx = this // Activity context

        // --- 권한 필요 여부 개별 판정 ---
        // 알림: Android 13+에서만 런타임 권한
        val needPostNotifications =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !PermissionManager.hasPostNotifications(ctx)

        // 위치: 6.0+ 모든 버전에서 런타임 권한
        val needLocation =
            !PermissionManager.hasLocationPermissions(ctx)

        if (needPostNotifications || needLocation) {
            PermissionManager.requestRuntimePermissions(this)
            // 결과는 onRequestPermissionsResult에서 마무리
            return
        }
        // (선택) 배터리 최적화 무시 요청 (Doze 예외)
        PermissionManager.requestIgnoreBatteryOptimization(ctx)

        askedOnce = true
        permissionFlowRunning = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 한 번만 시도하고 종료
        askedOnce = true
        permissionFlowRunning = false
    }
}