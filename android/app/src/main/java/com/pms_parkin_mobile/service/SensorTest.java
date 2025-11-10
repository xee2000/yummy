package com.pms_parkin_mobile.service;

import android.util.Log;
import com.pms_parkin_mobile.App;

public class SensorTest {

    // ======== 이전 센서값 저장용 ========
    private static float[] lastAccel = new float[3];
    private static float[] lastGyro = new float[3];
    private static boolean hasLastAccel = false;
    private static boolean hasLastGyro = false;

    // ======== 민감도 임계값 ========
    private static final float ACCEL_THRESHOLD = 0.15f; // 가속도 변화합 임계 (g 환산값처럼 사용 중)
    private static final float GYRO_THRESHOLD  = 0.05f; // 자이로 변화합 임계 (rad/s)

    // 두 센서 “동시” 판단을 위한 윈도우 (ms)
    private static final long COMBINE_WINDOW_MS = 600L;

    // 최근 움직임 감지 시각
    private static long accelMovedAt = 0L;
    private static long gyroMovedAt  = 0L;

    // 마지막으로 set한 상태(불필요한 반복 set 방지용)
    private static Boolean lastCombined = null;

    private static void updateCombined() {
        final long now = System.currentTimeMillis();

        // 최근 감지가 윈도우 내에 있는지
        boolean accelRecent = (now - accelMovedAt) <= COMBINE_WINDOW_MS;
        boolean gyroRecent  = (now - gyroMovedAt)  <= COMBINE_WINDOW_MS;

        // 둘 다 윈도우 내 감지 = AND
        boolean combined = accelRecent && gyroRecent;

        // 불필요한 중복 저장 방지
        if (lastCombined == null || lastCombined != combined) {
            App.getInstance().setSensorStatus(combined);
            lastCombined = combined;
            Log.d("SensorTest", "combined sensorStatus = " + combined +
                    " (accelRecent=" + accelRecent + ", gyroRecent=" + gyroRecent + ")");
        }
    }

    /** 가속도 센서 데이터 읽기 및 변화 감지 */
    public static void ReadAccel(float[] data) {
        if (!App.getInstance().isTestFlag()) return;
        if (data == null || data.length < 3) return;

        boolean moved = false;
        if (hasLastAccel) {
            float diffX = Math.abs(data[0] - lastAccel[0]);
            float diffY = Math.abs(data[1] - lastAccel[1]);
            float diffZ = Math.abs(data[2] - lastAccel[2]);
            float diffSum = diffX + diffY + diffZ;

            if (diffSum > ACCEL_THRESHOLD) {
                moved = true;
                accelMovedAt = System.currentTimeMillis();
            }
        }

        lastAccel[0] = data[0];
        lastAccel[1] = data[1];
        lastAccel[2] = data[2];
        hasLastAccel = true;

        Log.d("SensorTest", String.format(
                "Accel: [%.3f, %.3f, %.3f], moved=%s",
                data[0], data[1], data[2], moved));

        // AND 판단 갱신
        updateCombined();
    }

    /** 자이로 센서 데이터 읽기 및 변화 감지 */
    public static void ReadGyro(float[] data) {
        if (!App.getInstance().isTestFlag()) return;
        if (data == null || data.length < 3) return;

        boolean moved = false;
        if (hasLastGyro) {
            float diffX = Math.abs(data[0] - lastGyro[0]);
            float diffY = Math.abs(data[1] - lastGyro[1]);
            float diffZ = Math.abs(data[2] - lastGyro[2]);
            float diffSum = diffX + diffY + diffZ;

            if (diffSum > GYRO_THRESHOLD) {
                moved = true;
                gyroMovedAt = System.currentTimeMillis();
            }
        }

        lastGyro[0] = data[0];
        lastGyro[1] = data[1];
        lastGyro[2] = data[2];
        hasLastGyro = true;

        Log.d("SensorTest", String.format(
                "Gyro:  [%.3f, %.3f, %.3f], moved=%s",
                data[0], data[1], data[2], moved));

        // AND 판단 갱신
        updateCombined();
    }
}