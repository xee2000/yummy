package com.pms_parkin_mobile.service

import android.content.Context
import android.util.Log
import com.pms_parkin_mobile.dto.AccelBeacon
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.pow
import kotlin.math.sqrt

class PassiveParkingService private constructor(context: Context) {

    private val beaconCoords = mutableMapOf<String, BeaconLocation>()
    private val CSV_PATH = "excel/bansuk.csv"

    data class BeaconLocation(val serial: String, val x: Double, val y: Double)

    // ✅ 반환 타입: 가중치 좌표 + 가장 가까운 비컨 ID
    data class ParkingResult(
        val beaconId: String,   // CSV에서 가장 가까운 비컨 ID
        val x: Double,          // 가중치 중심 X 좌표 (추정)
        val y: Double,          // 가중치 중심 Y 좌표 (추정)
        val beaconX: Double,    // 매칭된 비컨의 CSV X 좌표
        val beaconY: Double     // 매칭된 비컨의 CSV Y 좌표
    )

    init {
        Log.d(TAG, "PassiveParkingService created")
        loadBeaconCoordinates(context)
    }

    private fun loadBeaconCoordinates(context: Context) {
        try {
            val inputStream = context.assets.open(CSV_PATH)
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    val tokens = line.split(",")
                    if (tokens.size >= 4) {
                        val serial = tokens[0].replace("\uFEFF", "").replace("\"", "").trim().padStart(4, '0').uppercase()
                        val x = tokens[2].trim().toDoubleOrNull() ?: 0.0
                        val y = tokens[3].trim().toDoubleOrNull() ?: 0.0
                        beaconCoords[serial] = BeaconLocation(serial, x, y)
                    }
                }
            }
            Log.i(TAG, "✅ CSV 로드 완료: ${beaconCoords.size}개 매핑됨.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CSV 읽기 실패: ${e.message}")
        }
    }

    private fun calculateDistance(rssi: Double): Double {
        val txPower = -62.0
        val n = 2.5
        return 10.0.pow((txPower - rssi) / (10 * n))
    }

    // ✅ 반환 타입을 ParkingResult?로 변경
    fun parkingEnd(): ParkingResult? {
        val beaconMap = App.instance!!.mAccelBeaconMap

        Log.i(TAG, "==========================================================")
        Log.i(TAG, "🚀 [수동 주차 전수 조사 시작] 총 후보 비컨 수: ${beaconMap.size}")
        Log.i(TAG, "==========================================================")

        if (beaconMap.isEmpty()) return null

        var totalWeight = 0.0
        var weightedSumX = 0.0
        var weightedSumY = 0.0
        var validBeaconCount = 0

        Log.d(TAG, "Step 1. [개별 비컨별 RSSI 이력 및 가중치 상세 분석]")

        for (entry in beaconMap) {
            val keyFromMap = entry.key.trim().uppercase()
            val beacon = entry.value
            val coord = beaconCoords[keyFromMap]

            if (coord == null) {
                Log.w(TAG, "   [-] 제외: ID [$keyFromMap]는 CSV에 좌표 정보가 없습니다.")
                continue
            }

            val delayList = beacon.delayList ?: continue
            val rssiHistory = delayList.mapNotNull { it?.split("_")?.getOrNull(0)?.toDoubleOrNull() }
            if (rssiHistory.isEmpty()) continue

            val avgRssi = rssiHistory.average()
            val distance = calculateDistance(avgRssi)
            val weight = 1.0 / (distance.coerceAtLeast(0.5).pow(2.0))

            weightedSumX += coord.x * weight
            weightedSumY += coord.y * weight
            totalWeight += weight
            validBeaconCount++

            Log.v(TAG, "--------------------------------------------------")
            Log.v(TAG, "📡 ID: $keyFromMap (Raw좌표: ${coord.x}, ${coord.y})")
            Log.v(TAG, "   └─ [RSSI 전체 이력]: $rssiHistory")
            Log.v(TAG, "   └─ [통계]: 평균 %.2f dBm (${rssiHistory.size}개)".format(avgRssi))
            Log.v(TAG, "   └─ [추론]: 거리 %.2fm | 가중치 %.6f".format(distance, weight))
        }

        if (validBeaconCount == 0) {
            Log.e(TAG, "❌ 분석 실패: 매핑 가능한 비컨이 없습니다.")
            return null
        }

        // Step 2. 가중 평균 좌표 도출
        val finalRawX = weightedSumX / totalWeight
        val finalRawY = weightedSumY / totalWeight

        Log.d(TAG, "==========================================================")
        Log.d(TAG, "Step 2. [가중치 중심점 도출 결과]")
        Log.i(TAG, "   - 분석 활용 비컨 : $validBeaconCount 개")
        Log.i(TAG, "   - 최종 추정 좌표 : (%.2f, %.2f)".format(finalRawX, finalRawY))

        // Step 3. 가중치 좌표 기준 최근접 비컨 매칭
        var finalBeaconId: String? = null
        var finalBeaconCoord: BeaconLocation? = null
        var minDistance = Double.MAX_VALUE

        for ((serial, location) in beaconCoords) {
            val d = sqrt((finalRawX - location.x).pow(2.0) + (finalRawY - location.y).pow(2.0))
            if (d < minDistance) {
                minDistance = d
                finalBeaconId = serial
                finalBeaconCoord = location
            }
        }

        Log.i(TAG, "----------------------------------------------------------")
        Log.i(TAG, "🏆 [최종 추론 결론]")
        Log.i(TAG, "   📍 결정된 비컨 ID : $finalBeaconId")
        Log.i(TAG, "   📍 가중치 좌표    : (%.2f, %.2f)".format(finalRawX, finalRawY))
        Log.i(TAG, "   📍 비컨 좌표      : (%.2f, %.2f)".format(finalBeaconCoord?.x, finalBeaconCoord?.y))
        Log.i(TAG, "   📍 비컨-추정지 오차 : %.2f".format(minDistance))
        Log.i(TAG, "==========================================================")

        return if (finalBeaconId != null && finalBeaconCoord != null) {
            ParkingResult(
                beaconId = finalBeaconId,
                x = finalRawX,
                y = finalRawY,
                beaconX = finalBeaconCoord.x,
                beaconY = finalBeaconCoord.y
            )
        } else null
    }

    fun getBeaconCoords(id: String): BeaconLocation? {
        return beaconCoords[id.trim().padStart(4, '0').uppercase()]
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