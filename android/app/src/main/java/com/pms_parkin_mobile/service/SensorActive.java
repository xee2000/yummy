package com.pms_parkin_mobile.service;

import static android.view.KeyCharacterMap.ALPHA;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.util.KalmanFilter;

public class SensorActive {

    // === 센서 이전값 저장 ===
    private static float[] lastAccel = new float[3];
    private static float[] lastGyro  = new float[3];
    private static boolean hasLastAccel = false;
    private static boolean hasLastGyro  = false;

    // === 민감도 임계값 ===
    private static final float ACCEL_THRESHOLD = 0.15f;
    private static final float GYRO_THRESHOLD  = 0.05f;

    SensorManager SensorManager_W;
    Sensor AccelSensor_W;
    Sensor GyroSensor_W;

    // 두 센서 “동시” 판단을 위한 윈도우 (ms)
    private static final long COMBINE_WINDOW_MS = 600L;

    // 최근 움직임 감지 시각
    private static long accelMovedAt = 0L;
    private static long gyroMovedAt  = 0L;

    private static KalmanFilter kalman_X = new KalmanFilter(0.0f);
    private static KalmanFilter kalman_Y = new KalmanFilter(0.0f);
    private static KalmanFilter kalman_Z = new KalmanFilter(0.0f);

    private static float accel_x;
    private static float accel_y;
    private static float accel_z;
    private static int   drive_accel_index = 0;
    private static int   drive_con_count   = 0;
    private static final float alpha = 0.8f;
    private static final float[] gravity             = new float[3];
    private static final float[] linear_acceleration = new float[3];

    static int RollResultCount  = 0;
    static int PitchResultCount = 0;
    static int YawResultCount   = 0;

    static int PreRollCount  = 0;
    static int PrePitchCount = 0;
    static int PreYawCount   = 0;

    static float NextValue       = 0;
    static float PreValue        = 0;
    static int   DefaultAbsValue = 4;
    static boolean BMatchValue   = true;
    static int     IMatchValue   = 0;
    static boolean SR = false;
    static boolean SP = false;
    static boolean SY = false;
    static int     limitValue    = 100;

    // 마지막 TestFlag 상태
    private static boolean lastTestFlag = false;

    // === 센서 테스트 모드 정의 (App 쪽 상수/값과 맞춰야 함) ===
    public static final String TEST_MODE_CENTER = "중앙";
    public static final String TEST_MODE_LEFT   = "좌측";
    public static final String TEST_MODE_RIGHT  = "우측";

    // === 각 테스트 시간 ===
    private static final long CENTER_TEST_DURATION_MS = 3000L;
    private static final long LEFT_TEST_DURATION_MS   = 3000L;
    private static final long RIGHT_TEST_DURATION_MS  = 3000L;

    // === 실측 데이터 기반 임계값 (널널하게 조정) ===
    // 중앙(정면): 0.6 이하까지 OK로 완전 느슨하게
    private static final float CENTER_GYRO_MAX_AVG = 0.60f;
    // 좌/우측: 팔/손 움직임 좀 더 허용해서 0.8까지 OK
    private static final float SIDE_GYRO_MAX_AVG   = 0.80f;

    // (참고용 로그에만 사용, 판정에는 사용하지 않음)
    private static final float ACCEL_MIN_MAG = 0.0f;
    private static final float ACCEL_MAX_MAG = 30.0f;

    // === 공통 누적 버퍼 (센터 / 좌 / 우 공용) ===
    private static boolean testAccumulating    = false;
    private static boolean testResultEvaluated = false;
    private static long    testStartMs         = 0L;

    private static int   testAccelSamples = 0;
    private static float testAccelMagSum  = 0f;
    private static int   testGyroSamples  = 0;
    private static float testGyroAbsSum   = 0f;

