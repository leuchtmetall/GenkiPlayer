package com.example.genkiplayer.kyouzai;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.example.genkiplayer.R;
import com.example.genkiplayer.util.Utils;

public class ThumbnailFragment extends DialogFragment {
    private static final String TAG = "THUMBNAIL FRAGMENT";
    private static final String ARG_SELECTION = "SELECTED_ITEM";

    private SlideSet mSlideSet;
    private GridView mGridView;
    private int initialSelection;

    private OnSlideSelectedListener mListener;

    public ThumbnailFragment() {
    }

    public static ThumbnailFragment newInstance(SlideSet slideSet, OnSlideSelectedListener callback, int selectedItem) {
        ThumbnailFragment fragment = new ThumbnailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SELECTION, selectedItem);
        fragment.setArguments(args);
        fragment.mSlideSet = slideSet;
        fragment.mListener = callback;
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (getArguments() != null) {
            initialSelection = getArguments().getInt(ARG_SELECTION);
        }
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_kyouzai_thumbnails, container, false);
        mGridView = (GridView) v.findViewById(R.id.gridview);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                mListener.onSlideSelected(position);
                dismiss();
            }
        });
        mGridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return mSlideSet.length();
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                final TextView tv = new TextView(getActivity());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bitmap = mSlideSet.getThumbnailSlide(position);
                        final Drawable drawable = new BitmapDrawable(getActivity().getResources(), bitmap);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
                            }
                        });
                    }
                }).start();

                String filename = Utils.filenameWithoutExtension(mSlideSet.getSlideName(position));
                tv.setText(filename);
                tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                tv.setHeight(370);
                tv.setPadding(30, 10, 30, 10);

                return tv;
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mGridView.setSelection(initialSelection);
                //Do something after 100ms
            }
        }, 100);


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSlideSelectedListener) {
            mListener = (OnSlideSelectedListener) context;
        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnSlideSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(1760, 1060);
    }

    public interface OnSlideSelectedListener {
        void onSlideSelected(int newSlide);
    }

}
