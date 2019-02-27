package com.kirosc.wifilogger;

/**
 * Updated by Kiros Choi on 2018/04/22.
 */
public class WiFi {

    public static final int OPEN = 0;
    public static final int WEP = 1;
    public static final int WPA = 2;
    public static final int WPA2 = 3;

    /**
     * The access point MAC address
     */
    private String bSSID;

    /**
     * The access point Name
     */
    private String sSID;

    /**
     * The access point adopted security protocol
     */
    private String protocol;

    /**
     * The access point detected signal level in dBm, also known as the RSSI.
     */
    private int level;

    /**
     * The constructor that used to initialize all the fields of the class.
     */
    WiFi(String bSSID, String sSID, int level, int encryptionMode) {
        this.bSSID = bSSID;
        this.sSID = sSID;
        this.level = level;

        switch (encryptionMode) {
            case OPEN:
                protocol = "OPEN";
                break;
            case WEP:
                protocol = "WEP";
                break;
            case WPA:
                protocol = "WPA";
                break;
            case WPA2:
                protocol = "WPA2";
                break;
        }
    }

    /**
     *  Get the BSSID
     * @return The BSSID
     */
    public String getbSSID() {
        return bSSID;
    }

    /**
     *  Get the SSID
     * @return The SSID
     */
    public String getsSID() {
        return sSID;
    }

    /**
     *  Get the signal strength in dBm
     * @return The signal strength in dBm
     */
    public int getLevel() {
        return level;
    }

    /**
     *  Get the encryption protocol used
     * @return The encryption protocol
     */
    public String getProtocol() {
        return protocol;
    }
}
