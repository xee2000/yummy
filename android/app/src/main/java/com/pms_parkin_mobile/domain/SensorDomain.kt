package com.woorisystem.domain

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * 자이로 센서 데이터 (표준 모드 / bigData raw 모드)
 * 기존 GyroSensor.java → Kotlin data class 교체
 * SerializedName은 기존 서버 API 필드명과 동일하게 유지
 */
@Keep
data class GyroSensorData(
    @SerializedName("X") @Expose val x: String,
    @SerializedName("Y") @Expose val y: String,
    @SerializedName("Z") @Expose val z: String,
    @SerializedName("Delay") @Expose val delay: String,
)

/**
 * 자이로 센서 데이터 2 (bigData 모드 — 회전 카운트값)
 * 기존 GyroSensor2.java → Kotlin data class 교체
 */
@Keep
data class GyroSensorData2(
    @SerializedName("X") @Expose val x: String,
    @SerializedName("Y") @Expose val y: String,
    @SerializedName("Z") @Expose val z: String,
    @SerializedName("Delay") @Expose val delay: String,
)

/**
 * 가속도 센서 상태 데이터 (T/S/W 분류값)
 * 기존 AccelSensor.java → Kotlin data class 교체
 */
@Keep
data class AccelSensorData(
    @SerializedName("State") @Expose val state: String,
    @SerializedName("Delay") @Expose val delay: String,
    @SerializedName("Seq")   @Expose val seq:   String,
)

/**
 * 가속도 센서 실측 데이터 (CVA 임계 초과 카운트 실측값)
 * 기존 AccelSensor2.java → Kotlin data class 교체
 */
@Keep
data class AccelSensorData2(
    @SerializedName("State") @Expose val state: String,
    @SerializedName("Delay") @Expose val delay: String,
    @SerializedName("Seq")   @Expose val seq:   String,
)
