package com.example.genkiplayer.kyouzai;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.R;
import com.example.genkiplayer.util.Utils;

import java.util.ArrayList;

public class PlaylistFragment extends DialogFragment {
    private static final String TAG = "BROWSE FRAGMENT";
    private static final String ARG_FILES = "files";
    private static final String ARG_PATH = "path";
    private static final String ARG_KIND = "kind";

    private ArrayList<String> mFiles;
    private String mPath;
    private BrowseItem.Kind mKind;
    private MainConfig config;

    private OnFragmentInteractionListener mListener;

    public PlaylistFragment() {
    }

    public static PlaylistFragment newInstance(ArrayList<String> files, String path, BrowseItem.Kind kind) {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_FILES, files);
        args.putString(ARG_PATH, path);
        args.putSerializable(ARG_KIND, kind);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFiles = getArguments().getStringArrayList(ARG_FILES);
            mPath = getArguments().getString(ARG_PATH);
            mKind = (BrowseItem.Kind) getArguments().getSerializable(ARG_KIND);
        }
        config = MainConfig.getInstance();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_kyouzai_playlist, container, false);
        GridView gridView = (GridView) v.findViewById(R.id.gridview);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
//                dismiss();
                if (mKind == BrowseItem.Kind.VIDEO) {
                    startVideoActivity(position);
                } else if (mKind == BrowseItem.Kind.AUDIO) {
                    startAudioActivity(position);
                }
            }
        });
        gridView.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return mFiles.size();
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
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = new TextView(getActivity());
                String filename = Utils.filenameWithoutExtension(mFiles.get(position));
                tv.setText(filename);
                tv.setPadding(7, 2, 7, 2);

                return tv;
            }
        });
        return v;
    }

    private void startAudioActivity(int index) {
        String path = mPath;
        Intent i = AudioActivity.newIntent(getActivity(), path, mFiles, index);
        startActivity(i);
    }

    private void startVideoActivity(int index) {
        String url = config.getServerBaseDataURL() + mPath + "/" + Uri.encode(mFiles.get(index));
        Intent i = VideoActivity.newIntent(getActivity(), url);
        startActivity(i);
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
}
