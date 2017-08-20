package com.example.genkiplayer.kyouzai;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.example.genkiplayer.util.KeyEventHandler;
import com.example.genkiplayer.util.SingleFragmentActivity;

import java.util.ArrayList;

public class AudioActivity extends SingleFragmentActivity {
    private KeyEventHandler eventHandler;

    public static Intent newIntent(Context context, String path, ArrayList<String> playlist, int playlistIndex) {
        Bundle b = new Bundle();
        b.putString(FolderActivity.PATH_EXTRA, path);
        b.putStringArrayList(FolderActivity.PLAYLIST_EXTRA, playlist);
        b.putInt(FolderActivity.PLAYLIST_INDEX_EXTRA, playlistIndex);

        Intent intent = new Intent(context, AudioActivity.class);
        intent.putExtras(b);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras();
        String path = bundle.getString(FolderActivity.PATH_EXTRA);
        ArrayList<String> playlist = bundle.getStringArrayList(FolderActivity.PLAYLIST_EXTRA);
        int playlistIndex = bundle.getInt(FolderActivity.PLAYLIST_INDEX_EXTRA);
        AudioFragment fragment = AudioFragment.newInstance(path, playlist, playlistIndex);
        eventHandler = fragment;
        return fragment;
    }

    @Override
    protected KeyEventHandler eventHandlerFragment() {
        return eventHandler;
    }
}
