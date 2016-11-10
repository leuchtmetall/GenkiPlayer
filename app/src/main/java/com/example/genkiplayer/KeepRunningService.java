package com.example.genkiplayer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class KeepRunningService extends Service {
    private static final String TAG = "KEEP RUNNING";

    private static final int INTERVAL = 1000; // poll every 3 secs

    private static boolean stopTask;
    private PowerManager.WakeLock mWakeLock;


    @Override
    public void onCreate() {
        super.onCreate();

        stopTask = false;

        mWakeLock = null;
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "a_tag");
        mWakeLock.acquire();

        final String appPackageName = getApplicationContext().getPackageName();
        final int[] stoppedCounter = {0};
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Start your (polling) task
        TimerTask task = new TimerTask() {
            @Override
            public void run() {

                // If you wish to stop the task/polling
                if (stopTask) {
                    this.cancel();
                }

                if(sharedPref.getBoolean("pref_keep_app_running", true)) {
                    // Check foreground app: If it is not in the foreground... bring it!
                    if(TrackedActivity.isEmpty()) {
                        stoppedCounter[0]++;
                    } else {
                        stoppedCounter[0] = 0;
                    }
                    if(stoppedCounter[0]> 1) {
                        Log.v(TAG, "will restart app now...");
                        Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(appPackageName);
                        startActivity(LaunchIntent);
                    }
                }
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, INTERVAL);
    }

    @Override
    public void onDestroy(){
        stopTask = true;
        if (mWakeLock != null)
            mWakeLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
