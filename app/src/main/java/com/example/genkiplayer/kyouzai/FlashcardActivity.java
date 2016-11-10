package com.example.genkiplayer.kyouzai;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.genkiplayer.util.KeyEventHandler;
import com.example.genkiplayer.util.SingleFragmentActivity;

import java.util.ArrayList;


public class FlashcardActivity extends SingleFragmentActivity {
    private static final String PATHS_EXTRA = "paths";
    KeyEventHandler eventHandler;

    public static Intent newIntent(Context context, ArrayList<String> paths) {
        Bundle b = new Bundle();
        b.putStringArrayList(PATHS_EXTRA, paths);
        Intent intent = new Intent(context, FlashcardActivity.class);
        intent.putExtras(b);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras();
        ArrayList<String> paths = bundle.getStringArrayList(PATHS_EXTRA);
        FlashcardFragment fragment = FlashcardFragment.newInstance(paths);
        eventHandler = fragment;
        return fragment;
    }

    @Override
    protected KeyEventHandler eventHandlerFragment() {
        return eventHandler;
    }
}
