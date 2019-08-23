package com.kirosc.wifilogger.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.kirosc.wifilogger.data.ScanResults



class JsonUtils {
    companion object {
        private val builder = GsonBuilder()
        private val gson = builder.create()


        /**
         * Convert a ScanResults to a JSON String.
         * @param results The ScanResults.
         * @return The JSON String.
         */
        fun toJson(results: ScanResults): String {
            return gson.toJson(results)
        }

        /**
         * Convert a JSON String to a ScanResults.
         * @param json The JSON String.
         * @return The ScanResults.
         */
        fun toScanResults(json: String): ScanResults? {
            return try {
                gson.fromJson(json, ScanResults::class.java)
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
                null
            }

        }
    }
}