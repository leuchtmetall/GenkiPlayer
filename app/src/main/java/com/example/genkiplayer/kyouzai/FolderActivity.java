package com.example.genkiplayer.kyouzai;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.genkiplayer.TrackedActivity;
import com.example.genkiplayer.R;

public class FolderActivity extends TrackedActivity {
    private static final String TAG = "FOLDER ACTIVITY";
    public static final String PATH_EXTRA = "path";
    public static final String NAME_EXTRA = "name";

    public static final String PLAYLIST_EXTRA = "playlist";
    public static final String PLAYLIST_INDEX_EXTRA = "playlistIndex";

    public static Intent newIntent(Context context, String name, String path) {
        Bundle b = new Bundle();
        b.putString(FolderActivity.NAME_EXTRA, name);
        b.putString(FolderActivity.PATH_EXTRA, path);
        Intent intent = new Intent(context, FolderActivity.class);
        intent.putExtras(b);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setExitTransition(null);
        getWindow().setEnterTransition(null);
        setContentView(R.layout.activity_kyouzai_folder);

        Button backButton = (Button) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FolderActivity.this.finish();

            }
        });
        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        Bundle b = getIntent().getExtras();
        String path = b.getString(PATH_EXTRA);
        TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
        titleTextView.setText(b.getString(NAME_EXTRA));

        if (fragment == null) {
            fragment = BrowseFragment.newInstance(path);
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }
}
