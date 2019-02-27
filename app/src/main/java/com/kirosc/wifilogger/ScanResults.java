package com.kirosc.wifilogger;

import java.util.ArrayList;

/**
 * Class that used to store a scan result.
 */
public class ScanResults
{
    private static final String TAG = "ScanResults_Debug";

    // The coordination of the scan
    private double latitude, longitude;
    // The List of WiFi discovered
    private ArrayList<WiFi> wiFiList;

    ScanResults(double latitude, double longitude, ArrayList<WiFi> wiFiList) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.wiFiList = wiFiList;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public ArrayList<WiFi> getWiFiList() {
        return wiFiList;
    }
}
