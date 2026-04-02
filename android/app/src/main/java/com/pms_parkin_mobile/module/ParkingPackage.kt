package com.pms_parkin_mobile.module

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class ParkingPackage : ReactPackage {

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        // listOf()를 사용하여 리스트 생성과 동시에 모듈을 추가하여 반환합니다.
        return listOf(AndroidModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        // 자바의 Collections.emptyList() 대신 코틀린의 내장 함수인 emptyList()를 사용합니다.
        // ViewManager에 제네릭 <*, *>을 붙여주면 경고창 없이 깔끔하게 컴파일됩니다.
        return emptyList()
    }
}