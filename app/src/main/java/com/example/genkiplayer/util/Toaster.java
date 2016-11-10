package com.example.genkiplayer.util;

import android.content.Context;
import android.widget.Toast;

public class Toaster {
    private Toast currentToast = null;
    private final Context context;
    public Toaster(Context c) {
        context = c;
    }

    public void toast(String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        if(currentToast != null) {
            currentToast.cancel();
        }
        toast.show();
        currentToast = toast;
    }
}
