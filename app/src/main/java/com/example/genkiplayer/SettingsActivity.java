package com.example.genkiplayer;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.example.genkiplayer.kyouzai.CacheManager;
import com.example.genkiplayer.util.Utils;

public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = "SETTINGS ACTIVITY";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            Preference button = findPreference("pref_clear_caches");
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    long before = Utils.getFreeStorageSpace();
                    boolean success = CacheManager.getInstance().clearCache();
                    long after = Utils.getFreeStorageSpace();
                    if(success) {
                        long freedKilobytes = (after - before) / 1024;
                        String freedMemoryText = freedKilobytes > 10000 ? (freedKilobytes / 1024) + "MB" : freedKilobytes + "kB";
                        String toastText = "Cache cleared. freed memory: " + freedMemoryText;
                        Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "No success", Toast.LENGTH_SHORT).show();
                    }
                    return success;
                }
            });
        }
    }

    @Override
    protected void onStart() {
        TrackedActivity.add(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        TrackedActivity.remove(this);
    }
}





