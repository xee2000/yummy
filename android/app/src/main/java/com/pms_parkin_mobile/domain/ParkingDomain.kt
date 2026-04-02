package com.woorisystem.domain

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.LinkedHashSet

/**
 * ParkingDomain.kt
 *
 * 주차위치 인식 서버 전송용 도메인 클래스 모음
 * 기존 Java 클래스(Total, AccelBeacon, Beacon) → Kotlin data class 통합
 *
 * SerializedName은 서버 API 필드명과 동일하게 유지
 */

// ─────────────────────────────────────────────────────────────────────────────
// Total — ParkingComplete API 전송 최상위 객체
// 기존 Total.java 대체
// ─────────────────────────────────────────────────────────────────────────────
@Keep
data class ParkingTotal(
    /** 입차 시각 문자열 */
    @SerializedName("InputDate") @Expose
    val inputDate: String = "",

    /** 가속도 T/S/W 상태 목록 */
    @SerializedName("Sensors") @Expose
    val sensorList: List<AccelSensorData> = emptyList(),

    /** 가속도 실측 카운트 목록 */
    @SerializedName("Sensors2") @Expose
    val sensorList2: List<AccelSensorData2> = emptyList(),

    /** 주차면 변화 비콘 목록 (Major=5) */
    @SerializedName("Beacons") @Expose
    val beaconList: List<ParkingBeaconData> = emptyList(),

    /** 자이로 데이터 목록 (표준 모드 / bigData raw) */
    @SerializedName("Gyros") @Expose
    val gyroList: List<GyroSensorData> = emptyList(),

    /** 자이로 데이터 목록 2 (bigData 모드 회전 카운트) */
    @SerializedName("Gyros2") @Expose
    var gyroList2: List<GyroSensorData2>? = null,

    /** 주차면 비콘 RSSI 최댓값 목록 (Major=4) */
    @SerializedName("AccelBeacons") @Expose
    val accelBeaconList: List<AccelBeaconData> = emptyList(),

    /** 주차 상태 판별값 ("ANDROID", "ANDROID-end", "ANDROID-lobby", "ANDROID-out" 등) */
    @SerializedName("ParingState") @Expose
    val paringState: String = "non-paring",

    /**
     * bigDataSend 모드 전용: Total JSON을 Gzip 압축한 바이트 배열
     * Gson 직렬화 시 포함되지만, 압축 전 gyroList2를 null로 비웠다가
     * 압축 후 다시 채우는 방식으로 사용 (원본 동일)
     */
    @SerializedName("file") @Expose
    var file: ByteArray? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// ParkingBeaconData — 주차면 변화 비콘 (Major=5, minor > 32768)
// 기존 Beacon.java 대체
// ─────────────────────────────────────────────────────────────────────────────
@Keep
data class ParkingBeaconData(
    @SerializedName("ID")    @Expose val beaconId: String,
    @SerializedName("State") @Expose val state:    String,  // Major 값
    @SerializedName("Rssi")  @Expose val rssi:     String,
    @SerializedName("Delay") @Expose val delay:    String,
    @SerializedName("Seq")   @Expose val seq:      String,
)

// ─────────────────────────────────────────────────────────────────────────────
// AccelBeaconData — 주차면 평시 비콘 RSSI 최댓값 (Major=4)
// 기존 AccelBeacon.java 대체
// ─────────────────────────────────────────────────────────────────────────────
@Keep
data class AccelBeaconData(
    @SerializedName("ID")        @Expose val beaconId:  String,
    @SerializedName("Rssi")      @Expose val rssi:      String,
    @SerializedName("Delay")     @Expose val delay:     String,
    @SerializedName("Count")     @Expose val count:     String,
    /** delay 기준 오름차순 정렬된 "rssi_delay" 문자열 Set */
    @SerializedName("DelayList") @Expose val delayList: LinkedHashSet<String> = LinkedHashSet(),
)
