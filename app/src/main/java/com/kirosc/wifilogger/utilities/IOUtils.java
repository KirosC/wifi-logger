package com.kirosc.wifilogger.utilities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.kirosc.wifilogger.ScanResults;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Updated by Kiros Choi on 2018/04/27.
 */
public class IOUtils {

    private static final String TAG = "SaveUtils_Debug";

    // Point to the last saved file
    private static File lastFile;

    /**
     * Save the ScanResults in the device's external storage.
     *
     * @param context of the called activity
     * @param scanResults The ScanResults that is going to save on the device
     * @return true if success, otherwise, false
     */
    public static boolean saveFile(Context context, ScanResults scanResults) {
        // Make sure start with a scan results that contained data
        if (scanResults == null) {
            return false;
        }
        // Get the time and append to the file name
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedTime = formatter.format(Calendar.getInstance().getTime());
        String fileName = "WiFiLogger_" + formattedTime + ".json";
        Log.d(TAG, "Current time format: " + formattedTime);

        if (isExternalStorageWritable()) {
            // Folder name
            String folder_main = "WiFi Logger";

            // File that points to the saving folder
            File file = new File(Environment.getExternalStorageDirectory() + "/" + folder_main);
            Log.d(TAG, "Save path: " + file.toString());

            // Create the directory if does not exist
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return false;
                }
            }

            // Saving process
            // Write in JSON format
            try {
                lastFile = file = new File(file, fileName);
                FileOutputStream stream = new FileOutputStream(file);
                try {
                    stream.write(JsonUtils.toJson(scanResults).getBytes());
                } finally {
                    stream.close();
                }
                Log.d(TAG, "File saved with name: " + fileName);
            } catch (IOException e) {
                Log.e(TAG, "File write failed: " + e.toString());
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Open a previously saved file.
     * @param resolver The Content Resolver.
     * @param data  The Intent that contains the Uri or Path that points to the file.
     * @return  The content of the JSON formatted file in String or null if failed to retrieve the file.
     */
    public static String openFile(ContentResolver resolver, Intent data) {
        if (data != null) {
            Log.d(TAG, "Intent Uri: " + data.getData().toString());
        }

        if (isExternalStorageReadable()) {
            try {
                // For using normal file manager that provides Content Provider
                // Get the InputStream from the Content Provider
                InputStream stream = resolver.openInputStream(data.getData());
                // Change the InputStream to String
                return new String(ByteStreams.toByteArray(stream));
            } catch (FileNotFoundException e) {
                e.printStackTrace();

                try {
                    // For using other file managers that provide storage path
                    File file = new File(data.getData().getPath());
                    // Get the InputStream from the given path
                    FileInputStream stream = new FileInputStream(file);

                    return new String(ByteStreams.toByteArray(stream));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Get the last saved file.
     * @return The last saved file.
     */
    public static File getSavedFile() {
        return lastFile;
    }

    /**
     * Delete the last saved file.
     * @return true if success, otherwise, false
     */
    public static boolean deleteLastFile() {
        if (lastFile == null) {
            return false;
        }
        boolean success = lastFile.delete();
        if (success) {
            lastFile = null;
        }
        return success;
    }

    /**
     * Checks if external storage is available for read and write
     * @return true if available, otherwise, false
     */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     * Checks if external storage is available for read
     * @return true if available, otherwise, false
     */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}