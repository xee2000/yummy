package com.pms_parkin_mobile.dto;

import lombok.Data;

@Data
public class Sensor {
    public long timestampMillis;
    public float gyroX;
    public float gyroY;
    public float gyroZ;

    public Sensor(long timestampMillis, float gyroX, float gyroY, float gyroZ) {
        this.timestampMillis = timestampMillis;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
    }

    // 여기에 기본(공통) 필드를 더 넣고 싶다면 자유롭게 추가하세요 (예: deviceId, sessionId 등)
}