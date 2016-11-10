package com.example.genkiplayer.kyouzai;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.genkiplayer.VLCFragment;
import com.example.genkiplayer.util.KeyEventHandler;
import com.example.genkiplayer.util.SingleFragmentActivity;

public class VideoActivity extends SingleFragmentActivity {
    private static final String TAG = "VIDEO ACTIVITY";
    private KeyEventHandler eventHandler;

    public static Intent newIntent(Context context, String path) {
        Bundle b = new Bundle();
        b.putString(FolderActivity.PATH_EXTRA, path);
        Intent intent = new Intent(context, VideoActivity.class);
        intent.putExtras(b);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras();
        String path = bundle.getString(FolderActivity.PATH_EXTRA);
        VLCFragment fragment = VLCFragment.newInstance(path);
        eventHandler = fragment;
        return fragment;
    }

    @Override
    protected KeyEventHandler eventHandlerFragment() {
        return eventHandler;
    }
}
