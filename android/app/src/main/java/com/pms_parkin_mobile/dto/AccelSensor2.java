package com.pms_parkin_mobile.dto;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.Data;

/**
 * 엑셀 센서 실측값 클래스 TSW 데이터가 아닌 실측값 전송
 *
 * Array List 에 넣기 위한 데이터를 만들 때
 * 서버로 데이터를 보낼때
 * 사용되는 클레스
 *
 * state      :   사용자의 행동 상태 값                          :   State
 * delay      :   행동 상태 값을 받아오기 까지 걸린 시간           :   Delay
 * Seq        :   행동 값을 받아온 횟수                          :   Seq
 **/
@Keep
@Data
public class AccelSensor2 {
    @SerializedName("State")
    @Expose
    private String state;

    @SerializedName("Delay")
    @Expose
    private String delay;

    @SerializedName("Seq")
    @Expose
    private String seq;

}
