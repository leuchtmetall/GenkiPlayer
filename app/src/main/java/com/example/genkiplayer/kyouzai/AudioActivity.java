package com.example.genkiplayer.kyouzai;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.genkiplayer.util.KeyEventHandler;
import com.example.genkiplayer.util.SingleFragmentActivity;

public class AudioActivity extends SingleFragmentActivity {
    private KeyEventHandler eventHandler;

    public static Intent newIntent(Context context, String name, String path) {
        Bundle b = new Bundle();
        b.putString(FolderActivity.NAME_EXTRA, name);
        b.putString(FolderActivity.PATH_EXTRA, path);
        Intent intent = new Intent(context, AudioActivity.class);
        intent.putExtras(b);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras();
        String name = bundle.getString(FolderActivity.NAME_EXTRA);
        String path = bundle.getString(FolderActivity.PATH_EXTRA);
        AudioFragment fragment = AudioFragment.newInstance(name, path);
        eventHandler = fragment;
        return fragment;
    }

    @Override
    protected KeyEventHandler eventHandlerFragment() {
        return eventHandler;
    }
}
