package com.pms_parkin_mobile.dto;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Data;

/**
 * 통합 클래스  -   전체 타이머 종료시 데이터를 모아 서버로 보내기 위한 클래스
 *
 * Array List 에 넣기 위한 데이터를 만들 때
 * 서버로 데이터를 보낼때
 * 사용되는 클레스
 *
 * phoneInfo        :   핸드폰 고유의 PhoneID                                    :   PhoneInfo
 * sensorList       :   Accel Sensor 값 ArrayList 로 모은것                      :   Sensors
 * beaconList       :   Beacon 값 ArrayList 로 모은것                            :   Beacons
 * gyroList         :   Gyro Sensor 값 ArrayList 로 모은것                       :   Gyros
 * AccelBeacons     :   가장 높은 Rssi 값을 가진 Beacon 값 ArrayList 로 모은것   :   AccelBeacons
 **/
@Keep
@Data
public class Total {
    @SerializedName("PhoneInfo")
    @Expose
    private String phoneInfo;

    @SerializedName("InputDate")
    @Expose
    private String inputDate;
    /**
     * 게이트웨이 보드 설정 정보 리스트
     */
    @SerializedName("Sensors")
    @Expose
    private List<AccelSensor> sensorList;

    // 0421 jhlee sensor2추가
    @SerializedName("Sensors2")
    @Expose
    private List<AccelSensor2> sensorList2;
    // 0421 jhlee sensor2추가끝

    /**
     * 게이트웨이 보드 설정 정보 리스트
     */
    @SerializedName("Beacons")
    @Expose
    private List<Beacon> beaconList;

    @SerializedName("Gyros")
    @Expose
    private List<GyroSensor> gyroList;

    @SerializedName("AccelBeacons")
    @Expose
    private List<AccelBeacon> accelBeaconList;

    @SerializedName("ParingState")
    @Expose
    private String paringState;

    public String getPhoneInfo() {
        return phoneInfo;
    }

    public void setPhoneInfo(String phoneInfo) {
        this.phoneInfo = phoneInfo;
    }

    public String getInputDate() {
        return inputDate;
    }

    public void setInputDate(String inputDate) {
        this.inputDate = inputDate;
    }

    public List<AccelSensor> getSensorList() {
        return sensorList;
    }
    //jhlee 0421 AccelSensor2추가
    public List<AccelSensor2> getSensorList2() {
        return sensorList2;
    }

    public void setSensorList(List<AccelSensor> sensorList) {
        this.sensorList = sensorList;
    }
    public void setSensorList2(List<AccelSensor2> sensorList2) {
        this.sensorList2 = sensorList2;
    }
    //jhlee 0421 AccelSensor2추가끝

    public List<Beacon> getBeaconList() {
        return beaconList;
    }

    public void setBeaconList(List<Beacon> beaconList) {
        this.beaconList = beaconList;
    }

    public List<GyroSensor> getGyroList() {
        return gyroList;
    }

    public void setGyroList(List<GyroSensor> gyroList) {
        this.gyroList = gyroList;
    }

    public List<AccelBeacon> getAccelBeaconList() {
        return accelBeaconList;
    }

    public void setAccelBeaconList(List<AccelBeacon> accelBeaconList) {
        this.accelBeaconList = accelBeaconList;
    }

    public String getParingState() {
        return paringState;
    }

    public void setParingState(String paringState) {
        this.paringState = paringState;
    }

    @Override
    public String toString() {
        return "Total{" +
                "phoneInfo='" + phoneInfo + '\'' +
                ", inputDate='" + inputDate + '\'' +
                ", sensorList=" + sensorList +
                ", sensorList2=" + sensorList2 +
                ", beaconList=" + beaconList +
                ", gyroList=" + gyroList +
                ", accelBeaconList=" + accelBeaconList +
                ", paringState='" + paringState + '\'' +
                '}';
    }
}
