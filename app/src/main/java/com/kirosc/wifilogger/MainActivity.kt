package com.kirosc.wifilogger

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.kirosc.wifilogger.data.ScanResults
import com.kirosc.wifilogger.databinding.ActivityMainBinding
import com.kirosc.wifilogger.data.WiFi
import com.kirosc.wifilogger.helper.WiFiHelper
import com.kirosc.wifilogger.utils.IOUtils
import com.kirosc.wifilogger.utils.IOUtils.Companion.openFile
import com.kirosc.wifilogger.utils.JsonUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    /**
     * Request code for permission
     */
    private val LOCATION_PERMISSIONS_REQUEST_CODE = 1000
    private val STORAGE_PERMISSIONS_REQUEST_CODE = 1001
    /**
     * Request code for file picker
     */
    private val PICK_FILE_REQUEST_CODE = 2000

    private val TAG = "WiFiLogger_Debug"

    private var wifiHelper: WiFiHelper? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var wifiList = ArrayList<WiFi>()
    private lateinit var mLocation: Location
    private var scanInterval: Long = 60000
    private lateinit var binding: ActivityMainBinding
    private var readOnly = false
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            binding.latitude = result?.lastLocation?.latitude
            binding.longitude = result?.lastLocation?.longitude
        }
    }

    // Create the Handler object (on the main thread by default)
    private val handler = Handler()
    // Define the code block to be executed
    private val runnableCode: Runnable = Runnable {
        // Do something here on the main thread
        Log.d(TAG, "Handler is called on main thread");
        if (wifiHelper != null && !binding.loading) scanAndSchedule(wifiHelper as WiFiHelper)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.loading = true

        // Initialize RecyclerView
        recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = WiFiAdapter(wifiList)
            addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        }

        initialize()

        // Add listener
        binding.fab.setOnClickListener {
            val icon: Int = if (readOnly) {
                startLocationUpdates()
                if (wifiHelper != null) scanAndSchedule(wifiHelper as WiFiHelper)
                toast(getString(R.string.resume_scanning))
                R.drawable.ic_stop
            } else {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                handler.removeCallbacksAndMessages(null);
                toast(getString(R.string.stop_scanning))
                R.drawable.ic_scan
            }
            binding.fab.setImageDrawable(resources.getDrawable(icon, theme))
            readOnly = !readOnly
        }

        // Hide FAB button when scrolling the list
        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 || dy < 0 && fab.isShown) fab.hide()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) fab.show()
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }

    override fun onResume() {
        super.onResume()

        val sharedPref =
            getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE)
        scanInterval = TimeUnit.SECONDS.toMillis(
            (sharedPref.getString(
                getString(R.string.key_scan_interval),
                "60"
            )).toLong()
        )
        if (haveLocationPermission()) {
            wifiHelper?.register()
            if (!readOnly) {
                getLastLocation()
                startLocationUpdates()
                if (wifiHelper != null) scanAndSchedule(wifiHelper as WiFiHelper)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(LocationCallback())
        try {
            handler.removeCallbacksAndMessages(null);
            wifiHelper?.unregister()
        } catch (e: Exception) {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.email -> {
                // TODO: sendEmail
                true
            }
            R.id.save -> {
                if (haveStoragePermission()) {
                    val scanResults = ScanResults(mLocation.latitude, mLocation.longitude, wifiList)
                    if (IOUtils.saveFile(scanResults)) {
                        toast("Saved to ${IOUtils.lastFile.absolutePath}")
                    } else {
                        toast(getString(R.string.fail_save))
                    }
                }
                true
            }
            R.id.open -> {
                openFile()
                true
            }
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initialize()
                } else {
                    toast(getString(R.string.require_location_permission))
                    finish()
                }
                return
            }

            STORAGE_PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    toast(getString(R.string.require_storage_permission))
                }
            }

            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Check which request the app is responding to
        if (requestCode == PICK_FILE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // Convert the JSON String into a ScanResults object
                    val readResult = try {
                        val rawString = openFile(contentResolver, data!!)
                        JsonUtils.toScanResults(rawString!!)!!
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toast(getString(R.string.file_not_loaded))
                        return
                    }

                    // Load result to UI
                    binding.latitude = readResult.latitude
                    binding.longitude = readResult.longitude

                    if (readResult.wifiList.size == 0) {
                        recycler_view.visibility = View.GONE
                        empty_view.text = getString(R.string.no_wifi_nearby)
                        empty_view.visibility = View.VISIBLE;
                    } else {
                        wifiList.clear()
                        wifiList.addAll(readResult.wifiList)
                        recycler_view.adapter?.notifyDataSetChanged()
                        recycler_view.smoothScrollToPosition(0)
                        binding.loading = false
                    }
                }
                Activity.RESULT_CANCELED -> readOnlyMode(0)
            }
        }
    }

    private fun initialize() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup helper and client
        wifiHelper = object : WiFiHelper(this) {
            override fun updateUI() {
                super.updateUI()
                wifiList.clear()
                wifiList.addAll(nearbyWifi)
                recycler_view.adapter?.notifyDataSetChanged()
                recycler_view.smoothScrollToPosition(0)
                binding.loading = false
            }
        }
    }


    private fun haveLocationPermission(): Boolean {
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
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSIONS_REQUEST_CODE
            )
            return false
        } else {
            return true
        }
    }

    private fun haveStoragePermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSIONS_REQUEST_CODE
            )
            return false
        } else {
            return true
        }
    }

    private fun getLastLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    mLocation = location
                    binding.latitude = location.latitude
                    binding.longitude = location.longitude
                } else {
                    toast(getString(R.string.location_error))
                }
            }
    }

    // Request location from the fused location provider
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = scanInterval
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun scanAndSchedule(wifiHelper: WiFiHelper) {
        binding.loading = true
        wifiHelper.scan()
        handler.postDelayed(runnableCode, scanInterval)
    }

    /**
     * Turn on or off the read only mode.
     *
     * @param mode Turn off if 0; Turn on if 1
     */
    private fun readOnlyMode(mode: Int) {
        val icon: Int = when(mode) {
            0 -> {
                startLocationUpdates()
                if (wifiHelper != null) scanAndSchedule(wifiHelper as WiFiHelper)
                toast(getString(R.string.resume_scanning))
                R.drawable.ic_stop
            }
            1 -> {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                handler.removeCallbacksAndMessages(null);
                toast(getString(R.string.stop_scanning))
                R.drawable.ic_scan
            }
            else -> return
        }
        binding.fab.setImageDrawable(resources.getDrawable(icon, theme))
        readOnly = !readOnly
    }

    /**
     * Open the file manager to let the user choose the file.
     */
    private fun openFile() {
        /* Checks if external storage is available to read */
        if (!IOUtils.isExternalStorageReadable()) {
            toast(getString(R.string.no_external_storage))
            return
        }

        readOnlyMode(1)

        // Start the file manager
        val mRequestFileIntent = Intent(Intent.ACTION_GET_CONTENT)
        mRequestFileIntent.type = "*/*"
        startActivityForResult(mRequestFileIntent, PICK_FILE_REQUEST_CODE)
    }

    // Extenstion function for Toast
    fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
