package com.kirosc.wifilogger;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kirosc.wifilogger.utilities.IOUtils;
import com.kirosc.wifilogger.utilities.JsonUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements LocationListener, View.OnClickListener {

    private static final String TAG = "WiFiLogger_Debug";

    /**
     * Request code for permission
     **/
    private static final int LOCATION_PERMISSIONS_REQUEST_CODE = 1000;
    private static final int STORAGE_PERMISSIONS_REQUEST_CODE = 1001;

    /**
     * Request code for file picker
     **/
    private static final int PICK_FILE_REQUEST_CODE = 2000;

    /**
     * Constant for the read only mode
     **/
    private static final int OFF = -1;
    private static final int ON = 1;
    private static final int TOGGLE = 0;

    private long scanTime;
    private boolean isHandlerPosted, readOnlyMode, sendingEmail;

    private TextView loadingView, emptyView, latitudeView, longitudeView;
    private FloatingActionButton fab;

    // Object for the RecyclerView
    private RecyclerView mRecyclerView;
    private WiFiAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private LocationManager locationManager;
    private WifiManager wifiManager;
    private Location lastKnownLocation;
    private AlertDialog.Builder noGpsDialogBuilder;
    private AlertDialog noGpsDialog;
    private ArrayList<WiFi> scanWiFiList;
    private ScanResults currentScanResult;
    private BroadcastReceiver wifiReceiver;
    private ProgressDialog progressDialog;
    private MenuItem menuItem;

    // Create the Handler object (on the main thread by default)
    private Handler handler = new Handler();
    // Define the code block to be executed
    Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            Log.d(TAG, "Handler is called on main thread");
            scanWiFi();
            handler.postDelayed(runnableCode, scanTime);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Initialize View Object   //
        loadingView = findViewById(R.id.loading_tv);
        emptyView = findViewById(R.id.empty_view);
        latitudeView = findViewById(R.id.latitude_number_tv);
        longitudeView = findViewById(R.id.longitude_number_tv);
        mRecyclerView = findViewById(R.id.recycler_view);
        fab = findViewById(R.id.fab);
        //  End of initialization    //

        // Improve performance since that changes in content
        // do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // Use a LinearLayout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Specify an adapter
        scanWiFiList = new ArrayList();
        mAdapter = new WiFiAdapter(this, scanWiFiList);
        mRecyclerView.setAdapter(mAdapter);

        // Add the divider line
        mRecyclerView.addItemDecoration(new DividerItemDecoration(
                mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL
        ));

        // Hide FAB button when scrolling the list
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && fab.isShown())
                    fab.hide();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    fab.show();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        // Initialize the preference value
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        scanTime = TimeUnit.SECONDS.toMillis(Long.parseLong(getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE).getString(getString(R.string.key_scan_interval), null)));
        Log.d(TAG, "Delay every " + TimeUnit.MILLISECONDS.toSeconds(scanTime) + " seconds");

        // Initialize the Managers
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);


        if (checkLocationPermission()) {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get the preference value again
        scanTime = TimeUnit.SECONDS.toMillis(Long.parseLong(getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE).getString(getString(R.string.key_scan_interval), null)));

        // Register Button Listener
        fab.setOnClickListener(this);

        if (!readOnlyMode) {
            if (checkLocationPermission()) {
                checkIsLocationEnabled();

                // Register the Listener
                registerBroadcastReceiver();
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

                // Delete email temp file if user comes back from Email app
                if (sendingEmail) {
                    IOUtils.deleteLastFile();
                    sendingEmail = false;
                }

                // Load the last known Location and scan WiFi
                getLastKnownLocationAndScan();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove BroadcastReceiver and Location Listener
        if (!readOnlyMode && wifiReceiver != null) {
            unregisterReceiver(wifiReceiver);
        }
        locationManager.removeUpdates(this);

        // Stop the handler
        handler.removeCallbacksAndMessages(null);
        isHandlerPosted = false;

        // Remove OnClick Listener
        fab.setOnClickListener(null);

        // Dismiss the scanning dialog
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        menuItem = item;
        int id = item.getItemId();

        if (id == R.id.email) {
            sendEmail();
        } else if (id == R.id.save) {
            if (checkStoragePermission()) {
                // Save file
                if (IOUtils.saveFile(this, currentScanResult)) {
                    Toast.makeText(this, "Saved to " + IOUtils.getSavedFile().getAbsolutePath(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.fail_save), Toast.LENGTH_SHORT).show();
                }
            }
        } else if (id == R.id.open) {
            openFile();
        } else if (id == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab) {
            readOnlyMode(TOGGLE);
            if (readOnlyMode) {
                Toast.makeText(this, getString(R.string.stop_scanning), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.resume_scanning), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed");
        lastKnownLocation = location;

        latitudeView.setText(String.format("%.6f", lastKnownLocation.getLatitude()));
        longitudeView.setText(String.format("%.6f", lastKnownLocation.getLongitude()));

        // Change the loadingView to the result list
        if (loadingView.getVisibility() == View.VISIBLE) {
            loadingView.setVisibility(View.GONE);
            latitudeView.setVisibility(View.VISIBLE);
            longitudeView.setVisibility(View.VISIBLE);
        }

        // Schedule the scanning if it has not been done
        if (!isHandlerPosted) {
            // Run the code block in the runnableCode after certain seconds
            Log.d(TAG, "Scanning for the first time");
            isHandlerPosted = true;
            scanWiFi();
            handler.postDelayed(runnableCode, scanTime);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (status == LocationProvider.OUT_OF_SERVICE) {
            Log.d(TAG, provider + "is out of service.");
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onProviderEnabled called");
        checkIsLocationEnabled();
    }

    @Override
    public void onProviderDisabled(String provider) {
        checkIsLocationEnabled();
    }

    /**
     * Check if the app has the Location permission, otherwise, request for the permission.
     *
     * @return true if has otherwise false
     */
    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission from user
            ActivityCompat.requestPermissions(this
                    , new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}
                    , LOCATION_PERMISSIONS_REQUEST_CODE);
            Log.d(TAG, "Request permission for Location");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check if the app has the Storage permission, otherwise, request for the permission.
     *
     * @return true if has otherwise false
     */
    private boolean checkStoragePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this
                    , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    , STORAGE_PERMISSIONS_REQUEST_CODE);
            Log.d(TAG, "Request permission for Storage");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length == 0) {
                return;
            }
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
                getLastKnownLocationAndScan();
            } else {
                Toast.makeText(this, getString(R.string.require_location_permission), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Location permission is not granted");
                finish();
            }
        } else if (requestCode == STORAGE_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length == 0) {
                return;
            }
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "Storage permission is not granted");
                Toast.makeText(this, getString(R.string.require_storage_permission), Toast.LENGTH_SHORT).show();
            } else {
                if (menuItem != null) {
                    // Do the same task again
                    onOptionsItemSelected(menuItem);
                }
            }
        }
    }

    /**
     * Check if GPS is enabled.
     *
     * @return true if provider is enable otherwise false
     */
    private boolean checkIsLocationEnabled() {
        Log.d(TAG, "Check Location");

        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            // Pop up an Alert Dialog to request user to enable GPS
            if (noGpsDialogBuilder == null) {
                noGpsDialogBuilder = new AlertDialog.Builder(this)
                        .setIcon(getResources().getDrawable(R.drawable.ic_gps, null))
                        .setTitle(getString(R.string.no_gps))
                        .setMessage(getString(R.string.enable_gps))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                        });
                noGpsDialogBuilder.setCancelable(false);
            }

            // Check if Dialog is built
            if (noGpsDialog != null) {
                if (!noGpsDialog.isShowing()) {
                    noGpsDialog = noGpsDialogBuilder.show();
                }
            } else {
                noGpsDialog = noGpsDialogBuilder.show();
            }
            return false;
        } else {
            // Dismiss the Dialog if GPS is enabled
            if (noGpsDialog != null) {
                noGpsDialog.dismiss();
                noGpsDialog = null;
            }
        }
        return true;
    }

    /**
     * Update the current Location and scan the WiFi.
     */
    private void getLastKnownLocationAndScan() {
        if (checkLocationPermission()) {
            Location newLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (newLocation == null) {
                Log.d(TAG, "newLocation is null");
                return;
            } else {
                lastKnownLocation = newLocation;

                latitudeView.setText(String.format("%.6f", lastKnownLocation.getLatitude()));
                longitudeView.setText(String.format("%.6f", lastKnownLocation.getLongitude()));

                // Change to the result view
                if (loadingView.getVisibility() == View.VISIBLE) {
                    loadingView.setVisibility(View.GONE);
                    latitudeView.setVisibility(View.VISIBLE);
                    longitudeView.setVisibility(View.VISIBLE);
                }
                if (!isHandlerPosted) {
                    // Run the code block in the runnableCode after certain seconds
                    Log.d(TAG, "Scanning for the first time");
                    isHandlerPosted = true;
                    scanWiFi();
                    handler.postDelayed(runnableCode, scanTime);
                }
            }
        }
    }

    /**
     * Register a BroadcastReceiver to receiver the WiFi scan result.
     */
    private void registerBroadcastReceiver() {
        // Define the IntentFilter listening to WiFi scanning result only
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // An ArrayList that used to store each access point data
                ArrayList<WiFi> tempList = new ArrayList();
                // The List of result that get back from the system
                List<ScanResult> results = wifiManager.getScanResults();

                Log.v(TAG, "Wi-Fi Scan Results ... Count:" + results.size());
                for (int i = 0; i < results.size(); ++i) {
                    Log.v(TAG, "  BSSID       =" + results.get(i).BSSID.toUpperCase());
                    Log.v(TAG, "  SSID        =" + results.get(i).SSID);
                    Log.v(TAG, "  dBm         =" + results.get(i).level);
                    Log.v(TAG, "  cap.        =" + results.get(i).capabilities);
                    Log.v(TAG, "---------------");

                    // Write the access point data to a new WiFi object
                    // Then, put the new WiFi object into an ArrayList
                    String protocol = results.get(i).capabilities;
                    if (protocol.contains("WEP")) {
                        tempList.add(new WiFi(results.get(i).BSSID.toUpperCase(), results.get(i).SSID, results.get(i).level, WiFi.WEP));
                    } else if (protocol.contains("WPA2")) {
                        tempList.add(new WiFi(results.get(i).BSSID.toUpperCase(), results.get(i).SSID, results.get(i).level, WiFi.WPA2));
                    } else if (protocol.contains("WPA")) {
                        tempList.add(new WiFi(results.get(i).BSSID.toUpperCase(), results.get(i).SSID, results.get(i).level, WiFi.WPA));
                    } else {
                        tempList.add(new WiFi(results.get(i).BSSID.toUpperCase(), results.get(i).SSID, results.get(i).level, WiFi.OPEN));
                    }
                }
                // Write the ArrayList to the global variable
                scanWiFiList = tempList;

                // Update the UI
                if (scanWiFiList.size() == 0) {
                    mRecyclerView.setVisibility(View.GONE);
                    emptyView.setText(getString(R.string.no_wifi_nearby));
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mAdapter.updateList(scanWiFiList);
                }

                // Write to the own ScanResults class
                if (lastKnownLocation != null) {
                    currentScanResult = new ScanResults(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), scanWiFiList);
                }
                Log.d(TAG, JsonUtils.toJson(currentScanResult));

                // Dismiss the scanning Dialog
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog.cancel();
                }

                // Scroll the RecyclerView to the top
                mLayoutManager.smoothScrollToPosition(mRecyclerView, null, 0);

                Toast.makeText(context, getString(R.string.scan_finished), Toast.LENGTH_SHORT).show();
            }
        };
        // Finally, register the Broadcast Receiver
        registerReceiver(wifiReceiver, filter);
    }

    /**
     * Scan the nearby WiFi.
     */
    private void scanWiFi() {
        // Only scan when the app has the GPS coordination
        if (lastKnownLocation == null) {
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        // Show the Progress Dialog
        progressDialog = ProgressDialog.show(this, "",
                getString(R.string.scanning), true);

        wifiManager.startScan();
    }

    /**
     * Open the file manager to let the user choose the file.
     */
    private void openFile() {
        /* Checks if external storage is available to at least read */
        if (!IOUtils.isExternalStorageReadable()) {
            Toast.makeText(this, getString(R.string.no_external_storage), Toast.LENGTH_SHORT).show();
            return;
        }

        readOnlyMode(ON);

        // Start the file manager
        Intent mRequestFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        mRequestFileIntent.setType("*/*");
        startActivityForResult(mRequestFileIntent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request the app is responding to
        if (requestCode == PICK_FILE_REQUEST_CODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // Convert the JSON String into a ScanResults object
                ScanResults oldResult = JsonUtils.toScanResults(IOUtils.openFile(getContentResolver(), data));

                if (oldResult == null) {
                    // Convert failure.
                    Toast.makeText(this, getString(R.string.file_not_loaded), Toast.LENGTH_SHORT).show();
                } else {
                    // Update the ScanResults
                    currentScanResult = oldResult;

                    latitudeView.setText(String.format("%.6f", currentScanResult.getLatitude()));
                    longitudeView.setText(String.format("%.6f", currentScanResult.getLongitude()));

                    // Update the UI
                    if (currentScanResult.getWiFiList().size() == 0) {
                        mRecyclerView.setVisibility(View.GONE);
                        emptyView.setText(getString(R.string.no_wifi_nearby));
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        mRecyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                        mAdapter.updateList(currentScanResult.getWiFiList());
                    }
                    Toast.makeText(this, getString(R.string.file_loaded), Toast.LENGTH_SHORT).show();
                }
            } else if (resultCode == RESULT_CANCELED) {
                // User does not choose a file
                readOnlyMode(OFF);
            }
        }
    }

    /**
     * Save the ScanResult on the device first, then send the Email
     */
    private void sendEmail() {
        // Cannot send Email since no scan result
        if (currentScanResult == null) {
            Toast.makeText(this, getString(R.string.no_scan_result), Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for storage permission
        if (!checkStoragePermission()) {
            return;
        }

        // Save the scan result and get the file Uri
        IOUtils.saveFile(this, currentScanResult);
        Uri path = Uri.fromFile(IOUtils.getSavedFile());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822"); // Only email apps should handle this
        intent.putExtra(Intent.EXTRA_STREAM, path);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Avoid FileUriExposedException
        // Since after Nougat, Google forbade the app to expose a file:// Uri to another app.
        // Also, it is not efficient to build a Content Provider for this app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        sendingEmail = true;
        startActivity(intent);
    }

    /**
     * Turn on or off the read only mode.
     *
     * @param mode Turn off if -1; Toggle if 0; Turn on if 1
     */
    private void readOnlyMode(int mode) {
        if ((mode == OFF) || (mode == TOGGLE && readOnlyMode)) {
            readOnlyMode = false;

            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop, getTheme()));

            if (checkLocationPermission()) {
                checkIsLocationEnabled();

                // Register the Listener
                registerBroadcastReceiver();
                // TODO: Synchronize with WiFi scan??
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

                fab.setOnClickListener(this);

                // Load the last known Location and scan WiFi
                getLastKnownLocationAndScan();
            }

        } else {
            readOnlyMode = true;

            fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_scan, getTheme()));

            // Remove BroadcastReceiver and Location Listener
            if (wifiReceiver != null) {
                unregisterReceiver(wifiReceiver);
                // Make sure will not unregister more than once
                wifiReceiver = null;
            }
            locationManager.removeUpdates(this);

            // Stop the handler
            handler.removeCallbacksAndMessages(null);
            isHandlerPosted = false;
        }
    }
}