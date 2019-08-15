package com.kirosc.wifilogger.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log

open class WiFiHelper(_context: Context) {
    private val TAG = "WiFiLogger_Debug"

    private val context = _context
    private val intentFilter = IntentFilter()
    private val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            } else {
                scanFailure()
            }
        }
    }

    var nearbyWifi = ArrayList<WiFi>()

    init {
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)
    }

    fun scan() {
        wifiManager.startScan();
    }

    fun scanSuccess() {
        val results = wifiManager.scanResults

        Log.v(TAG, "Wi-Fi Scan Results ... Count:" + results.size)
        Log.v(TAG, "---------------")
        for (result in results) {
            Log.v(TAG, "\tBSSID\t= " + result.BSSID.toUpperCase())
            Log.v(TAG, "\tSSID\t= " + result.SSID)
            Log.v(TAG, "\tdBm\t\t= " + result.level)
            Log.v(TAG, "\tcap.\t= " + result.capabilities)
            Log.v(TAG, "---------------")

            var encryption: String

            if ("WEP" in result.capabilities) {
                encryption = "WEP"
            } else if ("WPA2" in result.capabilities) {
                encryption = "WPA2"
            } else if ("WPA" in result.capabilities) {
                encryption = "WPA"
            } else {
                encryption = "OPEN"
            }

            nearbyWifi.add(WiFi(result.BSSID.toUpperCase(), result.SSID, result.level, encryption))
        }

        // Execute callback
        updateUI()
    }

    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        val results = wifiManager.scanResults
    }


    fun register() {
        context.registerReceiver(wifiScanReceiver, intentFilter)
    }

    fun unregister() {
        context.unregisterReceiver(wifiScanReceiver)
    }

    open fun updateUI() {

    }

}