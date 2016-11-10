package com.example.genkiplayer.contentdisplay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.R;
import com.example.genkiplayer.VideoVLCActivity;

import java.io.File;

public class ContentActivity extends VideoVLCActivity {
    private static final String TAG = "CONTENT ACTIVITY";
    private DisplayRunnable mDisplayRunnable = new DisplayRunnable(this);
    private DownloadRunnable mDownloadRunnable = new DownloadRunnable(this);
    private WebserverRunnable mWebserverRunnable = new WebserverRunnable(this);
    private Thread displayThread;
    private Thread downloadThread;
    private Thread webserverThread;
    private MainConfig config = MainConfig.getInstance();
    public volatile boolean runBackgroundThreads = true;
    private PowerManager.WakeLock mWakeLock;
    private ImageView mImageView;
    private FrameLayout mVideoView;
    private boolean videoRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_content);

        mImageView = (ImageView) findViewById(R.id.image_view);
        mVideoView = (FrameLayout) findViewById(R.id.video_view);
    }

    @Override
    public void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        config.setAppContext(getApplicationContext());
        displayThread = new Thread(mDisplayRunnable);
        downloadThread = new Thread(mDownloadRunnable);
        webserverThread = new Thread(mWebserverRunnable);
        this.runBackgroundThreads = true;
        displayThread.start();
        downloadThread.start();
        webserverThread.start();

        // set wake lock
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        cleanup();
    }

    private void cleanup() {
        this.runBackgroundThreads = false;
        if(downloadThread != null) {
            downloadThread.interrupt();
            downloadThread = null;
        }
        if(displayThread != null) {
            displayThread.interrupt();
            displayThread = null;
        }
        if(webserverThread != null) {
            webserverThread.interrupt();
            webserverThread = null;
        }

    }

    public DisplayRunnable getDisplayRunnable() {
        return mDisplayRunnable;
    }


    public void showSlide(Bitmap bitmap) {
        mImageView.setImageBitmap(bitmap);
        mImageView.invalidate();
    }

    public View getContainer() {
        return findViewById(R.id.container);
    }

    public void startVideo(final String path, boolean force) {
        if(force) {
            stopVideo();
        }
        if(!videoRunning) {
            Log.d(TAG, "startVideo() called while not running. " + path + ", force: " + force);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImageView.setVisibility(View.GONE);
                    mVideoView.setVisibility(View.VISIBLE);
                    startVLCVideoFile(path, true);
                    videoRunning = true;
                }
            });
        }
    }

    public void stopVideo() {
        stopVLCVideo();
        if(videoRunning) {
            Log.d(TAG, "stopVideo() called while videoRunning was true");
            videoRunning = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mVideoView.setVisibility(View.GONE);
                    mImageView.setVisibility(View.VISIBLE);
                }
            });

        }
    }


    public File regularContentPath() {
        return getDir(config.getRegularContentPath(), Activity.MODE_PRIVATE);
    }

    public File regularContentTempPath() {
        return getDir(config.getRegularContentTempPath(), Activity.MODE_PRIVATE);
    }
}
