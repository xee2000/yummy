package com.pms_parkin_mobile

import android.os.Build
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate
import com.pms_parkin_mobile.util.PermissionManager
// 필요 시 알림 설정 체크하려면 주석 해제
// import androidx.core.app.NotificationManagerCompat

class MainActivity : ReactActivity() {

    // RN 메인 컴포넌트 이름 (JS의 AppRegistry.registerComponent 이름과 동일)
    override fun getMainComponentName(): String = "pms_parkin_mobile"

    // RN 기본 델리게이트
    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

    private var permissionFlowRunning = false
    private var askedOnce = false

    override fun onResume() {
        super.onResume()

        if (permissionFlowRunning || askedOnce) return
        permissionFlowRunning = true

        val ctx = applicationContext

        // Android 13+ 에서만 알림 런타임 권한 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionManager.hasAllPermissions(ctx)) {
            PermissionManager.requestAllPermissions(this)
            // 결과는 onRequestPermissionsResult에서 마무리
            return
        }

        // (선택) 전역 알림이 꺼져있으면 설정으로 유도하려면 주석 해제
        // if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
        //   PermissionManager.openNotificationSettings(this)
        //   permissionFlowRunning = false
        //   askedOnce = true
        //   return
        // }

        // (선택) 배터리 최적화 무시 요청
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