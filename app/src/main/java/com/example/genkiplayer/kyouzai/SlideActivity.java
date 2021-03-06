package com.example.genkiplayer.kyouzai;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.genkiplayer.util.KeyEventHandler;
import com.example.genkiplayer.util.SingleFragmentActivity;

import java.util.ArrayList;

public class SlideActivity extends SingleFragmentActivity {
    private static final String AUTOPLAY_EXTRA = "autoplay";
    private static final String PATHS_EXTRA = "paths";
    KeyEventHandler eventHandler;

    public static Intent newIntent(Context context, ArrayList<String> paths, boolean autoplay) {
        Bundle b = new Bundle();
        b.putBoolean(AUTOPLAY_EXTRA, autoplay);
        b.putStringArrayList(PATHS_EXTRA, paths);
        Intent intent = new Intent(context, SlideActivity.class);
        intent.putExtras(b);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras();
        boolean autoplay = bundle.getBoolean(AUTOPLAY_EXTRA);
        ArrayList<String> paths = bundle.getStringArrayList(PATHS_EXTRA);
        SlideFragment fragment = SlideFragment.newInstance(paths, autoplay);
        eventHandler = fragment;
        return fragment;
    }

    @Override
    protected KeyEventHandler eventHandlerFragment() {
        return eventHandler;
    }
}
