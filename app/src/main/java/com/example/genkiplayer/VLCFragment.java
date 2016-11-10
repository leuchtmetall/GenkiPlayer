package com.example.genkiplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.genkiplayer.util.KeyEventHandler;
import com.example.genkiplayer.util.Toaster;
import com.example.genkiplayer.util.Utils;
import com.example.genkiplayer.util.VlcPlayerState;

import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VLCFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VLCFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VLCFragment extends Fragment implements IVideoPlayer, KeyEventHandler {

    private static final String TAG = "VLC FRAGMENT";
    private static final String ARG_PATH = "path";

    private SurfaceView mSurfaceView;
    private Surface mSurface = null;
    private View mVideoView;

    private LibVLC mLibVLC = null;
    private Thread movieFinishedWatcher = null;
    private Toaster mToaster;
    private String mPath;

    private OnFragmentInteractionListener mListener;

    public VLCFragment() {}

    public static VLCFragment newInstance(String path) {
        VLCFragment fragment = new VLCFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, path);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPath = getArguments().getString(ARG_PATH);
        }
        mToaster = new Toaster(getActivity());
        Log.d(TAG, "VideoVLC -- onCreate -- START ------------");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_vlc, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mVideoView = view.findViewById(R.id.video_view);
        mSurfaceView = (SurfaceView) view.findViewById(R.id.player_surface);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        mSurface = surfaceHolder.getSurface();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
        }
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

            mLibVLC.init(getActivity());
        } catch (LibVlcException e){
            Log.e(TAG, e.toString());
        }

        mLibVLC.attachSurface(mSurface, VLCFragment.this);

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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        startVLCVideoFromPath(mPath, false);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopVLCVideo();
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
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

        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(d[Utils.WIDTH], d[Utils.HEIGHT]));
                    mSurfaceView.invalidate();
                    if (mLibVLC != null) {
                        mLibVLC.detachSurface();
                        mLibVLC.attachSurface(mSurface, VLCFragment.this);
                    }
                }
            });
        }
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
                seekDelta(-5000);
                mToaster.toast(getString(R.string.media_toast_rewind));
                handled = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seekDelta(5000);
                mToaster.toast(getString(R.string.media_toast_fast_forward));
                handled = true;
                break;
        }
        return handled;
    }

}
