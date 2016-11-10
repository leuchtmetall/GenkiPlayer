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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.genkiplayer.R;
import com.example.genkiplayer.util.KeyEventHandler;
import com.example.genkiplayer.util.Toaster;

import java.util.ArrayList;



public class FlashcardFragment extends Fragment implements SlideSet.Callback, KeyEventHandler, ThumbnailFragment.OnSlideSelectedListener {
    private static final String ARG_PATHS = "paths";
    private static final String TAG = "Flashcard Fragment";

    private ArrayList<String> paths;
    private OnFragmentInteractionListener mListener;
    private FlashcardSet mFlashcardSet;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private TextView mLoadingStatusTextView;
    private int currentFlashcard = 0;
    private boolean didStartShowingSlides = false;
    private boolean flashcardsReady = false;
    private View mButtonBar;
    private Button mCorrectButton;
    private Toaster mToaster;

    public FlashcardFragment() {
    }

    public static FlashcardFragment newInstance(ArrayList<String> paths) {
        FlashcardFragment fragment = new FlashcardFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PATHS, paths);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            paths = getArguments().getStringArrayList(ARG_PATHS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_flashcard, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mImageView = (ImageView) view.findViewById(R.id.slideImageView);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mLoadingStatusTextView = (TextView) view.findViewById(R.id.loadingStatusTextView);
        mButtonBar = view.findViewById(R.id.buttonBar);
        mCorrectButton = (Button) view.findViewById(R.id.answerRightButton);
        Button answerWrongButton = (Button) view.findViewById(R.id.answerWrongButton);

        mCorrectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNext(true);
            }
        });
        answerWrongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNext(false);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mFlashcardSet = new FlashcardSet(paths, getView());
        mFlashcardSet.LoadDataAsync(this);
        mToaster = new Toaster(getActivity());
    }

    @Override
    public void dataReady(boolean withErrors) {
        flashcardsReady = true;

        if (!withErrors) {
            if(!isHidden()) {
                startFlashcards();
            }
            flashcardsReady = true;
        } else {
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
                } else {
                    if(view != null) {
                        getView().setBackgroundColor(Color.BLACK);
                    }
                    mProgressBar.setVisibility(View.GONE);
                    mLoadingStatusTextView.setVisibility(View.GONE);
                    mImageView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if(flashcardsReady) {
            startFlashcards();
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

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        boolean handled = false;

        Bitmap bitmap;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if(mState == State.FRONT) {
                    showBack();
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if(mState != State.BACK) {
                    mState = State.HISTORY;
                    bitmap = mFlashcardSet.getFromHistoryStack();
                    if(bitmap != null) {
                        Log.v(TAG, "show previous slide: " + bitmap);
                        showSlide(bitmap);
                    }
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if(mState == State.HISTORY) {
                    bitmap = mFlashcardSet.getFromFutureStack();
                    if(bitmap != null) {
                        Log.v(TAG, "show next slide: " + bitmap);
                        showSlide(bitmap);
                    } else {
                        mState = State.FRONT;
                        showFront();
                    }
                }
                break;
            case KeyEvent.KEYCODE_MENU:
                if(mState == State.FRONT) {
                    showThumbnails();
                }
        }
        return handled;
    }

    private void showThumbnails() {
        if(flashcardsReady) {
            FragmentManager fm = getFragmentManager();
            ThumbnailFragment playlistDialog = ThumbnailFragment.newInstance(mFlashcardSet, this, currentFlashcard);
            playlistDialog.show(fm, "CHOOSE_FROM_PLAYLIST_DIALOG");
        }
    }

    @Override
    public void onSlideSelected(int newSlide) {
        currentFlashcard = newSlide;
        showFront();
    }

    // Flashcard Lifecycle


    private void startFlashcards() {
        if(!didStartShowingSlides) {
            setIsLoading(false);
            currentFlashcard = 0;
            showFront();
            didStartShowingSlides = true;
        }
    }

    private enum State {
        FRONT, BACK, HISTORY
    }

    private State mState;

    public void showFront() {
        Log.v(TAG, "Showing Front " + currentFlashcard);
        Bitmap card = mFlashcardSet.getFront(currentFlashcard);
        showSlide(card);
        mState = State.FRONT;
    }

    public void showBack() {
        Log.v(TAG, "Showing Back " + currentFlashcard);
        Bitmap card = mFlashcardSet.getBack(currentFlashcard);
        showSlide(card);
        setButtonBarVisibility(true);
        mCorrectButton.requestFocus();
        mState = State.BACK;
    }

    public void showNext(boolean wasCorrect) {
        mFlashcardSet.copyToHistoryStack(currentFlashcard);
        if(wasCorrect) {
            mFlashcardSet.removeSlide(currentFlashcard);
        } else {
            currentFlashcard++;
        }
        if(mFlashcardSet.length() == 0) {
            showCompletion();
            return;
        }
        if(currentFlashcard >= mFlashcardSet.length()) {
            currentFlashcard = 0;
        }
        setButtonBarVisibility(false);
        showFront();
    }

    public void setButtonBarVisibility(final boolean willBeVisible) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mButtonBar.setVisibility(willBeVisible ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    public void showCompletion() {
        getActivity().finish();
    }

    public void showSlide(final Bitmap slide) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImageView.setImageBitmap(slide);
                }
            });
        }
    }

}
