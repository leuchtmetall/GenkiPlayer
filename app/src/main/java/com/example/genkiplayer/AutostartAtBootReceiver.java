package com.example.genkiplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class AutostartAtBootReceiver extends BroadcastReceiver {
    private static final String TAG = "AUTOSTART";
    public AutostartAtBootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean willAutostart = sharedPref.getBoolean("pref_autostart_on_boot", false);

        Log.i("BROADCAST RECEIVER", "Broadcast \"" + intent.getAction() + "\"");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "BOOT_COMPLETED received, will autostart: " + willAutostart);
            if (willAutostart) {
                Intent i = new Intent(context, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            }
        }
    }
}
