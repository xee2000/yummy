package com.pms_parkin_mobile.service;

import static android.view.KeyCharacterMap.ALPHA;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import com.pms_parkin_mobile.dataManager.SaveArrayListValue;
import com.pms_parkin_mobile.util.KalmanFilter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    // (기존 변수들 유지)
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

    // === 센서 테스트 모드 정의 ===
    public static final String TEST_MODE_CENTER = "중앙";
    public static final String TEST_MODE_LEFT   = "좌측";
    public static final String TEST_MODE_RIGHT  = "우측";

    // === 각 테스트 시간 ===
    private static final long CENTER_TEST_DURATION_MS = 3000L;
    private static final long LEFT_TEST_DURATION_MS   = 3000L;
    private static final long RIGHT_TEST_DURATION_MS  = 3000L;

    // === 임계값 ===
    private static final float CENTER_GYRO_MAX_AVG = 0.60f;
    private static final float SIDE_GYRO_MAX_AVG   = 0.80f;

    // === 공통 누적 버퍼 ===
    private static boolean testAccumulating    = false;
    private static boolean testResultEvaluated = false;
    private static long    testStartMs         = 0L;

    private static int   testAccelSamples = 0;
    private static float testAccelMagSum  = 0f;
    private static int   testGyroSamples  = 0;
    private static float testGyroAbsSum   = 0f;

    // ✅ 레이스 방지용 Lock
    private static final Object TEST_LOCK = new Object();

    // ✅ 평가를 “센서 이벤트 기다리지 않고” 정확히 3초 후 실행하기 위한 스케줄러
    private static final ScheduledExecutorService TEST_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor();

    private static ScheduledFuture<?> evaluateFuture = null;

    // ✅ 이번 런에서 고정되는 모드(중간에 mode 바뀌어도 이 값으로 평가)
    private static String activeRunMode = TEST_MODE_CENTER;

    // 마지막 TestStartFlag 상태
    private static boolean lastTestStartFlag = false;

    private static long durationForMode(String mode) {
        if (TEST_MODE_LEFT.equals(mode))  return LEFT_TEST_DURATION_MS;
        if (TEST_MODE_RIGHT.equals(mode)) return RIGHT_TEST_DURATION_MS;
        return CENTER_TEST_DURATION_MS;
    }

    private static void cancelEvaluateFuture() {
        if (evaluateFuture != null) {
            evaluateFuture.cancel(false);
            evaluateFuture = null;
        }
    }

    // ✅ 테스트 시작 처리(모드 고정 + 누적 초기화 + 3초 후 평가 예약)
    private static void onTestStart(final String mode, final long now) {
        synchronized (TEST_LOCK) {
            activeRunMode = (mode == null ? TEST_MODE_CENTER : mode);

            Log.d("SensorTest", "Sensor Test start, mode=" + activeRunMode);

            testAccumulating    = true;
            testResultEvaluated = false;
            testStartMs         = now;

            testAccelSamples = 0;
            testAccelMagSum  = 0f;
            testGyroSamples  = 0;
            testGyroAbsSum   = 0f;

            // 현재 단계 결과만 초기화
            if (TEST_MODE_CENTER.equals(activeRunMode)) {
                App.getInstance().setStayTestResult(false);
            } else if (TEST_MODE_LEFT.equals(activeRunMode)) {
                App.getInstance().setLeftTestResult(false);
            } else if (TEST_MODE_RIGHT.equals(activeRunMode)) {
                App.getInstance().setRightTestResult(false);
            }

            // ✅ 기존 예약된 평가 제거 후 새로 예약
            cancelEvaluateFuture();

            long durationMs = durationForMode(activeRunMode);
            evaluateFuture = TEST_SCHEDULER.schedule(() -> {
                // 3초 되면 센서 이벤트 없이도 평가가 실행됨
                evaluateNow("timer");
            }, durationMs, TimeUnit.MILLISECONDS);
        }
    }

    // ✅ 테스트 종료 처리
    private static void onTestStop(String mode) {
        synchronized (TEST_LOCK) {
            Log.d("SensorTest", "테스트 종료확인 (mode=" + mode + ")");
            testAccumulating = false;
            cancelEvaluateFuture();
        }
    }

    // ✅ (핵심) 즉시 평가 실행: JS가 3초에 맞춰 읽어도 이미 값이 세팅되게 만들기
    private static void evaluateNow(String from) {
        synchronized (TEST_LOCK) {
            if (!testAccumulating || testResultEvaluated) return;

            boolean result;
            if (TEST_MODE_CENTER.equals(activeRunMode)) {
                result = evaluateCenterTest();
                App.getInstance().setStayTestResult(result);
            } else if (TEST_MODE_LEFT.equals(activeRunMode)) {
                result = evaluateLeftTest();
                App.getInstance().setLeftTestResult(result);
            } else {
                result = evaluateRightTest();
                App.getInstance().setRightTestResult(result);
            }

            App.getInstance().updateSensorTestAllPassed();

            testResultEvaluated = true;
            testAccumulating = false;
            cancelEvaluateFuture();

            // ✅★★ 이게 없어서 WaitResult가 무조건 timeout 났던 거임
            App.getInstance().setTestStartFlag(false);

            // ✅ 내부 edge 상태도 같이 맞춰서 “좌측 시작”이 바로 먹게 함
            lastTestStartFlag = false;

            Log.d("SensorTest",
                    "Test evaluated(" + from + "). mode=" + activeRunMode +
                            ", result=" + result +
                            ", allPassed=" + App.getInstance().isSensorTestAllPassed() +
                            ", -> startFlag=false"
            );
        }
    }
    // === 센서 결합 상태 갱신 + 테스트 타이밍 제어 ===
    private static void updateCombined() {
        final long now = System.currentTimeMillis();

        boolean testStartFlag = App.getInstance().isTestStartFlag();
        String  testMode = App.getInstance().getSensorTestMode();

        Log.d("SensorTest",
                "[updateCombined] now=" + now
                        + " startFlag=" + testStartFlag
                        + " lastStartFlag=" + lastTestStartFlag
                        + " mode(app)=" + testMode
                        + " accumulating=" + testAccumulating
                        + " evaluated=" + testResultEvaluated
                        + " activeRunMode=" + activeRunMode
                        + " samples gyro=" + testGyroSamples
                        + " accel=" + testAccelSamples
        );
        // ⬆ 테스트 시작 (false -> true)
        if (testStartFlag && !lastTestStartFlag) {
            Log.d("SensorTest",
                    "[EDGE] RISE detected!"
                            + " appMode=" + testMode
                            + " (startFlag=true, lastStartFlag=false)"
            );
            onTestStart(testMode, now);
        }

        // ⬇ 테스트 종료 (true -> false)
        if (!testStartFlag && lastTestStartFlag) {
            onTestStop(testMode);
        }

        // (보조 안전장치) 혹시 타이머가 밀려도 센서 이벤트로도 평가 가능
        if (testAccumulating && !testResultEvaluated) {
            long elapsed = now - testStartMs;
            long durationMs = durationForMode(activeRunMode);
            if (elapsed >= durationMs) {
                evaluateNow("sensor");
            }
        }

        lastTestStartFlag = testStartFlag;
    }

    // === 평가 로직들 ===
    private static boolean evaluateCenterTest() {
        if (testGyroSamples == 0) return false;
        float avgGyroAbs = testGyroAbsSum / testGyroSamples;
        Log.d("SensorTest", "[CENTER] avgGyroAbs=" + avgGyroAbs);

        boolean stable = (avgGyroAbs <= CENTER_GYRO_MAX_AVG);

        if (testAccelSamples > 0) {
            float avgAccelMag = testAccelMagSum / testAccelSamples;
            Log.d("SensorTest", "[CENTER] avgAccelMag=" + avgAccelMag);
        }

        Log.d("SensorTest", "[CENTER] result=" + stable);
        return stable;
    }

    private static boolean evaluateLeftTest() {
        if (testGyroSamples == 0) return false;
        float avgGyroAbs = testGyroAbsSum / testGyroSamples;
        Log.d("SensorTest", "[LEFT] avgGyroAbs=" + avgGyroAbs);

        boolean stable = (avgGyroAbs <= SIDE_GYRO_MAX_AVG);

        if (testAccelSamples > 0) {
            float avgAccelMag = testAccelMagSum / testAccelSamples;
            Log.d("SensorTest", "[LEFT] avgAccelMag=" + avgAccelMag);
        }

        Log.d("SensorTest", "[LEFT] result=" + stable);
        return stable;
    }

    private static boolean evaluateRightTest() {
        if (testGyroSamples == 0) return false;
        float avgGyroAbs = testGyroAbsSum / testGyroSamples;
        Log.d("SensorTest", "[RIGHT] avgGyroAbs=" + avgGyroAbs);

        boolean stable = (avgGyroAbs <= SIDE_GYRO_MAX_AVG);

        if (testAccelSamples > 0) {
            float avgAccelMag = testAccelMagSum / testAccelSamples;
            Log.d("SensorTest", "[RIGHT] avgAccelMag=" + avgAccelMag);
        }

        Log.d("SensorTest", "[RIGHT] result=" + stable);
        return stable;
    }

    // === 가속도 센서 ===
    public static void ReadAccel(float[] data) {
        if (data == null || data.length < 3) return;

        // ✅ 테스트 중이면 누적 + 상태갱신(하지만 테스트가 아니어도 "운전/낙상" 로직은 살아있어야 함)
        if (App.getInstance().isTestStartFlag()) {
            synchronized (TEST_LOCK) {
                if (testAccumulating) {
                    float ax  = data[0];
                    float ay  = data[1];
                    float az  = data[2];
                    float mag = (float) Math.sqrt(ax * ax + ay * ay + az * az);
                    testAccelMagSum += mag;
                    testAccelSamples++;
                }
            }

            // moved 계산(기존 유지)
            if (hasLastAccel) {
                float diffX = Math.abs(data[0] - lastAccel[0]);
                float diffY = Math.abs(data[1] - lastAccel[1]);
                float diffZ = Math.abs(data[2] - lastAccel[2]);
                float diffSum = diffX + diffY + diffZ;
                if (diffSum > ACCEL_THRESHOLD) {
                    // 필요하면 기록
                }
            }

            lastAccel[0] = data[0];
            lastAccel[1] = data[1];
            lastAccel[2] = data[2];
            hasLastAccel = true;

            updateCombined();
        }

        // ==== 아래부터는 기존 운전/낙상 관련 로직 ====
        if (!App.getInstance().isServiceFlag() || !App.getInstance().isSensorTestAllPassed()) {
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

        if (drive_accel_index >= 9) {
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

        // ✅ 테스트 중이면 누적 + 상태갱신
        if (App.getInstance().isTestStartFlag()) {
            synchronized (TEST_LOCK) {
                if (testAccumulating) {
                    float gx = data[0];
                    float gy = data[1];
                    float gz = data[2];
                    float absSum = Math.abs(gx) + Math.abs(gy) + Math.abs(gz);
                    testGyroAbsSum += absSum;
                    testGyroSamples++;
                }
            }

            if (hasLastGyro) {
                float diffX = Math.abs(data[0] - lastGyro[0]);
                float diffY = Math.abs(data[1] - lastGyro[1]);
                float diffZ = Math.abs(data[2] - lastGyro[2]);
                float diffSum = diffX + diffY + diffZ;
                if (diffSum > GYRO_THRESHOLD) {
                    // 필요하면 기록
                }
            }

            lastGyro[0] = data[0];
            lastGyro[1] = data[1];
            lastGyro[2] = data[2];
            hasLastGyro = true;

            updateCombined();
        }

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

    // (아래 GyroSensorResult는 원본 그대로)
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

                if (App.getInstance().isServiceFlag()) {
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