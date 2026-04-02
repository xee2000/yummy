package com.pms_parkin_mobile.util

class KalmanFilter(InitValue: Double) {
    private val Q = 0.00001
    private val R = 0.001
    private var X = 0.0
    private var P = 1.0
    private var K = 0.0

    init {
        X = InitValue
    }

    private fun MeasuremEntUpdate() {
        K = (P + Q) / (P + Q + R)
        P = R * (P + Q) / (R + P + Q)
    }

    fun Update(Value: Double): Double {
        MeasuremEntUpdate()
        X = X + (Value - X) * K

        return X
    }

    fun Init() {
        X = 0.0
        P = 0.0
        K = 0.0

        Update(0.0)
    }
}
