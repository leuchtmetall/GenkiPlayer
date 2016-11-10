package com.example.genkiplayer.kyouzai;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.genkiplayer.R;
import com.example.genkiplayer.util.KeyEventHandler;
import com.semantive.waveformandroid.waveform.soundfile.CheapSoundFile;
import com.semantive.waveformandroid.waveform.view.WaveformView;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class AudioFragment extends Fragment implements KeyEventHandler {
    private static final String ARG_NAME = "name";
    private static final String ARG_PATH = "path";
    private static final String TAG = "Audio Fragment";

    private String name;
    private String path;

    private File audioFile;

    private OnFragmentInteractionListener mListener;
    private MediaPlayer mMediaPlayer;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private TextView mDurationText;
    private TextView mMessageTextView;
    private WaveformView mWaveformView;
    private CheapSoundFile mSoundFile;
    private boolean ready = false;
    protected Handler mHandler;


    public AudioFragment() { }

    public static AudioFragment newInstance(String name, String path) {
        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_PATH, path);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        if (getArguments() != null) {
            name = getArguments().getString(ARG_NAME);
            path = getArguments().getString(ARG_PATH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kyouzai_audio, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mMessageTextView = (TextView) view.findViewById(R.id.messageText);
        TextView textView = (TextView) view.findViewById(R.id.titleTextView);
        textView.setText(name);
        mWaveformView = (WaveformView) view.findViewById(R.id.waveform);
        mProgressText = (TextView) view.findViewById(R.id.progressText);
        mDurationText = (TextView) view.findViewById(R.id.durationText);
    }

    @Override
    public void onStart() {
        super.onStart();
        mProgressBar.setVisibility(View.VISIBLE);
        CacheManager.getInstance().getFile(path, new CacheManager.Callback() {
            @Override
            public void error(String description) {
                // TODO handle error
            }

            @Override
            public void gotFile(final File file, boolean isFallback, boolean isEarlyCache, boolean gotNewData) {
                Log.i(TAG, "Got Audio File: " + file.getAbsolutePath() + (isEarlyCache ? " EARLY_CACHE " : "") + (gotNewData ? " NEW_DATA" : "") + (isFallback ? " FALLBACK" : ""));
                if(isFallback || isEarlyCache || gotNewData) { // all cases except when download deemed unnecessary
                    audioFile = file;
                    preparePlayback();
                    setWaveform();
                }
            }
        });
    }

    private void setWaveform() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSoundFile = CheapSoundFile.create(audioFile.getAbsolutePath(), new CheapSoundFile.ProgressListener() {
                        @Override
                        public boolean reportProgress(double v) {
                            return true;
                        }
                    });
                    if(mSoundFile != null) { // if audio file format supported
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mWaveformView.setSoundFile(mSoundFile);
                                mWaveformView.invalidate();
                            }
                        });


                    } else {
                        // TODO fallback
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    @Override
    public void onStop() {
        super.onStop();
        if(mUpdateSeekBarThread != null) {
            mUpdateSeekBarThread.interrupt();
            try {
                mUpdateSeekBarThread.join(200);
            } catch (InterruptedException e) { }
        }
        mUpdateSeekBarThread = null;
        if(mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        mMediaPlayer = null;

    }

    Thread mUpdateSeekBarThread = null;
    private void preparePlayback() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer mp) {
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setVisibility(View.GONE);
                                mProgressText.setText(R.string.zeroTime);
                                int duration = mp.getDuration() / 1000;
                                mDurationText.setText(String.format(Locale.getDefault(), "%02d:%02d", duration / 60, duration% 60));
                            }
                        });
                        ready = true;
                        mUpdateSeekBarThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                boolean keepRunning = true;
                                while(keepRunning) {
                                    if(mp != null && mp.isPlaying()) {
                                        updateWaveform(mp.getCurrentPosition());
                                    }
                                    try {
                                        Thread.sleep(30);
                                    } catch (InterruptedException e) { keepRunning = false;}
                                }
                            }
                        });
                        mUpdateSeekBarThread.start();
//                        mp.start();
                    }
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    if(getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mProgressBar.setVisibility(View.GONE);
                                mMessageTextView.setText(R.string.error_could_not_play_audio_file);
                                mMessageTextView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                    return false;
                }
            });
            mMediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void playPause() {
        if(ready) {
            if(mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            } else {
                mMediaPlayer.start();
            }
        }
    }

    public void seek(int millis) {
        if(ready) {
            int pos = mMediaPlayer.getCurrentPosition() + millis;
            if(pos < 0) {
                pos = 0;
            } else if (pos > mMediaPlayer.getDuration()) {
                pos = mMediaPlayer.getDuration();
            }
            mMediaPlayer.seekTo(pos);
            updateWaveform(pos);
        }
    }

    int mPreviousPos;
    public void updateWaveform(int pos) {
        if(pos != mPreviousPos) {
            Log.v(TAG, "set Position to " + pos);
            int posInSeconds = pos / 1000;
            final String progressText = String.format(Locale.getDefault(), "%02d:%02d", posInSeconds / 60, posInSeconds % 60);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressText.setText(progressText);
                }
            });
            if(mSoundFile != null) {
                int frames = mWaveformView.millisecsToPixels(pos);
                mWaveformView.setPlayback(frames);
                mWaveformView.postInvalidate();
                mPreviousPos = pos;
            }

        }
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                playPause();
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seek(-2000);
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seek(2000);
                break;
        }


        return false;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
