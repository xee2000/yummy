package com.pms_parkin_mobile

import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

    override fun getMainComponentName(): String = "pms_parkin_mobile"

    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

    override fun onCreate(savedInstanceState: Bundle?) {
        // react-native-screens: Fragment 복원을 막기 위해 null 전달
        // https://github.com/software-mansion/react-native-screens/issues/17
        super.onCreate(null)
    }

    override fun onResume() {
        super.onResume()
        // ✅ 권한 요청/흐름은 RN에서만 처리 (MainActivity에서는 아무것도 안함)
    }

    // ✅ onRequestPermissionsResult도 필요 없음 (PermissionAwareActivity로 RN 모듈에서 처리)
}