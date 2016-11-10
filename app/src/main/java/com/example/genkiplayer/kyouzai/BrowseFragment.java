package com.example.genkiplayer.kyouzai;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.R;
import com.example.genkiplayer.util.Toaster;
import com.example.genkiplayer.util.Utils;


public class BrowseFragment extends Fragment implements BrowseItem.Callback {
    private static final String TAG = "BROWSE FRAGMENT";
    private static final String ARG_PATH = "path";

    private String mPath;
    private BrowseItem mItem;

    private GridView mGridView;
    private OnFragmentInteractionListener mListener;
    private MainConfig config;
    private Toaster mToaster;

    private ProgressBar getProgressBar() {
        return (ProgressBar) getActivity().findViewById(R.id.progressBar);
    }

    public BrowseFragment() {
    }

    public static BrowseFragment newInstance(String path) {
        BrowseFragment fragment = new BrowseFragment();
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
        config = MainConfig.getInstance();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.browse_fragment, container, false);
        mGridView = (GridView) v.findViewById(R.id.gridview);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                BrowseItem item = mItem.getContents().get(position);

                if (item.getKind() == BrowseItem.Kind.AUDIO) {
                    if (item.getFiles().size() < 1) {
                        mToaster.toast(getString(R.string.noContentError));
                    } else if (item.getFiles().size() == 1) {
                        startAudioActivity(item, 0);
                    } else {
                        FragmentManager fm = getFragmentManager();
                        PlaylistFragment playlistDialog = PlaylistFragment.newInstance(item.getFiles(), item.getPathUrl(), item.getKind());
                        playlistDialog.show(fm, "CHOOSE_AUDIO_FROM_PLAYLIST_DIALOG");
                    }
                } else if (item.getKind() == BrowseItem.Kind.VIDEO) {
                    if (item.getFiles().size() < 1) {
                        mToaster.toast(getString(R.string.noContentError));
                    } else if (item.getFiles().size() == 1) {
                        startVideoActivity(item, 0);
                    } else {
                        FragmentManager fm = getFragmentManager();
                        PlaylistFragment playlistDialog = PlaylistFragment.newInstance(item.getFiles(), item.getPathUrl(), item.getKind());
                        playlistDialog.show(fm, "CHOOSE_VIDEO_FROM_PLAYLIST_DIALOG");
                    }
                } else if (item.getKind() == BrowseItem.Kind.SLIDESHOW) {
                    if (item.getFiles().size() < 1) {
                        mToaster.toast(getString(R.string.noContentError));
                    } else {
                        startSlideActivity(item, true);
                    }
                } else if (item.getKind() == BrowseItem.Kind.PRESENTATION) {
                    if (item.getFiles().size() < 1) {
                        mToaster.toast(getString(R.string.noContentError));
                    } else {
                        startSlideActivity(item, false);
                    }
                } else if (item.getKind() == BrowseItem.Kind.FLASHCARDS) {
                    if (item.getFiles().size() < 1) {
                        mToaster.toast(getString(R.string.noContentError));
                    } else {
                        startFlashcardActivity(item);
                    }
                } else if (item.getKind() == BrowseItem.Kind.FOLDER) {
                    startFolderActivity(item);
                } else {
                    mToaster.toast("" + position);
                }
            }
        });

        mItem = new BrowseItem(mPath);
        setAdapter();

        return v;
    }


    private void startFlashcardActivity(BrowseItem item) {
        Intent i = FlashcardActivity.newIntent(getActivity(), item.getFileUrlPaths());
        startActivity(i);
    }

    private void startSlideActivity(BrowseItem item, boolean autoplay) {
        Intent i = SlideActivity.newIntent(getActivity(), item.getFileUrlPaths(), autoplay);
        startActivity(i);
    }

    private void startVideoActivity(BrowseItem item, int index) {
        String url = config.getServerBaseDataURL() + item.getFileUrlPaths().get(index);
        Intent i = VideoActivity.newIntent(getActivity(), url);
        startActivity(i);
    }

    private void startAudioActivity(BrowseItem item, int index) {
        String path = item.getFileUrlPaths().get(index);
        String name = Utils.filenameWithoutExtension(item.getFiles().get(index));
        Intent i = AudioActivity.newIntent(getActivity(), name, path);
        startActivity(i);
    }

    private void startFolderActivity(BrowseItem item) {
        Intent i = FolderActivity.newIntent(getActivity(), item.getName(), item.getPathUrl());
        startActivity(i);
    }

    private void setAdapter() {
        mGridView.setAdapter(new BaseAdapter() {
            @Override  public int getCount() { return mItem.getContents().size(); }
            @Override  public Object getItem(int position) { return null; }
            @Override  public long getItemId(int position) { return 0; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                BrowseItem item = mItem.getContents().get(position);
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if(convertView == null) {
                    convertView = inflater.inflate(R.layout.browse_element, parent, false);
                }
                ImageView imageView = (ImageView) convertView.findViewById(R.id.itemImage);
                item.setIconToImageView(imageView, BrowseFragment.this);
                TextView textView = (TextView) convertView.findViewById(R.id.itemText);
                textView.setText(item.getName());
                return convertView;
            }
        });
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
    public void onResume() {
        super.onResume();
        mToaster = new Toaster(getActivity());
        mItem.getDataAsync(this);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }


    // Browse Item Callbacks

    @Override
    public void onDownloadStart() {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getProgressBar() != null) {
                        getProgressBar().setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    @Override
    public void onDownloadComplete(final boolean newData) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(getProgressBar()!= null) {
                        getProgressBar().setVisibility(View.INVISIBLE);
                    }
                    if(newData) {
                        int columns = 5;
                        switch(mItem.getContents().size()) {
                            case 1:
                                columns = 1;
                                break;
                            case 2:
                            case 4:
                                columns = 2;
                                break;
                            case 3:
                            case 5:
                            case 6:
                            case 7:
                            case 9:
                                columns = 3;
                                break;
                            case 8:
                            case 10:
                            case 11:
                            case 12:
                                columns = 4;
                        }
                        mGridView.setNumColumns(columns);
                        setAdapter();
                        mGridView.requestFocus();
                    }
                }
            });
        }
    }

    @Override
    public void onImageDownloadComplete(final ImageView view, final Bitmap bitmap) {
        if(getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.setImageBitmap(bitmap);
                }
            });
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }
}
