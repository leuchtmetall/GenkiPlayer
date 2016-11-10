package com.example.genkiplayer;

import android.app.Activity;

import java.util.HashSet;

public abstract class TrackedActivity extends Activity {

    private static final HashSet<Activity> activities = new HashSet<>();

    @Override
    protected void onStart() {
        add(this);
        super.onStart();
//        Log.i("TrackedActivity", "..... onStart() called, activities in Set: " + TrackedActivity.count());
    }

    @Override
    protected void onStop() {
        super.onStop();
        remove(this);
//        Log.i("TrackedActivity", "..... onStop() called, activities in Set: " + TrackedActivity.count());
    }

    public static void add(Activity activity) {
        synchronized (activities) {
            activities.add(activity);
        }
    }

    public static void remove(Activity activity) {
        synchronized (activities) {
            if (activities.contains(activity)) {
                activities.remove(activity);
            }
        }
    }

    public static boolean isEmpty() {
        return activities.isEmpty();
    }

    public static int count() {
        return activities.size();
    }
}