    // === 센서 결합 상태 갱신 + 테스트 타이밍 제어 ===
    private static void updateCombined() {
        final long now = System.currentTimeMillis();

        boolean accelRecent = (now - accelMovedAt) <= COMBINE_WINDOW_MS;
        boolean gyroRecent  = (now - gyroMovedAt)  <= COMBINE_WINDOW_MS;

        boolean testFlag = App.getInstance().isTestFlag();
        // JS/NativeModule 쪽에서 반드시 세팅해 줘야 함: "중앙"/"좌측"/"우측"
        String  testMode = App.getInstance().getSensorTestMode();

        // 어떤 테스트인지에 따라 duration 선택
        long durationMs;
        if (TEST_MODE_LEFT.equals(testMode)) {
            durationMs = LEFT_TEST_DURATION_MS;
        } else if (TEST_MODE_RIGHT.equals(testMode)) {
            durationMs = RIGHT_TEST_DURATION_MS;
        } else { // 기본은 중앙
            durationMs = CENTER_TEST_DURATION_MS;
        }

        // ⬆ 테스트 시작 (false -> true)
        if (testFlag && !lastTestFlag) {
            Log.d("SensorTest", "Sensor Test start, mode=" + testMode);

            testAccumulating    = true;
            testResultEvaluated = false;
            testStartMs         = now;

            testAccelSamples = 0;
            testAccelMagSum  = 0f;
            testGyroSamples  = 0;
            testGyroAbsSum   = 0f;

            // 시작 시 각 결과 false로 초기화
            App.getInstance().setStayTestResult(false);
            App.getInstance().setLeftTestResult(false);
            App.getInstance().setRightTestResult(false);
        }

        // ⬇ 테스트 종료/취소 (true -> false)
        if (!testFlag && lastTestFlag) {
            Log.d("SensorActive", "테스트 종료확인 (mode=" + testMode + ")");
            testAccumulating = false;
        }

        // ⏱ 시간 채워졌으면 평가 한 번만 수행
        if (testAccumulating && !testResultEvaluated) {
            long elapsed = now - testStartMs;
            if (elapsed >= durationMs) {
                boolean result;
                if (TEST_MODE_CENTER.equals(testMode)) {
                    result = evaluateCenterTest();
                    App.getInstance().setStayTestResult(result);
                } else if (TEST_MODE_LEFT.equals(testMode)) {
                    result = evaluateLeftTest();
                    App.getInstance().setLeftTestResult(result);
                } else { // RIGHT
                    result = evaluateRightTest();
                    App.getInstance().setRightTestResult(result);
                }

                testResultEvaluated = true;
                testAccumulating    = false;

                Log.d(
                        "SensorTest",
                        "Test evaluated. mode=" + testMode +
                                ", result=" + result +
                                ", accelSamples=" + testAccelSamples +
                                ", gyroSamples=" + testGyroSamples
                );
            }
        }

        lastTestFlag = testFlag;

        Log.d(
                "SensorTest",
                "TestFlag=" + testFlag +
                        ", accelRecent=" + accelRecent +
                        ", gyroRecent=" + gyroRecent +
                        ", mode=" + testMode
        );
    }

    // === 평가 로직들 (널널 버전) ===

    // 중앙(정면): 거의 안 흔들릴 때 통과 (0.6 이하)
    private static boolean evaluateCenterTest() {
        if (testGyroSamples == 0) return false;

        float avgGyroAbs = testGyroAbsSum / testGyroSamples;
        Log.d("SensorActive", "[CENTER] avgGyroAbs=" + avgGyroAbs);

        boolean stable = (avgGyroAbs <= CENTER_GYRO_MAX_AVG);

        if (testAccelSamples > 0) {
            float avgAccelMag = testAccelMagSum / testAccelSamples;
            Log.d("SensorActive", "[CENTER] avgAccelMag=" + avgAccelMag);
            // 지금은 참고용만, 판정에는 사용 X
        }

        Log.d("SensorActive", "[CENTER] result=" + stable);
        return stable;
    }

    // 좌측: 기준은 중앙보다 조금 느슨 (0.8 이하)
    private static boolean evaluateLeftTest() {
        if (testGyroSamples == 0) return false;

        float avgGyroAbs = testGyroAbsSum / testGyroSamples;
        Log.d("SensorActive", "[LEFT] avgGyroAbs=" + avgGyroAbs);

        boolean stable = (avgGyroAbs <= SIDE_GYRO_MAX_AVG);

        if (testAccelSamples > 0) {
            float avgAccelMag = testAccelMagSum / testAccelSamples;
            Log.d("SensorActive", "[LEFT] avgAccelMag=" + avgAccelMag);
        }

        Log.d("SensorActive", "[LEFT] result=" + stable);
        return stable;
    }

