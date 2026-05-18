package com.pms_parkin_mobile.service

import android.content.Context
import android.util.Log
import com.pms_parkin_mobile.api.RestController
import kotlin.math.pow

class PassiveParkingService private constructor() {

    /** parkingEnd() 최종 반환 타입: 추정 좌표 + 서버가 매칭한 비컨 ID */
    data class ParkingResult(
        val beaconId: String,  // 서버에서 받은 비컨 ID
        val x: Double,         // 가중치 중심 X 좌표 (추정)
        val y: Double          // 가중치 중심 Y 좌표 (추정)
    )

    // -----------------------------------------------------------------------
    // 거리 계산
    // -----------------------------------------------------------------------
    private fun calculateDistance(rssi: Double): Double {
        val txPower = -62.0
        val n = 2.5
        return 10.0.pow((txPower - rssi) / (10 * n))
    }

    // -----------------------------------------------------------------------
    // 가중치 좌표 계산 → RestController 로 x, y 전송 → 비컨 콜백 반환
    // -----------------------------------------------------------------------
    fun parkingEnd(onResult: (ParkingResult?) -> Unit) {
        val beaconMap = App.instance!!.mAccelBeaconMap

        Log.i(TAG, "==========================================================")
        Log.i(TAG, "🚀 [수동 주차 전수 조사 시작] 총 후보 비컨 수: ${beaconMap.size}")
        Log.i(TAG, "==========================================================")

        if (beaconMap.isEmpty()) {
            onResult(null)
            return
        }

        var totalWeight    = 0.0
        var weightedSumX   = 0.0
        var weightedSumY   = 0.0
        var validBeaconCount = 0

        Log.d(TAG, "Step 1. [개별 비컨별 RSSI 이력 및 가중치 상세 분석]")

        for (entry in beaconMap) {
            val keyFromMap = entry.key.trim().uppercase()
            val beacon     = entry.value

            val delayList   = beacon.delayList ?: continue
            val rssiHistory = delayList.mapNotNull { it?.split("_")?.getOrNull(0)?.toDoubleOrNull() }
            if (rssiHistory.isEmpty()) continue

            // 비컨 좌표는 서버에서 관리하므로, RSSI/거리 기반 가중치만 계산
            val avgRssi  = rssiHistory.average()
            val distance = calculateDistance(avgRssi)
            val weight   = 1.0 / (distance.coerceAtLeast(0.5).pow(2.0))

            // ※ 실제 x·y 좌표는 서버에서 반환받으므로 여기서는 RSSI 기반 가중치 누적만 수행
            weightedSumX += distance * weight   // 거리를 임시 x 축으로 활용 (서버에서 재계산)
            weightedSumY += 0.0
            totalWeight  += weight
            validBeaconCount++

            Log.v(TAG, "--------------------------------------------------")
            Log.v(TAG, "📡 ID: $keyFromMap")
            Log.v(TAG, "   └─ [RSSI 전체 이력]: $rssiHistory")
            Log.v(TAG, "   └─ [통계]: 평균 %.2f dBm (${rssiHistory.size}개)".format(avgRssi))
            Log.v(TAG, "   └─ [추론]: 거리 %.2fm | 가중치 %.6f".format(distance, weight))
        }

        if (validBeaconCount == 0) {
            Log.e(TAG, "❌ 분석 실패: 처리 가능한 비컨이 없습니다.")
            onResult(null)
            return
        }

        // 가중 평균 좌표 도출
        val finalX = weightedSumX / totalWeight
        val finalY = weightedSumY / totalWeight

        Log.d(TAG, "==========================================================")
        Log.d(TAG, "Step 2. [가중치 중심점 도출 결과]")
        Log.i(TAG, "   - 분석 활용 비컨 : $validBeaconCount 개")
        Log.i(TAG, "   - 최종 추정 좌표 : (%.2f, %.2f)".format(finalX, finalY))
        Log.i(TAG, "   - 서버로 좌표 전송 중...")

        // RestController 에 x, y 전송 → 범위 내 비컨 1개 수신
        RestController.instance.findBeaconByCoords(finalX, finalY) { response ->
            if (response == null) {
                Log.e(TAG, "❌ 서버에서 비컨 정보를 받아오지 못했습니다.")
                onResult(null)
                return@findBeaconByCoords
            }

            Log.i(TAG, "🏆 [서버 매칭 결과]")
            Log.i(TAG, "   📍 비컨 ID : ${response.beaconId}")
            Log.i(TAG, "   📍 서버 좌표: (${response.x}, ${response.y})")
            Log.i(TAG, "==========================================================")

            onResult(
                ParkingResult(
                    beaconId = response.beaconId,
                    x = response.x ?: finalX,
                    y = response.y ?: finalY
                )
            )
        }
    }

    companion object {
        private const val TAG = "PassiveParkingService"

        @Volatile
        private var instance: PassiveParkingService? = null

        @Synchronized
        fun getInstance(context: Context): PassiveParkingService {
            return instance ?: PassiveParkingService().also { instance = it }
        }
    }
}
