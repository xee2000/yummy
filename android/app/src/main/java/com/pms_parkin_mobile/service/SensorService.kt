package com.pms_parkin_mobile.service

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.hardware.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pms_parkin_mobile.R
import com.pms_parkin_mobile.api.RestController
import com.pms_parkin_mobile.data.SensorDataStore
import com.woorisystem.domain.AccelSensorData
import com.woorisystem.domain.AccelSensorData2
import com.woorisystem.domain.GyroSensorData
import com.woorisystem.domain.GyroSensorData2
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

@SuppressLint("MissingPermission")
class SensorService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "SensorService"
        private const val NOTIFICATION_ID = 822
        private const val CHANNEL_ID = "sensor_scan_channel"

        private const val LOW_PASS_ALPHA = 0.8f
        private const val CVA_THRESHOLD = 4.0f
        private const val DRIVE_SAMPLE_WINDOW = 10
        private const val DRIVE_ACCEL_MIN = 0.02f
        private const val DRIVE_ACCEL_MAX = 0.4f
        private const val GYRO_QUEUE_SIZE = 4
        private const val GYRO_STILL_THRESHOLD = 0.025f
        private const val GYRO_ROTATION_LIMIT = 100
        private const val ACCEL_TIMER_INTERVAL_MS = 1_000L
        private const val ACCEL_MOTION_CONFIRM_COUNT = 3

        @Volatile
        private var instance: SensorService? = null
        fun getInstance(): SensorService? = instance
        fun isRunning(): Boolean = instance != null

        private val rollAxis = GyroAxisState()
        private val pitchAxis = GyroAxisState()
        private val yawAxis = GyroAxisState()
    }

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null
    val sensorDataStore = SensorDataStore()

    private var prevCva = 0f
    private val gravity = FloatArray(3)
    private val linearAccelAcc = FloatArray(3)
    private var driveAccelIndex = 0

    private var iMatchValue = 0
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var accelTimerJob: Job? = null

    @Volatile private var isAccelTimerRunning = false

    private val parkingState get() = BluetoothService.getInstance()?.parkingState

    // =========================================================================
    // 서비스 라이프사이클
    // =========================================================================

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.i("$TAG: onCreate")

        startForegroundCompat()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 서비스가 죽어도 다시 살아나도록 설정
        return START_STICKY
    }

    private fun startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(CHANNEL_ID, "Sensor Service", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(chan)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("스마트파킹")
            .setContentText("센서 데이터 감지 중")
            .setSmallIcon(R.drawable.logo)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)} else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // =========================================================================
    // 센서 이벤트 처리
    // =========================================================================

    override fun onSensorChanged(event: SensorEvent) {
        val state = parkingState ?: return
        if (!state.isWorkingState) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> onAccelerometerChanged(event)
            Sensor.TYPE_GYROSCOPE -> onGyroscopeChanged(event)
        }
    }

    private fun onAccelerometerChanged(event: SensorEvent) {
        val filtered = lowPassFilter(event.values)
        val cva = sqrt(filtered[0] * filtered[0] + filtered[1] * filtered[1] + filtered[2] * filtered[2])

        if (prevCva != 0f && abs(prevCva - cva) >= CVA_THRESHOLD) {
            sensorDataStore.incrementAccelCount()
        }
        prevCva = cva

        if (!isAccelTimerRunning) startAccelTimer()
        updateDriveDetection(event.values)
    }

    private fun onGyroscopeChanged(event: SensorEvent) {
        val roll = event.values[0].toDouble()
        val pitch = event.values[1].toDouble()
        val yaw = event.values[2].toDouble()
        collectGyroRaw(roll, pitch, yaw)
    }

    // =========================================================================
    // 가속도 타이머 (T/S/W 판단)
    // =========================================================================

    private fun startAccelTimer() {
        if (isAccelTimerRunning) return
        isAccelTimerRunning = true

        accelTimerJob = scope.launch {
            var prevState: String? = null
            var motionConsecutiveCount = 0
            var motionStartedFromT = false
            var seq = sensorDataStore.accelSequence

            while (parkingState?.isWorkingState == true) {
                delay(ACCEL_TIMER_INTERVAL_MS)

                val count = sensorDataStore.accelCount
                val state = if (count < 3) "T" else if (count < 12) "S" else "W"
                val delayTime = parkingState?.wholeTimerDelay ?: 0

                sensorDataStore.addAccelSensorData(AccelSensorData(state, delayTime.toString(), seq.toString()))
                sensorDataStore.addAccelSensorData2(AccelSensorData2(count.toString(), delayTime.toString(), seq.toString()))

                if (state == "S" || state == "W") {
                    if (motionConsecutiveCount == 0 && prevState == "T") motionStartedFromT = true
                    motionConsecutiveCount++
                } else {
                    motionConsecutiveCount = 0
                    motionStartedFromT = false
                }

                if (motionStartedFromT && motionConsecutiveCount == ACCEL_MOTION_CONFIRM_COUNT) {
                    BluetoothService.getInstance()?.beaconTimers?.setCollectAccelBeacon(true)
                    motionStartedFromT = false
                }

                prevState = state
                seq++
                sensorDataStore.accelSequence = seq
                sensorDataStore.resetAccelCount()
            }
            isAccelTimerRunning = false
        }
    }

    // =========================================================================
    // 자이로 데이터 수집 및 회전 감지
    // =========================================================================

    private fun collectGyroRaw(roll: Double, pitch: Double, yaw: Double) {
        val delayTime = parkingState?.wholeTimerDelay?.toString() ?: "0"
        sensorDataStore.addGyroSensorData(GyroSensorData(cutTo2(roll).toString(), cutTo2(pitch).toString(), cutTo2(yaw).toString(), delayTime))
        collectGyroFiltered(roll, pitch, yaw)
    }

    private fun collectGyroFiltered(roll: Double, pitch: Double, yaw: Double) {
        updateGyroAxis(rollAxis, roll)
        updateGyroAxis(pitchAxis, pitch)
        updateGyroAxis(yawAxis, yaw)

        val isLimitReached = (rollAxis.preCount >= GYRO_ROTATION_LIMIT || pitchAxis.preCount >= GYRO_ROTATION_LIMIT || yawAxis.preCount >= GYRO_ROTATION_LIMIT)
        val isStoppedNow = ((rollAxis.count == 0 && rollAxis.preCount != 0) || (pitchAxis.count == 0 && pitchAxis.preCount != 0) || (yawAxis.count == 0 && yawAxis.preCount != 0))

        if (isLimitReached && isStoppedNow) {
            iMatchValue++
            sensorDataStore.saveCountRoll = rollAxis.preCount
            sensorDataStore.saveCountPitch = pitchAxis.preCount
            sensorDataStore.saveCountYaw = yawAxis.preCount

            rollAxis.flag = false; rollAxis.preCount = 0
            pitchAxis.flag = false; pitchAxis.preCount = 0
            yawAxis.flag = false; yawAxis.preCount = 0
        } else if (iMatchValue != 0) {
            saveGyro()
            RestController.instance.Message("자이로반응확인")
            BluetoothService.getInstance()?.beaconTimers?.startAfterGyroTimer()
            iMatchValue = 0
        }
    }

    private fun updateGyroAxis(axis: GyroAxisState, newValue: Double) {
        if (axis.queue.size == GYRO_QUEUE_SIZE) {
            val still = GYRO_STILL_THRESHOLD.toDouble()
            val allMoving = axis.queue.none { it in -still..still }
            if (allMoving) axis.count++ else {
                if (axis.count >= GYRO_ROTATION_LIMIT) {
                    axis.flag = true
                    axis.preCount = axis.count
                }
                axis.count = 0
            }
            axis.queue.removeFirst()
        }
        axis.queue.addLast(newValue)
    }

    private fun saveGyro() {
        val delayTime = parkingState?.wholeTimerDelay?.toString() ?: "0"
        sensorDataStore.addGyroSensorData2(GyroSensorData2(sensorDataStore.saveCountRoll.toString(), sensorDataStore.saveCountPitch.toString(), sensorDataStore.saveCountYaw.toString(), delayTime))
    }

    // =========================================================================
    // 유틸리티 및 필터
    // =========================================================================

    private val lpfBuffer = FloatArray(3)
    private fun lowPassFilter(input: FloatArray): FloatArray {
        for (i in lpfBuffer.indices) lpfBuffer[i] = lpfBuffer[i] + LOW_PASS_ALPHA * (input[i] - lpfBuffer[i])
        return lpfBuffer
    }

    private fun updateDriveDetection(rawValues: FloatArray) {
        if (driveAccelIndex >= DRIVE_SAMPLE_WINDOW - 1) {
            linearAccelAcc.fill(0f)
            driveAccelIndex = 0
        } else {
            for (i in 0..2) {
                gravity[i] = LOW_PASS_ALPHA * gravity[i] + (1 - LOW_PASS_ALPHA) * rawValues[i]
                linearAccelAcc[i] += abs(rawValues[i] - gravity[i])
            }
            driveAccelIndex++
        }
    }

    private fun cutTo2(value: Double): Double = (value * 100.0).toLong() / 100.0

    fun resetSensorState() {
        prevCva = 0f; gravity.fill(0f); linearAccelAcc.fill(0f); driveAccelIndex = 0
        rollAxis.clear(); pitchAxis.clear(); yawAxis.clear()
        iMatchValue = 0; accelTimerJob?.cancel(); isAccelTimerRunning = false
        sensorDataStore.reset()
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        accelTimerJob?.cancel()
        instance = null
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?): IBinder? = null

    private data class GyroAxisState(
        val queue: ArrayDeque<Double> = ArrayDeque(GYRO_QUEUE_SIZE),
        var count: Int = 0,
        var preCount: Int = 0,
        var flag: Boolean = false
    ) {
        fun clear() { queue.clear(); count = 0; preCount = 0; flag = false }
    }
}