package com.pms_parkin_mobile.dto;

public class Item {
    private String address;
    private double rssi;
    private int txPower;
    private double distance;
    private int major;
    private int minor;

    public Item(String address, double rssi, int txPower, double distance, int major, int minor) {
        this.address = address;
        this.rssi = rssi;
        this.txPower = txPower;
        this.distance = distance;
        this.major = major;
        this.minor = minor;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public int getTxPower() {
        return txPower;
    }

    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }
}
