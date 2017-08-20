package com.example.genkiplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.genkiplayer.util.ChallengeResponse;
import com.example.genkiplayer.util.SlideshowTimings;
import com.example.genkiplayer.util.Utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainConfig extends AppSecrets {
    // singleton
    private static final MainConfig ourInstance = new MainConfig();
    public static MainConfig getInstance() {
        return ourInstance;
    }
    private MainConfig() {
    }

    // private constants
    private static final boolean DEVELOPMENT_MODE = false;
    private static final String TAG = "MAIN CONFIG";
    private static final String SERVICE_TYPE_PROD = "genkiserver";
    private static final String SERVICE_TYPE_DEV = "genkiserverdev";
    private static final String CONTENT_SERVICE_TYPE_PROD = "genkicontent";
    private static final String CONTENT_SERVICE_TYPE_DEV = "genkicontentdev";
    private static final String regularContentPath = "content_display_files";
    private static final String regularContentTempPath = "content_display_files_temp";
    private static final String[] contentDisplayFormats = {"pdf", "m4v"};

    // fields
    private boolean mNeedsReload;
    private String serverIP = null;
    private String serverPort = "3000";
    private Context appContext;
    private boolean forceReload = false;
    private long lastUpdate = 0;
    private SlideshowTimings slideshowTimings = new SlideshowTimings();

    public String getServiceType() {
        return "_" + (DEVELOPMENT_MODE ? SERVICE_TYPE_DEV : SERVICE_TYPE_PROD) + "._tcp.";
    }

    public String getContentServiceType() {
        return "_" + (DEVELOPMENT_MODE ? CONTENT_SERVICE_TYPE_DEV : CONTENT_SERVICE_TYPE_PROD) + "._tcp.";
    }

    public String getDeviceTokenSecret() {
        return deviceTokenSecret;
    }

    public boolean needsForceReload() {
        if(forceReload) {
            forceReload = false;
            return true;
        }
        return false;
    }

    public ChallengeResponse generateNewChallengeResponse() {
        return new ChallengeResponse(serverAuthSecretPre, serverAuthSecretPost);
    }

    public void setForceReload() {
        forceReload = true;
    }

    public void setAppContext(Context appContext) {
        if(appContext != null) {
            this.appContext = appContext;
            if (this.browseCacheDir == null) {
                File temp = appContext.getDir("browseCache", Activity.MODE_PRIVATE);
                this.browseCacheDir = temp;
            }
        } else {
            try {
                throw new Exception("Trying to set App Context to null!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private File browseCacheDir = null;
    public File getBrowseCacheDir() {
        if(browseCacheDir == null) {
            browseCacheDir = new File("");
            browseCacheDir = appContext.getDir("browseCache", Activity.MODE_PRIVATE);
        }
        return browseCacheDir;
    }

    // TODO remember and try to reuse last valid server
    public String getServerIp() {
        if(useCustomServerAddress()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
            return sharedPref.getString("pref_server_ip", "");
        }
        return serverIP;
    }

    public String getServerPort() {
        if(useCustomServerAddress()) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
            return sharedPref.getString("pref_server_port", "");
        }
        return serverPort;
    }

    private boolean useCustomServerAddress() {
        if(appContext != null) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
            return sharedPref.getBoolean("pref_use_custom_server_address", false);
        }
        return false;
    }

    public void setServerIP(String newIP) {
        if(newIP == null) {
            this.serverIP = null;
            Log.d(TAG, "Server lost. set new IP to null.");
        } else if(!newIP.equals(this.serverIP)) {
            this.serverIP = newIP;
            this.setNeedsReload();
            Log.d(TAG, "new IP is " + newIP);
        }
    }

    public void setServerPort(int port) {
        if(port != 0) {
            this.serverPort = "" + port;
        }
    }

    public String getServerURL(String format) throws Exception {
        if(getServerIp() == null || getServerPort() == null) {
            return null;
        }
        return getServerBaseURL() + "/datac/" + getSanitizedDeviceName() + "/content." + format
                + "?token=" + Utils.getToken();
    }

    public String getServerBaseURL() {
        return "http://" + getServerIp() + ":" + getServerPort();
    }

    public String getServerBaseDataURL() {
        return "http://" + getServerIp() + ":" + getServerPort() + "/data";
    }

    public String getSanitizedDeviceName() throws Exception {
        return getSanitizedDeviceName(true);
    }

    public String getSanitizedDeviceName(boolean urlEncode) throws Exception {
        String deviceName;
        if(appContext == null) {
            throw new Exception("No App Context");
        }
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        deviceName = sharedPref.getString("pref_device_name", "Fire TV");
        try {
            String spacelessName = deviceName.replace(' ', '_');
            return urlEncode ? URLEncoder.encode(spacelessName, "UTF-8") : spacelessName;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException in getServerURL(); Falling back to String");
            return deviceName;
        }
    }

    public boolean getNeedsReload() {
        boolean oldValue = mNeedsReload;
        mNeedsReload = false;
        return oldValue;
    }

    public void setNeedsReload() {
        mNeedsReload = true;
    }

    public String getRegularContentPath() {
        return MainConfig.regularContentPath;
    }

    public String getRegularContentTempPath() {
        return MainConfig.regularContentTempPath;
    }

    public String[] getContentDisplayFormats() {
        return MainConfig.contentDisplayFormats;
    }


    public long getLastUpdate() {
        return lastUpdate;
    }

    public void resetLastUpdate() { lastUpdate = 0; }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public SlideshowTimings getSlideshowTimings() {
        return slideshowTimings;
    }

    public void setSlideshowTimings(SlideshowTimings slideshowTimings) {
        this.slideshowTimings = slideshowTimings;
    }

    public Context getAppContext() {
            return appContext;
    }
}
