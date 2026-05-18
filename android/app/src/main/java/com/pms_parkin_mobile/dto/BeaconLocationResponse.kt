package com.pms_parkin_mobile.dto

import com.google.gson.annotations.SerializedName

/**
 * 서버에서 좌표(x, y) 기반으로 반환하는 비컨 정보
 * GET /app/beacon/findByLocation?x=...&y=...
 */
data class BeaconLocationResponse(
    @SerializedName("beaconId") val beaconId: String,
    @SerializedName("x")        val x: Double?,
    @SerializedName("y")        val y: Double?
)
