package com.kirosc.wifilogger

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.kirosc.wifilogger.databinding.ActivityMainBinding
import com.kirosc.wifilogger.helper.WiFi
import com.kirosc.wifilogger.helper.WiFiHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    /**
     * Request code for permission
     */
    private val LOCATION_PERMISSIONS_REQUEST_CODE = 1000
    private val STORAGE_PERMISSIONS_REQUEST_CODE = 1001
    private val TAG = "WiFiLogger_Debug"

    private lateinit var wifiHelper: WiFiHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var wifiList = ArrayList<WiFi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)


        binding.loading = true

        // Setup helper and client
        wifiHelper = WiFiHelper(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // What to do after getting the last location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    binding.loading = false
                    binding.latitude = location.latitude.toString()
                    binding.longitude = location.longitude.toString()
                } else {
                    Toast.makeText(this, getString(R.string.location_error), Toast.LENGTH_SHORT).show()
                }
            }

//        val adapter = object : WiFiHelper(this) {
//            override fun scanSuccess() {
//                super.scanSuccess()
//
//            }
//        }
        wifiList.add(WiFi("38:BC:01:64:2D:38", "Example", -87, "WPA2"))

        recycler_view.apply {

            layoutManager = LinearLayoutManager(context)

            adapter = WiFiAdapter(wifiList)
        }

        Handler().postDelayed({
            recycler_view.adapter?.notifyDataSetChanged()
        }, 1)

//        if (checkLocationPermission()) {
//            wifiHelper.scan()
//        }
    }

    override fun onResume() {
        super.onResume()
        wifiHelper.register()
    }


    override fun onPause() {
        super.onPause()
        wifiHelper.unregister()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    Toast.makeText(this, getString(R.string.require_location_permission), Toast.LENGTH_SHORT).show()
                    finish()
                }
                return
            }

            STORAGE_PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    Toast.makeText(this, getString(R.string.require_storage_permission), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }


    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSIONS_REQUEST_CODE
            )
            return false
        } else {
            return true
        }
    }
}
