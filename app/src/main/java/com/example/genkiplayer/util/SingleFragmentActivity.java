package com.example.genkiplayer.util;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.view.KeyEvent;

import com.example.genkiplayer.TrackedActivity;
import com.example.genkiplayer.R;

public abstract class SingleFragmentActivity extends TrackedActivity {
    protected abstract Fragment createFragment();
    protected abstract KeyEventHandler eventHandlerFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = createFragment();
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if(eventHandlerFragment() != null) {
            handled = eventHandlerFragment().onKeyDown(keyCode, event);
        }
        return handled || super.onKeyDown(keyCode, event);
    }


}
