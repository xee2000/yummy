package com.pms_parkin_mobile.data

import android.util.Log
import com.woorisystem.domain.AccelSensorData
import com.woorisystem.domain.AccelSensorData2
import com.woorisystem.domain.GyroSensorData
import com.woorisystem.domain.GyroSensorData2

/**
 * SensorDataStore
 *
 * SensorService가 수집하는 모든 센서 데이터를 보관하는 저장소.
 *
 * 기존 DataManagerSingleton에 산재되어 있던 센서 관련 필드들을
 * 별도 클래스로 분리하여 책임을 명확하게 함.
 *
 * -- 보관 데이터 ------------------------------------------------------------------
 *   gyroList      : 자이로 (표준 모드 / bigData 모드 raw) → Total.gyroList
 *   gyroList2     : 자이로 (bigData 모드 회전 카운트)      → Total.gyroList2
 *   accelList     : 가속도 T/S/W 상태                     → Total.sensorList
 *   accelList2    : 가속도 실측 카운트                     → Total.sensorList2
 *
 * -- 수치 상태 -------------------------------------------------------------------
 *   accelCount    : 1초 내 CVA 임계값 초과 횟수 (AccelTimer가 1초마다 읽고 리셋)
 *   accelSequence : AccelSensorData seq 번호 (AccelTimer가 관리)
 *   saveCountRoll / Pitch / Yaw : 회전 종료 시 기록된 최대 카운트
 */
class SensorDataStore {

    // -- 자이로 -----------------------------------------------------------------
    private val _gyroList  = mutableListOf<GyroSensorData>()
    private val _gyroList2 = mutableListOf<GyroSensorData2>()

    val gyroList:  List<GyroSensorData>  get() = _gyroList.toList()
    val gyroList2: List<GyroSensorData2> get() = _gyroList2.toList()

    fun addGyroSensorData(data: GyroSensorData)   { _gyroList.add(data) }
    fun addGyroSensorData2(data: GyroSensorData2) { _gyroList2.add(data) }

    // -- 가속도 T/S/W -----------------------------------------------------------
    private val _accelList  = mutableListOf<AccelSensorData>()
    private val _accelList2 = mutableListOf<AccelSensorData2>()

    val accelList:  List<AccelSensorData>  get() = _accelList.toList()
    val accelList2: List<AccelSensorData2> get() = _accelList2.toList()

    fun addAccelSensorData(data: AccelSensorData)   { _accelList.add(data) }
    fun addAccelSensorData2(data: AccelSensorData2) {
        Log.d("TEST" ," data : " + data)
        _accelList2.add(data)
    }

    // -- 가속도 카운터 (AccelTimer용) --------------------------------------------
    @Volatile var accelCount:    Int = 0; private set
    @Volatile var accelSequence: Int = 0

    fun incrementAccelCount() { accelCount++ }
    fun resetAccelCount()     { accelCount = 0 }

    // -- 자이로 회전 카운트 저장값 (SaveGyro용) ------------------------------------------
    @Volatile var saveCountRoll:  Int = 0
    @Volatile var saveCountPitch: Int = 0
    @Volatile var saveCountYaw:   Int = 0

    // -- 전체 초기화 (WholeTimer 시작 시 호출) -----------------------------------------
    fun reset() {
        _gyroList.clear()
        _gyroList2.clear()
        _accelList.clear()
        _accelList2.clear()
        accelCount    = 0
        accelSequence = 0
        saveCountRoll  = 0
        saveCountPitch = 0
        saveCountYaw   = 0
    }

    companion object {
        @Volatile
        private var INSTANCE: SensorDataStore? = null

        val instance: SensorDataStore
            get() = INSTANCE ?: synchronized(this) {
                INSTANCE ?: SensorDataStore().also { INSTANCE = it }
            }
    }
}
