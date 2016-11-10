package com.example.genkiplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.genkiplayer.contentdisplay.ContentActivity;
import com.example.genkiplayer.kyouzai.BrowseFragment;
import com.example.genkiplayer.kyouzai.CacheManager;
import com.example.genkiplayer.util.Utils;

public class MainActivity extends TrackedActivity {
    private static final String TAG = "MAIN ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setExitTransition(null);
        getWindow().setEnterTransition(null);
        MainConfig.getInstance().setAppContext(getApplicationContext());
        CacheManager.getInstance(); // init after context is set for main config.
        setContentView(R.layout.activity_main);

        Button mContentButton = (Button) findViewById(R.id.content_button);
        mContentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ContentActivity.class);
                startActivity(i);
            }
        });

        Button mPreferencesButton = (Button) findViewById(R.id.preferences_button);
        mPreferencesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(i);
            }
        });

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        startDisplayActivityIfRequested();
        startNetworkDiscovery();
        startKeepRunningService();

        Log.i(TAG, "Free Storage: " + (Utils.getFreeStorageSpace() / 1024 / 1024) + "MB");
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainConfig.getInstance().setAppContext(getApplicationContext());

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment == null) {
            fragment = BrowseFragment.newInstance("");
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    private void startDisplayActivityIfRequested() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        if(sharedPref.getBoolean("pref_launch_into_content_display", false)) {
            Intent i = new Intent(MainActivity.this, ContentActivity.class);
            startActivity(i);
        }
    }

    private void startNetworkDiscovery() {
        Log.d(TAG, "starting network discovery service");
        Intent intent = new Intent(this, ZeroconfDiscoverService.class);
        startService(intent);
    }

    private void startKeepRunningService() {
        Log.d(TAG, "starting service to keep app running");
        Intent intent = new Intent(this, KeepRunningService.class);
        startService(intent);
    }
}
