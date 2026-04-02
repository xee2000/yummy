package com.pms_parkin_mobile.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.pms_parkin_mobile.dto.AccelBeacon

class PassiveParkingService private constructor(context: Context) {
    private val sensorManager: SensorManager?
    private val accelerometer: Sensor?

    init {
        val appCtx = context.applicationContext
        sensorManager = appCtx.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        Log.d(TAG, "PassiveParkingService created")
    }



    /**
     * 수집 종료 및 평균 RSSI 기준 최적 비컨 추출
     */
    /**
     * 수집 종료 및 평균 RSSI 기준 최적 비컨 추출
     */
    fun parkingEnd(): AccelBeacon? {
        val beaconMap = App.instance!!.mAccelBeaconMap

        Log.i(TAG, "==============================================")
        Log.i(TAG, "📡 [수동 주차 분석 시작] 총 후보 비컨 수: ${beaconMap.size}")
        Log.i(TAG, "==============================================")

        if (beaconMap.isEmpty()) {
            Log.e(TAG, "❌ 분석 실패: 수집된 비컨 데이터가 전혀 없습니다.")
            return null
        }

        var strongestBeacon: AccelBeacon? = null
        var maxScore = -Double.MAX_VALUE

        for (entry in beaconMap) {
            val beacon = entry.value
            val delayList = beacon.delayList

            Log.d(TAG, "🔍 비컨 검사 중 -> ID: ${beacon.beaconId}, Minor: ${beacon.minor})")

            if (delayList.isNullOrEmpty()) {
                Log.w(TAG, "   └─ [건너뜀] 수집된 RSSI 레코드가 없습니다.")
                continue
            }

            // Minor 33000대 필터링
            val minorValue = beacon.minor?.toString()?.toIntOrNull() ?: 0
            if (minorValue > 33000) {
                Log.w(TAG, "   └─ [건너뜀] Minor 33000대 노이즈 비컨 제외")
                continue
            }

            var rssiSum = 0.0
            var validCount = 0
            val rssiDetails = StringBuilder()

            for (record in delayList) {
                val split = record?.split("_")
                val rawRssi = split?.getOrNull(0)?.toDoubleOrNull()
                val delay = split?.getOrNull(1) ?: "?"

                if (rawRssi != null) {
                    rssiSum += rawRssi
                    validCount++
                    rssiDetails.append("[$rawRssi dBm (T:${delay}s)] ")
                }
            }

            if (validCount == 0) {
                Log.w(TAG, "   └─ [건너뜀] 유효한 RSSI 값이 없습니다.")
                continue
            }

            val averageRssi = rssiSum / validCount

            Log.v(TAG, "   └─ 데이터 목록: $rssiDetails")
            Log.d(TAG, "   └─ 중간 산출: 합계 ${rssiSum.toInt()}, 횟수 $validCount -> 평균 %.2f dBm".format(averageRssi))

            // 가중치 점수 계산
            // 데이터 수에 따라 보정값 부여
            val countBonus = when {
                validCount >= 3 -> 10.0   // 3개 이상 +10점
                validCount == 2 -> 5.0    // 2개 +5점
                else -> -10.0             // 1개 -10점 패널티
            }
            val score = averageRssi + countBonus

            Log.d(TAG, "   └─ 가중치 점수: ${"%.2f".format(score)} (평균 ${"%.2f".format(averageRssi)} + 보정 $countBonus)")

            if (strongestBeacon == null || score > maxScore) {
                if (strongestBeacon != null) {
                    Log.v(TAG, "   ⭐ [갱신] 기존 최고점(${maxScore.toInt()})보다 높음 -> 새로운 1위 후보!")
                } else {
                    Log.v(TAG, "   ⭐ [최초 선정] 첫 번째 유효 후보 등록")
                }
                maxScore = score
                strongestBeacon = beacon
            }
            Log.d(TAG, "----------------------------------------------")
        }

        Log.i(TAG, "==============================================")
        if (strongestBeacon != null) {
            Log.i(TAG, "🏆 [최종 분석 완료]")
            Log.i(TAG, "   📍 결정된 비컨 ID : ${strongestBeacon.beaconId}")
            Log.i(TAG, "   📍 최종 가중치 점수 : %.2f".format(maxScore))
            Log.i(TAG, "   📍 수신 데이터 수 : ${strongestBeacon.delayList?.size ?: 0}개")
            strongestBeacon.rssi = maxScore.toInt().toString()
        } else {
            Log.e(TAG, "❌ [분석 종료] 적합한 비컨을 찾지 못했습니다.")
        }
        Log.i(TAG, "==============================================")

        return strongestBeacon
    }
    companion object {
        private const val TAG = "PassiveParkingService"
        private var instance: PassiveParkingService? = null

        @Synchronized
        fun getInstance(context: Context): PassiveParkingService {
            if (instance == null) {
                instance = PassiveParkingService(context)
            }
            return instance!!
        }
    }
}