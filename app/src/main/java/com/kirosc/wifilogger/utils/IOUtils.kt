package com.kirosc.wifilogger.utils

import android.content.ContentResolver
import android.content.Intent
import android.os.Environment
import com.kirosc.wifilogger.data.ScanResults
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment.MEDIA_MOUNTED_READ_ONLY
import android.os.Environment.MEDIA_MOUNTED
import com.google.common.io.ByteStreams
import java.io.*


class IOUtils {
    companion object {
        lateinit var lastFile: File

        /**
         * Save the ScanResults in the device's external storage.
         *
         * @param scanResults The ScanResults that is going to save to the device
         * @return true if success, otherwise, false
         */
        fun saveFile(scanResults: ScanResults): Boolean {
            // Get the time and append to the file name
            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss")
            val formattedTime = formatter.format(Calendar.getInstance().time)
            val fileName = "WiFiLogger_$formattedTime.json"

            if (isExternalStorageWritable()) {
                // Folder name
                val foler = "WiFi Logger"

                // File that points to the saving folder
                var file =
                    File(Environment.getExternalStorageDirectory().toString() + "/" + foler)

                if (!file.exists()) {
                    if (!file.mkdir()) {
                        return false
                    }
                }

                // Saving process
                try {
                    file = File(file, fileName)
                    lastFile = file
                    val stream = FileOutputStream(file)
                    try {
                        stream.write(JsonUtils.toJson(scanResults).toByteArray())
                    } finally {
                        stream.close()
                    }
                } catch (e: IOException) {
                    return false
                }
                return true

            } else {
                return false
            }
        }

        /**
         * Open a previously saved file.
         * @param resolver The Content Resolver.
         * @param data  The Intent that contains the Uri or Path that points to the file.
         * @return  The content of the JSON formatted file in String or null if failed to retrieve the file.
         */
        fun openFile(resolver: ContentResolver, data: Intent): String? {
            if (isExternalStorageReadable()) {
                try {
                    // For using normal file manager that provides Content Provider
                    // Get the InputStream from the Content Provider
                    val stream = resolver.openInputStream(data.data!!)
                    // Change the InputStream to String
                    return String(ByteStreams.toByteArray(stream!!))
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()

                    try {
                        // For using other file managers that provide storage path
                        val file = File(data.data!!.path)
                        // Get the InputStream from the given path
                        val stream = FileInputStream(file)

                        return String(ByteStreams.toByteArray(stream))
                    } catch (e1: IOException) {
                        e1.printStackTrace()
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
            return null
        }

        /**
         * Checks if external storage is available for read and write
         * @return true if available, otherwise, false
         */
        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return MEDIA_MOUNTED == state
        }

        /**
         * Checks if external storage is available for read
         * @return true if available, otherwise, false
         */
        fun isExternalStorageReadable(): Boolean {
            val state = Environment.getExternalStorageState()
            return MEDIA_MOUNTED == state ||    MEDIA_MOUNTED_READ_ONLY == state
        }
    }


}