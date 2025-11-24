package com.pms_parkin_mobile.dto;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashSet;

import lombok.Data;

/**
 * 비컨 클래스 - 조건 상관없이 들어오는 Beacon의 Data 를 수집하여 RSSI 가 가장 큰 데이터만 가지고 있는다
 * <p>
 * Array List 에 넣기 위한 데이터를 만들 때
 * 서버로 데이터를 보낼때
 * 사용되는 클레스
 * <p>
 * BeaconId     :   Beacon ID                    :   ID
 * Rssi         :   Beacon 의 Rssi (강도)         :   Rssi
 * Delay        :   Beacon 을 받아온 소요 시간     :   Delay
 * Count        :   Beacon 을 받은 갯수           :   Count
 **/
@Keep
@Data
public class AccelBeacon {
    @SerializedName("ID")
    @Expose
    private String beaconId;

    @SerializedName("Rssi")
    @Expose
    private String rssi;

    @SerializedName("Delay")
    @Expose
    private String delay;

    @SerializedName("Count")
    @Expose
    private String count;

    // 타입을 String으로 변환 수정 JHLEE 2025.1.10
    @SerializedName("DelayList")
    @Expose
    private LinkedHashSet<String> delayList;
    // 타입을 String으로 변환 수정끝 JHLEE 2025.1.10
}
