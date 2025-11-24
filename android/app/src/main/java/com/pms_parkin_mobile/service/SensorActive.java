package com.pms_parkin_mobile.service;

import static android.view.KeyCharacterMap.ALPHA;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.util.KalmanFilter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SensorActive {

    // ======== 이전 센서값 저장용 ========
    private static float[] lastAccel = new float[3];
    private static float[] lastGyro = new float[3];
    private static boolean hasLastAccel = false;
    private static boolean hasLastGyro = false;

    // ======== 민감도 임계값 ========
    private static final float ACCEL_THRESHOLD = 0.15f; // 가속도 변화합 임계 (g 환산값처럼 사용 중)
    private static final float GYRO_THRESHOLD = 0.05f; // 자이로 변화합 임계 (rad/s)
    SensorManager SensorManager_W;
    Sensor AccelSensor_W;
    Sensor GyroSensor_W;

    // 두 센서 “동시” 판단을 위한 윈도우 (ms)
    private static final long COMBINE_WINDOW_MS = 600L;

    // 최근 움직임 감지 시각
    private static long accelMovedAt = 0L;
    private static long gyroMovedAt = 0L;

    private static KalmanFilter kalman_X = new KalmanFilter(0.0f);
    private static KalmanFilter kalman_Y = new KalmanFilter(0.0f);
    private static KalmanFilter kalman_Z = new KalmanFilter(0.0f);

    private static float accel_x;
    private static float accel_y;
    private static float accel_z;
    private static int drive_accel_index = 0;
    private static int drive_con_count = 0;
    private static final float alpha = 0.8f;
    private static final float[] gravity = new float[3];
    private static final float[] linear_acceleration = new float[3];

    static int RollResultCount = 0;
    static int PitchResultCount = 0;
    static int YawResultCount = 0;

    static int PreRollCount = 0;
    static int PrePitchCount = 0;
    static int PreYawCount = 0;

    static float NextValue = 0;      // 현재 CVA 값
    static float PreValue = 0;       // 이전 CVA 값
    static int DefaultAbsValue = 4;  // 기본 Default PreValue-NextValue 절대값
    static boolean BMatchValue = true;
    static int IMatchValue = 0;
    static boolean SR = false;
    static boolean SP = false;
    static boolean SY = false;
    static int limitValue = 100;

    // 마지막으로 set한 상태(불필요한 반복 set 방지용)
    private static Boolean lastCombined = null;

    private static void updateCombined() {
        final long now = System.currentTimeMillis();

        // 최근 감지가 윈도우 내에 있는지
        boolean accelRecent = (now - accelMovedAt) <= COMBINE_WINDOW_MS;
        boolean gyroRecent = (now - gyroMovedAt) <= COMBINE_WINDOW_MS;

        // 테스트를 진행하려는 경우에만 업데이트가 되도록 한다.
        if (App.getInstance().isTestFlag() && !App.getInstance().isTestCompleteFlag()) {
            boolean combined = accelRecent && gyroRecent;
            if(combined){
                App.getInstance().setTestCompleteFlag(true);
            }
            // 불필요한 중복 저장 방지
            if (lastCombined == null || lastCombined != combined) {
                App.getInstance().setSensorStatus(combined);
                lastCombined = combined;
                Log.d("SensorTest", "combined sensorStatus = " + combined +
                        " (accelRecent=" + accelRecent + ", gyroRecent=" + gyroRecent + ")");
            }
        }


    }

    /**
     * 가속도 센서 데이터 읽기 및 변화 감지
     */
    public static void ReadAccel(float[] data) {
        if (data == null || data.length < 3) return;
        float CVA;


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
        updateCombined();
        Log.d("SensorTest", String.format(
                "Accel: [%.3f, %.3f, %.3f], moved=%s",
                data[0], data[1], data[2], moved));
        //서비스 시작시 시작 지점
        float[] accelData = new float[3];

        //서비스가 시작한경우 데이터 수집을 시작한디.
        if (App.getInstance().isServiceFlag()) {
            accelData = filter(data.clone(), accelData);
            CVA = (float) Math.sqrt(accelData[0] * accelData[0] + accelData[1] * accelData[1] + accelData[2] * accelData[2]);


            if (PreValue != 0) {
                NextValue = CVA;

                float ABSValue = Math.abs(PreValue - NextValue);

                if (ABSValue >= DefaultAbsValue) {
                    App.getInstance().setmAccelCount(App.getInstance().getmAccelCount() + 1);
                }
                PreValue = NextValue;
            } else {
                PreValue = CVA;
            }

            if (drive_accel_index >= 9) { //10 -> 0.1초
                float accel_x_value = (float) (accel_x / 10);
                float accel_y_value = (float) (accel_y / 10);
                float accel_z_value = (float) (accel_z / 10);

                if ((accel_x_value > 0.02 && accel_x_value < 0.4) || (accel_y_value > 0.02 && accel_y_value < 0.4) || (accel_z_value > 0.02 && accel_z_value < 0.4)) {
                    drive_con_count++;
                    // jhlee 2025.04.01 30 -> 10으로 단축
                    if (drive_con_count > 10) // 1초 유지
                    {
                        drive_con_count = 0;
                    }
                } else {
                    drive_con_count = 0;
                }

                accel_x = 0;
                accel_y = 0;
                accel_z = 0;
                drive_accel_index = 0;
            } else {
                //운전중을 확인하는 로직
                gravity[0] = alpha * gravity[0] + (1 - alpha) * data[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * data[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * data[2];

                //중력 성분 제거
                linear_acceleration[0] = data[0] - gravity[0];
                linear_acceleration[1] = data[1] - gravity[1];
                linear_acceleration[2] = data[2] - gravity[2];

                accel_x += Math.abs(linear_acceleration[0]);
                accel_y += Math.abs(linear_acceleration[1]);
                accel_z += Math.abs(linear_acceleration[2]);
            }
            drive_accel_index++;
            //jhlee 추가 끝 */
        }
    }

    public static void ReadGyro(float[] data) {
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

        final float LIMIT_MAX = 0.5f;
        final float LIMIT_MIN = -0.5f;

        double Roll = data[0];
        double Pitch = data[1];
        double Yaw = data[2];

        if ((Roll > LIMIT_MAX || Roll < LIMIT_MIN) || (Pitch > LIMIT_MAX || Pitch < LIMIT_MIN) || (Yaw > LIMIT_MAX || Yaw < LIMIT_MIN)) {
            kalman_X.Init();
            kalman_Y.Init();
            kalman_Z.Init();
        } else {
            double FilterX = kalman_X.Update(Roll);
            double FilterY = kalman_Y.Update(Pitch);
            double FilterZ = kalman_Z.Update(Yaw);

            GyroSensorResult((FilterX), (FilterY), (FilterZ));
        }
    }


    private static float[] filter(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }


    private static void  GyroSensorResult(double Roll, double Pitch, double Yaw) {
        final float LimitMinus = -0.025f;
        final float LimitPlus = 0.025f;

        if(App.getInstance().getROLL_QUEUE() == null){
            return;
        }
        if (App.getInstance().getROLL_QUEUE().size() == 4) {
            Object[] RollA = App.getInstance().getROLL_QUEUE().toArray();

            if (!(((double) RollA[0] < LimitPlus && (double) RollA[0] > LimitMinus) || ((double) RollA[1] < LimitPlus && (double) RollA[1] > LimitMinus) || ((double) RollA[2] < LimitPlus && (double) RollA[2] > LimitMinus) || ((double) RollA[3] < LimitPlus && (double) RollA[3] > LimitMinus))) {
                RollResultCount++;
            } else {
                if (RollResultCount >= limitValue) {
                    SR = true;
                    PreRollCount = RollResultCount;
                }
                RollResultCount = 0;
            }

            App.getInstance().getROLL_QUEUE().poll();
            Double SECONDR = App.getInstance().getROLL_QUEUE().poll();
            Double THIRDR = App.getInstance().getROLL_QUEUE().poll();
            Double FORTHR = App.getInstance().getROLL_QUEUE().poll();

            App.getInstance().getROLL_QUEUE().offer(SECONDR == null ? 0.0 : SECONDR);
            App.getInstance().getROLL_QUEUE().offer(THIRDR == null ? 0.0 : THIRDR);
            App.getInstance().getROLL_QUEUE().offer(FORTHR == null ? 0.0 : FORTHR);
            App.getInstance().getROLL_QUEUE().offer(Roll);
        } else {
            App.getInstance().getROLL_QUEUE().offer(Roll);
        }

        if (App.getInstance().getPITCH_QUEUE().size() == 4) {
            Object[] PITCHA = App.getInstance().getPITCH_QUEUE().toArray();

            if (!(((double) PITCHA[0] < LimitPlus && (double) PITCHA[0] > LimitMinus) || ((double) PITCHA[1] < LimitPlus && (double) PITCHA[1] > LimitMinus) || ((double) PITCHA[2] < LimitPlus && (double) PITCHA[2] > LimitMinus) || ((double) PITCHA[3] < LimitPlus && (double) PITCHA[3] > LimitMinus))) {
                PitchResultCount++;

            } else {
                if (PitchResultCount >= limitValue) {
                    SP = true;
                    PrePitchCount = PitchResultCount;
                }
                PitchResultCount = 0;
            }

            App.getInstance().getPITCH_QUEUE().poll();
            Double SECONDP = App.getInstance().getPITCH_QUEUE().poll();
            Double THIRDP = App.getInstance().getPITCH_QUEUE().poll();
            Double FORTHP = App.getInstance().getPITCH_QUEUE().poll();

            App.getInstance().getPITCH_QUEUE().offer(SECONDP == null ? 0.0 : SECONDP);
            App.getInstance().getPITCH_QUEUE().offer(THIRDP == null ? 0.0 : THIRDP);
            App.getInstance().getPITCH_QUEUE().offer(FORTHP == null ? 0.0 : FORTHP);
            App.getInstance().getPITCH_QUEUE().offer(Pitch);
        } else {
            App.getInstance().getPITCH_QUEUE().offer(Pitch);
        }

        if (App.getInstance().getYAW_QUEUE().size() == 4) {
            Object[] YAWA = App.getInstance().getYAW_QUEUE().toArray();

            if (!(((double) YAWA[0] < LimitPlus && (double) YAWA[0] > LimitMinus) ||
                    ((double) YAWA[1] < LimitPlus && (double) YAWA[1] > LimitMinus) ||
                    ((double) YAWA[2] < LimitPlus && (double) YAWA[2] > LimitMinus) ||
                    ((double) YAWA[3] < LimitPlus && (double) YAWA[3] > LimitMinus))) {
                YawResultCount++;
            } else {
                if (YawResultCount >= limitValue) {
                    SY = true;
                    PreYawCount = YawResultCount;
                }
                YawResultCount = 0;
            }

            App.getInstance().getYAW_QUEUE().poll();
            Double SECONDY = App.getInstance().getYAW_QUEUE().poll();
            Double THIRDY = App.getInstance().getYAW_QUEUE().poll();
            Double FORTH = App.getInstance().getYAW_QUEUE().poll();

            App.getInstance().getYAW_QUEUE().offer(SECONDY == null ? 0.0 : SECONDY);
            App.getInstance().getYAW_QUEUE().offer(THIRDY == null ? 0.0 : THIRDY);
            App.getInstance().getYAW_QUEUE().offer(FORTH == null ? 0.0 : FORTH);
            App.getInstance().getYAW_QUEUE().offer(Yaw);
        } else {
            App.getInstance().getYAW_QUEUE().offer(Yaw);
        }

        if ((PreRollCount >= limitValue || PrePitchCount >= limitValue || PreYawCount >= limitValue) && (RollResultCount == 0 && PreRollCount != 0) || (PitchResultCount == 0 && PrePitchCount != 0) || (YawResultCount == 0 && PreYawCount != 0)) {
            IMatchValue++;
            BMatchValue = true;

            App.getInstance().setmSaveCountRoll(PreRollCount);
            App.getInstance().setmSaveCountPitch(PrePitchCount);
            App.getInstance().setmSaveCountYaw(PreYawCount);

            SR = false;
            SP = false;
            SY = false;
            PreRollCount = 0;
            PrePitchCount = 0;
            PreYawCount = 0;
        } else {
            BMatchValue = false;
            if (IMatchValue != 0) {
                
                if (App.getInstance().isStartFlag()) {
                    SaveArrayListValue saveArrayListValue = new SaveArrayListValue();
                    saveArrayListValue.SaveGyro();
                }

                BMatchValue = false;
                IMatchValue = 0;

                PitchResultCount = 0;
                RollResultCount = 0;
                YawResultCount = 0;
            }
        }
    }
}
