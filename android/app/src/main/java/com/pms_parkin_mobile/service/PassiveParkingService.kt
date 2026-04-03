package com.pms_parkin_mobile.service

import android.content.Context
import android.util.Log
import com.pms_parkin_mobile.dto.AccelBeacon
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.pow

class PassiveParkingService private constructor(context: Context) {

    // 비컨 좌표 데이터를 저장할 맵 (Key: 4자리 Serial, Value: 좌표 객체)
    private val beaconCoords = mutableMapOf<String, BeaconLocation>()

    data class BeaconLocation(val serial: String, val x: Double, val y: Double)

    init {
        Log.d(TAG, "PassiveParkingService created")
        // 시스템 시작 시 CSV 파일 로드
        loadBeaconCoordinates(context)
    }

    /**
     * res/raw/bansuk.csv 파일을 읽어 좌표 맵을 구성합니다.
     * serial_number가 3자리인 경우 앞에 '0'을 붙여 4자리로 정규화합니다.
     */
    private fun loadBeaconCoordinates(context: Context) {
        try {
            val inputStream = context.assets.open("excel/bansuk.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))

            reader.useLines { lines ->
                lines.drop(1).forEach { line -> // 헤더 스킵
                    val tokens = line.split(",")
                    if (tokens.size >= 4) {
                        // 3자리 시리얼을 4자리로 보정 (예: 0D3 -> 00D3)
                        val serial = tokens[0].trim().padStart(4, '0').uppercase()
                        val x = tokens[2].trim().toDoubleOrNull() ?: 0.0
                        val y = tokens[3].trim().toDoubleOrNull() ?: 0.0

                        beaconCoords[serial] = BeaconLocation(serial, x, y)
                    }
                }
            }
            Log.i(TAG, "✅ CSV 좌표 로드 완료: ${beaconCoords.size}개 매핑")
        } catch (e: Exception) {
            Log.e(TAG, "❌ CSV 파싱 중 오류 발생: ${e.message}")
        }
    }

    /**
     * RSSI(dBm) 값을 거리(m)로 변환합니다.
     * txPower: 1미터 거리에서의 평균 RSSI (현장 실측에 따라 -59 ~ -65 사이 조정 필요)
     * n: 환경 감쇄 계수 (실내 주차장 기준 보통 2.0 ~ 3.0)
     */
    private fun calculateDistance(rssi: Double): Double {
        val txPower = -62.0
        val n = 2.5
        return 10.0.pow((txPower - rssi) / (10 * n))
    }

    /**
     * 수집된 모든 비컨 데이터를 활용하여 사용자의 예상 좌표(X, Y)를 산출합니다.
     * RSSI가 약한 비컨도 거리의 역제곱 가중치를 활용하여 계산에 포함합니다.
     */
    /**
     * 삼각측량으로 계산된 좌표와 가장 가까운 실제 비컨 ID를 찾아 반환합니다.
     */
    fun parkingEnd(): String? {
        val beaconMap = App.instance!!.mAccelBeaconMap

        if (beaconMap.isEmpty()) return null

        var totalWeight = 0.0
        var weightedSumX = 0.0
        var weightedSumY = 0.0
        var validBeaconCount = 0

        // 1. 모든 수집 데이터를 활용해 추정 좌표(Centroid) 계산
        for (entry in beaconMap) {
            val beacon = entry.value
            val serial = beacon.minor?.toString()?.padStart(4, '0')?.uppercase() ?: continue
            val coord = beaconCoords[serial] ?: continue

            val delayList = beacon.delayList ?: continue
            val avgRssi = delayList.mapNotNull { it?.split("_")?.getOrNull(0)?.toDoubleOrNull() }.average()

            if (avgRssi.isNaN()) continue

            val distance = calculateDistance(avgRssi)
            val weight = 1.0 / (distance.coerceAtLeast(0.5).pow(2))

            weightedSumX += coord.x * weight
            weightedSumY += coord.y * weight
            totalWeight += weight
            validBeaconCount++
        }

        if (validBeaconCount == 0) return null

        // 추정 위치 X, Y
        val finalX = weightedSumX / totalWeight
        val finalY = weightedSumY / totalWeight

        // 2. 🔥 [핵심 추가] 계산된 좌표에서 가장 가까운 실제 비컨 ID 매칭
        var finalBeaconId: String? = null
        var minDistance = Double.MAX_VALUE

        for ((serial, location) in beaconCoords) {
            // 피타고라스 정리를 이용한 거리 계산: sqrt((x1-x2)^2 + (y1-y2)^2)
            val dist = Math.sqrt(
                (finalX - location.x).pow(2.0) + (finalY - location.y).pow(2.0)
            )

            if (dist < minDistance) {
                minDistance = dist
                finalBeaconId = serial
            }
        }

        Log.i(TAG, "==============================================")
        Log.i(TAG, "🏆 [최종 분석 완료]")
        Log.i(TAG, "   📍 추정 좌표: X=%.2f, Y=%.2f".format(finalX, finalY))
        Log.i(TAG, "   📍 최종 매칭 비컨 ID: $finalBeaconId (오차 거리: %.2f)".format(minDistance))
        Log.i(TAG, "==============================================")

        return finalBeaconId
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