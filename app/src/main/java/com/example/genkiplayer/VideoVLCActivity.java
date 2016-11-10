package com.example.genkiplayer;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

import com.example.genkiplayer.util.Toaster;
import com.example.genkiplayer.util.Utils;
import com.example.genkiplayer.util.VlcPlayerState;

import java.io.File;

public abstract class VideoVLCActivity extends TrackedActivity implements IVideoPlayer {
    private static final String TAG = "VIDEO BASE ACTIVITY";

    private SurfaceView mSurfaceView;
    private Surface mSurface = null;
    private View mVideoView;

    private LibVLC mLibVLC = null;
    private Thread movieFinishedWatcher = null;

    private Toaster mToaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToaster = new Toaster(this);
        Log.d(TAG, "VideoVLC -- onCreate -- START ------------");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView = findViewById(R.id.video_view);
        mSurfaceView = (SurfaceView) findViewById(R.id.player_surface);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        mSurface = surfaceHolder.getSurface();
    }

    protected void startVLCVideoFile(final String path, boolean loop) {
        if(!new File(path).exists()) {
            return;
        }
        Log.d(TAG, "play MRL " + path);
        final String movieUrlString = "file://" + path;
        startVLCVideoFromPath(movieUrlString, loop);
    }

    protected void startVLCVideoFromPath(final String path, boolean loop) {
        stopVLCVideo();
        try {
            mLibVLC = new LibVLC();
            mLibVLC.setAout(LibVLC.AOUT_AUDIOTRACK);
            mLibVLC.setVout(LibVLC.VOUT_ANDROID_SURFACE);
            mLibVLC.setHardwareAcceleration(LibVLC.HW_ACCELERATION_FULL);

            mLibVLC.init(getApplicationContext());
        } catch (LibVlcException e){
            Log.e(TAG, e.toString());
        }

        mLibVLC.attachSurface(mSurface, VideoVLCActivity.this);

        mLibVLC.playMRL(path);

        if(loop) {
            movieFinishedWatcher = new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean running = true;
                    while (running) {
                        try {
                            if (mLibVLC.getPlayerState() == VlcPlayerState.ENDED) {
                                Log.d(TAG, "reached end of movie");
                                mLibVLC.playMRL(path);
                                Thread.sleep(2000);
                            } else {
                                Thread.sleep(100);
                            }

                        } catch (InterruptedException e) {
                            running = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            movieFinishedWatcher.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopVLCVideo();
    }

    protected void stopVLCVideo() {
        if(mLibVLC != null) {
            if(movieFinishedWatcher != null) {
                movieFinishedWatcher.interrupt();
            }
            movieFinishedWatcher = null;
            mLibVLC.stop();
            mLibVLC.destroy();
            mLibVLC = null;
        }
    }

    public void eventHardwareAccelerationError() {
        Log.e(TAG, "eventHardwareAccelerationError()!");
    }

    @Override
    public void setSurfaceLayout(final int width, final int height, int visible_width, int visible_height, final int sar_num, int sar_den){
        Log.d(TAG, "setSurfaceSize -- START. width: " + width + " height: " + height + " vis_width: " +
                visible_width+ " vis_height: " + visible_height+ " sar_num: " + sar_num + " sar_den: " + sar_den);
        if (width * height == 0)
            return;

        final int[] d = Utils.getDimensions(width, height, mVideoView);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(d[Utils.WIDTH], d[Utils.HEIGHT]));
                mSurfaceView.invalidate();
                if (mLibVLC != null) {
                    mLibVLC.detachSurface();
                    mLibVLC.attachSurface(mSurface, VideoVLCActivity.this);
                }
            }
        });
    }

    @Override
    public int configureSurface(android.view.Surface surface, int i, int i1, int i2){
        return -1;
    }

    public void playOrPauseVideo() {
        if(mLibVLC != null) {
            if (mLibVLC.getPlayerState() == VlcPlayerState.PAUSED) {
                mLibVLC.play();
                mToaster.toast(getString(R.string.media_toast_play));
            } else if (mLibVLC.getPlayerState() == VlcPlayerState.PLAYING) {
                mLibVLC.pause();
                mToaster.toast(getString(R.string.media_toast_pause));

            }
        }
    }

    public void seekDelta(int delta) {
        if(mLibVLC.getLength() <= 0 || !mLibVLC.isSeekable()) return;

        long position = mLibVLC.getTime() + delta;
        if (position < 0) position = 0;
        mLibVLC.setTime(position);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        boolean handled = false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                playOrPauseVideo();
                handled = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seekDelta(-10000);
                mToaster.toast(getString(R.string.media_toast_rewind));
                handled = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seekDelta(10000);
                mToaster.toast(getString(R.string.media_toast_fast_forward));
                handled = true;
                break;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

}

