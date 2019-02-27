package com.kirosc.wifilogger.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kirosc.wifilogger.ScanResults;

/**
 * Used to convert ScanResults between JSON String.
 */
public class JsonUtils {
    private static GsonBuilder builder = new GsonBuilder();
    private static Gson gson = builder.create();

    /**
     * Convert a ScanResults to a JSON String.
     * @param results The .
     * @return The JSON String.
     */
    public static String toJson(ScanResults results) {
        return gson.toJson(results);
    }

    /**
     * Convert a JSON String to a ScanResults.
     * @param json The JSON String.
     * @return The ScanResults.
     */
    public static ScanResults toScanResults(String json) {
        try {
            return gson.fromJson(json, ScanResults.class);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}