    // 우측: 좌측과 동일한 기준 (0.8 이하)
    private static boolean evaluateRightTest() {
        if (testGyroSamples == 0) return false;

        float avgGyroAbs = testGyroAbsSum / testGyroSamples;
        Log.d("SensorActive", "[RIGHT] avgGyroAbs=" + avgGyroAbs);

        boolean stable = (avgGyroAbs <= SIDE_GYRO_MAX_AVG);

        if (testAccelSamples > 0) {
            float avgAccelMag = testAccelMagSum / testAccelSamples;
            Log.d("SensorActive", "[RIGHT] avgAccelMag=" + avgAccelMag);
        }

        Log.d("SensorActive", "[RIGHT] result=" + stable);
        return stable;
    }

    // === 가속도 센서 ===
    public static void ReadAccel(float[] data) {
        if (data == null || data.length < 3) return;
        if (!App.getInstance().isTestFlag()) {
            return;
        }

        // 테스트용 Accel 원본 누적 (센터/좌/우 공통)
        if (testAccumulating) {
            float ax  = data[0];
            float ay  = data[1];
            float az  = data[2];
            float mag = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            testAccelMagSum += mag;
            testAccelSamples++;
        }

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

        // ==== 아래부터는 기존 운전/낙상 관련 로직 (그대로 유지) ====
        if (!App.getInstance().isServiceFlag() || !App.getInstance().isTestFlag()) {
            return;
        }

        float CVA;
        float[] accelData = new float[3];

        accelData = filter(data.clone(), accelData);
        CVA = (float) Math.sqrt(
                accelData[0] * accelData[0] +
                        accelData[1] * accelData[1] +
                        accelData[2] * accelData[2]);

        if (PreValue != 0) {
            NextValue = CVA;
            float ABSValue = Math.abs(PreValue - NextValue);
            if (ABSValue >= DefaultAbsValue) {
                App.getInstance().setmAccelCount(
                        App.getInstance().getmAccelCount() + 1);
            }
            PreValue = NextValue;
        } else {
            PreValue = CVA;
        }

        if (drive_accel_index >= 9) { //10 -> 0.1초
            float accel_x_value = (float) (accel_x / 10);
            float accel_y_value = (float) (accel_y / 10);
            float accel_z_value = (float) (accel_z / 10);

            if ((accel_x_value > 0.02 && accel_x_value < 0.4) ||
                    (accel_y_value > 0.02 && accel_y_value < 0.4) ||
                    (accel_z_value > 0.02 && accel_z_value < 0.4)) {
                drive_con_count++;
                if (drive_con_count > 10) {
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
            gravity[0] = alpha * gravity[0] + (1 - alpha) * data[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * data[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * data[2];

            linear_acceleration[0] = data[0] - gravity[0];
            linear_acceleration[1] = data[1] - gravity[1];
            linear_acceleration[2] = data[2] - gravity[2];

            accel_x += Math.abs(linear_acceleration[0]);
            accel_y += Math.abs(linear_acceleration[1]);
            accel_z += Math.abs(linear_acceleration[2]);
        }
        drive_accel_index++;
    }

    // === 자이로 센서 ===
    public static void ReadGyro(float[] data) {
        if (data == null || data.length < 3) return;

        // 테스트용 gyro 원본 누적
        if (testAccumulating) {
            float gx = data[0];
            float gy = data[1];
            float gz = data[2];
            float absSum = Math.abs(gx) + Math.abs(gy) + Math.abs(gz);
            testGyroAbsSum += absSum;
            testGyroSamples++;
        }

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

        updateCombined();

        // ==== 아래부터는 기존 Gyro + Kalman 로직 (그대로 유지) ====
        final float LIMIT_MAX = 0.5f;
        final float LIMIT_MIN = -0.5f;

        double Roll  = data[0];
        double Pitch = data[1];
        double Yaw   = data[2];

        if ((Roll > LIMIT_MAX || Roll < LIMIT_MIN) ||
                (Pitch > LIMIT_MAX || Pitch < LIMIT_MIN) ||
                (Yaw > LIMIT_MAX || Yaw < LIMIT_MIN)) {
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

    private static void GyroSensorResult(double Roll, double Pitch, double Yaw) {
        final float LimitMinus = -0.025f;
        final float LimitPlus  = 0.025f;

        if (App.getInstance().getROLL_QUEUE() == null) {
            return;
        }
        if (App.getInstance().getROLL_QUEUE().size() == 4) {
            Object[] RollA = App.getInstance().getROLL_QUEUE().toArray();

            if (!(((double) RollA[0] < LimitPlus && (double) RollA[0] > LimitMinus) ||
                    ((double) RollA[1] < LimitPlus && (double) RollA[1] > LimitMinus) ||
                    ((double) RollA[2] < LimitPlus && (double) RollA[2] > LimitMinus) ||
                    ((double) RollA[3] < LimitPlus && (double) RollA[3] > LimitMinus))) {
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
            Double THIRDR  = App.getInstance().getROLL_QUEUE().poll();
            Double FORTHR  = App.getInstance().getROLL_QUEUE().poll();

            App.getInstance().getROLL_QUEUE().offer(SECONDR == null ? 0.0 : SECONDR);
            App.getInstance().getROLL_QUEUE().offer(THIRDR  == null ? 0.0 : THIRDR);
            App.getInstance().getROLL_QUEUE().offer(FORTHR  == null ? 0.0 : FORTHR);
            App.getInstance().getROLL_QUEUE().offer(Roll);
        } else {
            App.getInstance().getROLL_QUEUE().offer(Roll);
        }

        if (App.getInstance().getPITCH_QUEUE().size() == 4) {
            Object[] PITCHA = App.getInstance().getPITCH_QUEUE().toArray();

            if (!(((double) PITCHA[0] < LimitPlus && (double) PITCHA[0] > LimitMinus) ||
                    ((double) PITCHA[1] < LimitPlus && (double) PITCHA[1] > LimitMinus) ||
                    ((double) PITCHA[2] < LimitPlus && (double) PITCHA[2] > LimitMinus) ||
                    ((double) PITCHA[3] < LimitPlus && (double) PITCHA[3] > LimitMinus))) {
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
            Double THIRDP  = App.getInstance().getPITCH_QUEUE().poll();
            Double FORTHP  = App.getInstance().getPITCH_QUEUE().poll();

            App.getInstance().getPITCH_QUEUE().offer(SECONDP == null ? 0.0 : SECONDP);
            App.getInstance().getPITCH_QUEUE().offer(THIRDP  == null ? 0.0 : THIRDP);
            App.getInstance().getPITCH_QUEUE().offer(FORTHP  == null ? 0.0 : FORTHP);
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
            Double THIRDY  = App.getInstance().getYAW_QUEUE().poll();
            Double FORTH   = App.getInstance().getYAW_QUEUE().poll();

            App.getInstance().getYAW_QUEUE().offer(SECONDY == null ? 0.0 : SECONDY);
            App.getInstance().getYAW_QUEUE().offer(THIRDY  == null ? 0.0 : THIRDY);
            App.getInstance().getYAW_QUEUE().offer(FORTH   == null ? 0.0 : FORTH);
            App.getInstance().getYAW_QUEUE().offer(Yaw);
        } else {
            App.getInstance().getYAW_QUEUE().offer(Yaw);
        }

        if ((PreRollCount >= limitValue || PrePitchCount >= limitValue || PreYawCount >= limitValue) &&
                ((RollResultCount == 0 && PreRollCount != 0) ||
                        (PitchResultCount == 0 && PrePitchCount != 0) ||
                        (YawResultCount == 0 && PreYawCount != 0))) {

            IMatchValue++;
            BMatchValue = true;

            App.getInstance().setmSaveCountRoll(PreRollCount);
            App.getInstance().setmSaveCountPitch(PrePitchCount);
            App.getInstance().setmSaveCountYaw(PreYawCount);

            SR = false;
            SP = false;
            SY = false;
            PreRollCount  = 0;
            PrePitchCount = 0;
            PreYawCount   = 0;
        } else {
            BMatchValue = false;
            if (IMatchValue != 0) {

                if (App.getInstance().isStartFlag()) {
                    SaveArrayListValue saveArrayListValue = new SaveArrayListValue();
                    saveArrayListValue.SaveGyro();
                }

                BMatchValue      = false;
                IMatchValue      = 0;
                PitchResultCount = 0;
                RollResultCount  = 0;
                YawResultCount   = 0;
            }
        }
    }
}