package com.pms_parkin_mobile.dto

import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import lombok.Data

/**
 * 비컨 클래스 - MAJOR 번호가 5번인 Beacon Data 를 수집
 *
 * Array List 에 넣기 위한 데이터를 만들 때
 * 서버로 데이터를 보낼때
 * 사용되는 클레스
 *
 * beaconId     :   Beacon ID                       :   ID
 * State        :   Beacon MAJOR 값                 :   State
 * Rssi         :   Beacon 의 Rssi                  :   Rssi
 * Delay        :   Beacon 을 받은 시간             :   Delay
 * Seq          :   Beacon 을 받은 갯수             :   Seq
 */
@Keep
@Data
class Beacon {
    @SerializedName("ID")
    @Expose
    private val beaconId: String? = null

    @SerializedName("State")
    @Expose
    private val state: String? = null

    @SerializedName("Rssi")
    @Expose
    private val rssi: String? = null

    @SerializedName("Delay")
    @Expose
    private val delay: String? = null

    @SerializedName("Seq")
    @Expose
    private val seq: String? = null
}
