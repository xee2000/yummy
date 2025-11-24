package com.pms_parkin_mobile.util;

public class KalmanFilter {
    private double Q = 0.00001;
    private double R = 0.001;
    private double X = 0, P = 1, K;

    public KalmanFilter(double InitValue) {
        X = InitValue;
    }

    private void MeasuremEntUpdate() {
        K = (P + Q) / (P + Q + R);
        P = R * (P + Q) / (R + P + Q);
    }

    public double Update(double Value) {
        MeasuremEntUpdate();
        X = X + (Value - X) * K;

        return X;
    }

    public void Init() {
        X = 0;
        P = 0;
        K = 0;

        Update(0);
    }
}
