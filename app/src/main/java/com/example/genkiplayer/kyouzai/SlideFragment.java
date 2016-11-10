package com.example.genkiplayer.kyouzai;

import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.genkiplayer.R;
import com.example.genkiplayer.util.KeyEventHandler;
import com.example.genkiplayer.util.Toaster;

import java.util.ArrayList;


public class SlideFragment extends Fragment implements SlideSet.Callback, KeyEventHandler, ThumbnailFragment.OnSlideSelectedListener {
    private static final String ARG_PATHS = "paths";
    private static final String ARGS_AUTOPLAY = "autoplay";
    private static final String TAG = "Slide Fragment";

    private ArrayList<String> paths;
    private OnFragmentInteractionListener mListener;
    private SlideSet mSlideSet;
    private boolean isLoading;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private TextView mLoadingStatusTextView;
    private int currentSlide = 0;

    private boolean isRunningAutomatically = false;
    private int autoSlideInterval = 5000;
    private Toaster mToaster;

    public SlideFragment() {
    }

    public static SlideFragment newInstance(ArrayList<String> paths, boolean autoplay) {
        SlideFragment fragment = new SlideFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PATHS, paths);
        args.putBoolean(ARGS_AUTOPLAY, autoplay);
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            paths = getArguments().getStringArrayList(ARG_PATHS);
            isRunningAutomatically = getArguments().getBoolean(ARGS_AUTOPLAY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_slide, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mImageView = (ImageView) view.findViewById(R.id.slideImageView);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mLoadingStatusTextView = (TextView) view.findViewById(R.id.loadingStatusTextView);
    }

    @Override
    public void onStart() {
        super.onStart();
        mSlideSet = new SlideSet(paths, getView());
        this.isLoading = true;
        mSlideSet.LoadDataAsync(this);
    }

    @Override
    public void dataReady(boolean withErrors) {
        slidesReady = true;

        if (!withErrors) {
            if(!isHidden()) {
                startShowingSlides();
            }
            slidesReady = true;
        } else {
            // TODO implement
            mToaster.toast(getString(R.string.an_error_occured));
        }
    }

    @Override
    public void updateProgress(final int count, final int of) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String percentText = "" + (count * 100 / of) + "%";
                    mLoadingStatusTextView.setText(percentText);
                }
            });
        }
    }

    private boolean didStartShowingSlides = false;
    private boolean slidesReady = false;

    private void startShowingSlides() {
        if(!didStartShowingSlides) {
            setIsLoading(false);
            currentSlide = 0;
            showSlide();
            didStartShowingSlides = true;
        }

    }


    private void setPreviousSlide() {
        currentSlide--;
        if(currentSlide < 0) {
            currentSlide = mSlideSet.length() - 1;
        }
    }

    private void setNextSlide() {
        currentSlide++;
        if(currentSlide >= mSlideSet.length()) {
            currentSlide = 0;
        }
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        boolean handled = false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if(isRunningAutomatically) {
                    mToaster.toast(getString(R.string.media_toast_pause));
                    isRunningAutomatically = false;
                } else {
                    mToaster.toast(getString(R.string.media_toast_play));
                    isRunningAutomatically = true;
                }
                handled = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if(!isLoading) {
                    setPreviousSlide();
                    showSlide();
                }
                handled = true;
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if(!isLoading) {
                    setNextSlide();
                    showSlide();
                }
                handled = true;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                autoSlideInterval += 1000;
                mToaster.toast("" + (autoSlideInterval / 1000) + "s");
                handled = true;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if(autoSlideInterval > 1000) {
                    autoSlideInterval -= 1000;
                }
                mToaster.toast("" + (autoSlideInterval / 1000) + "s");
                handled = true;
                break;
            case KeyEvent.KEYCODE_MENU:
                showThumbnails();
        }
        return handled;
    }

    private void showThumbnails() {
        if(slidesReady) {
            FragmentManager fm = getFragmentManager();
            ThumbnailFragment playlistDialog = ThumbnailFragment.newInstance(mSlideSet, this, currentSlide);
            playlistDialog.show(fm, "CHOOSE_FROM_PLAYLIST_DIALOG");
        }
    }

    private void setIsLoading(final boolean valueToSetTo) {
        if(getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = getView();
                if (valueToSetTo) {
                    if(view != null) {
                        getView().setBackgroundColor(Color.WHITE);
                    }
                    mProgressBar.setVisibility(View.VISIBLE);
                    mLoadingStatusTextView.setVisibility(View.VISIBLE);
                    mImageView.setVisibility(View.GONE);
                    SlideFragment.this.isLoading = true;
                } else {
                    if(view != null) {
                        getView().setBackgroundColor(Color.BLACK);
                    }
                    mProgressBar.setVisibility(View.GONE);
                    mLoadingStatusTextView.setVisibility(View.GONE);
                    mImageView.setVisibility(View.VISIBLE);
                    SlideFragment.this.isLoading = false;
                }
            }
        });
    }

    public void showSlide() {
        Log.v(TAG, "Showing Slide " + currentSlide);
        final Bitmap slide = mSlideSet.getSlide(currentSlide);
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImageView.setImageBitmap(slide);
                }
            });
        }
    }

    private Thread presentationThread;

    @Override
    public void onResume() {
        super.onResume();
        if(slidesReady) {
            startShowingSlides();
        }
        mToaster = new Toaster(getActivity());
        presentationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean keepRunning = true;
                while (keepRunning) {
                    if (isRunningAutomatically && !isLoading) {
                        setNextSlide();
                        showSlide();
                    }
                    try {
                        Thread.sleep(autoSlideInterval);
                    } catch (InterruptedException e) {
                        keepRunning = false;
                    }
                }
            }
        });
        presentationThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        presentationThread.interrupt();
        presentationThread = null;
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
    public void onSlideSelected(int newSlide) {
        this.currentSlide = newSlide;
        this.showSlide();
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
