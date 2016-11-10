package com.example.genkiplayer.kyouzai;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.R;
import com.example.genkiplayer.util.DownloadHelper;
import com.example.genkiplayer.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class BrowseItem {
    private static final String TAG = "BROWSE ITEM";
    private String basePath;
    private String path = null;
    private String name;
    private String imagePath;
    private Kind kind;
    private ArrayList<BrowseItem> contents;
    private ArrayList<String> files;
    private String mJson;
    private boolean mImageIsBeingLoaded = false;

    private final MainConfig config = MainConfig.getInstance();

    public BrowseItem(String path) {
        contents = new ArrayList<>();
        this.path = path;
    }

    public BrowseItem(String basePath, String name, String imagePath, Kind kind, ArrayList<String> files) {
        this.basePath = basePath;
        this.name = name;
        this.imagePath = imagePath.equals("null") ? null : imagePath;
        this.kind = kind;
        this.files = files;
    }

    public BrowseItem(String jsonString, boolean a) {
        mJson = jsonString;
        parseJSON(jsonString);
    }

    public File getMetadataCachefilePath() {
        return getCachefilePathForFilename("metadata.json");
    }

    public File getImageCachefilePath(String extension) {
        return getCachefilePathForFilename("metadataImage." + extension);
    }

    public File getCachefilePathForFilename(String filename) {
        File dir = config.getAppContext().getDir("browseCache", Activity.MODE_PRIVATE); // TODO NullPointer
        File f = new File(dir, getUnescapedPath());
        f.mkdirs();
        return new File(f, filename);
    }

    public boolean getCachedData() {
        if(mJson == null) {
            Log.v(TAG, "reading from cache file: " + getMetadataCachefilePath());
            final String cachedMetadata = Utils.readTextFile(getMetadataCachefilePath());
            Log.v(TAG, "cached JSON: " + cachedMetadata);
            if(cachedMetadata != null && cachedMetadata.length() > 0) {
                mJson = cachedMetadata;
                parseJSON(mJson);
            }
            return true;
        } return false;
    }

    public boolean setJson(String json) {
        if(json != null && !json.equals(mJson)) {
            Log.v(TAG, "new Data");
            File f = getMetadataCachefilePath();
            try(Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"))) {
                mJson = json;
                parseJSON(mJson);
                writer.write(mJson);
            } catch (IOException ioe) {
                Log.e(TAG, "IOException:", ioe);
            }
            return true;
        } else {
            Log.v(TAG, "NO new Data");
            return false;
        }
    }

    private Bitmap getBitmapForFile(Context c, File f) {
        final float scale = c.getResources().getDisplayMetrics().density;
//        Log.v(TAG, "scale: " + scale);
        // scale is 2 on Fire TV 2nd Gen.
        //noinspection UnnecessaryLocalVariable
        Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
        return bitmap;
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(f.getAbsolutePath(), options);
//        int imageHeight = options.outHeight;
//        int imageWidth = options.outWidth;
//        String imageType = options.outMimeType;
        // TODO scale to appropriate size

    }

    private final LinkedList<ImageView> imageViews = new LinkedList<>();

    private boolean isDownloading = false;


    public void setIconToImageView(ImageView view, final Callback callback) {
        if(kind != null) {
            kind.setDefaultImage(view);
        }

        if(this.imagePath != null && this.imagePath.length() > 0) {
            synchronized (imageViews) {
                imageViews.add(view);
            }
            if(!isDownloading) {
                isDownloading = true;
                CacheManager.getInstance().getFile(getPathUrl() + "/" + imagePath, new CacheManager.Callback() {

                    @Override
                    public void error(String description) {
                        Log.i(TAG, "error getting image data: " + description);
                        isDownloading = false;
                    }

                    @Override
                    public void gotFile(File file, boolean isFallback, boolean isEarly, boolean gotNewData) {
                        Bitmap bitmap = getBitmapForFile(callback.getContext(), file);
                        synchronized (imageViews) {
                            for (ImageView imageView: imageViews) {
                                callback.onImageDownloadComplete(imageView, bitmap);
                            }
                            if(!isEarly) {
                                imageViews.clear();
                                isDownloading = false;
                            }
                        }
                    }
                });
            }
        }
    }

    public void getDataAsync(final Callback callback) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                callback.onDownloadComplete(getCachedData());
                callback.onDownloadStart();
                int keepTrying = 5;
                while(keepTrying > 0) {
                    keepTrying--;
                    try {
                        if(config.getServerIp() != null) {
                            String url = MainConfig.getInstance().getServerBaseDataURL() + path;
                            DownloadHelper dh = new DownloadHelper(url, true);
                            String json = dh.downloadToString();
                            Log.d(TAG, "Got Data: " + json);
                            callback.onDownloadComplete(setJson(json));
                            keepTrying = 0;
                        } else {
                            Log.i(TAG, "no server found (yet). retrying in 3 seconds... (" + keepTrying + " more)");
                        }
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "Malformed URL Exception");
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.e(TAG, "IO Exception");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        keepTrying = 0;
                    }
                }
            }
        });
        t.start();
    }

    public void parseJSON(String json) {
        try {
            JSONObject o = new JSONObject(json);
            this.name = o.getString("name");
            this.imagePath = o.getString("image");
            this.kind = Kind.byString(o.getString("kind"));
            JSONArray contents = o.getJSONArray("contents");
            this.contents = new ArrayList<>(contents.length());
            for (int i = 0; i < contents.length(); i++) {
                JSONObject jo = contents.getJSONObject(i);

                String cName = jo.getString("name");
                String cImagePath = jo.getString("image");
                Kind cKind = Kind.byString(jo.getString("kind"));
                String cBasePath = this.path;

                ArrayList<String> cFiles = null;
                if(jo.has("contents")) {
                    JSONArray files = jo.getJSONArray("contents");

                    cFiles = new ArrayList<>(files.length());
                    for (int j = 0; j < files.length(); j++) {
                        cFiles.add(files.getString(j));
                    }
                }
                BrowseItem bi = new BrowseItem(cBasePath, cName, cImagePath, cKind, cFiles);
                this.contents.add(bi);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public String getName() {
        return name;
    }

    public List<BrowseItem> getContents() {
        return contents;
    }

    public Kind getKind() {
        return kind;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getPathUrl() {
        return basePath + "/" + Uri.encode(name);
    }

    public String getUnescapedPath() {
        String p = (path != null) ? path : (basePath + "/" + name);
        String[] components = p.split("/");
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<components.length; i++) {
            if(i != 0) {
                sb.append("/");
            }
            sb.append(Uri.decode(components[i]));
        }
        return sb.toString();
    }

    public String getPath() {
        if(path != null) {
            return path;
        } else {
            return basePath + "/" + name;
        }
    }

    public String getJson() {
        return mJson;
    }

    public ArrayList<String> getFiles() {
        return files;
    }

    public ArrayList<String> getFileUrlPaths() {
        ArrayList<String> paths = new ArrayList<>(files.size());
        for(String file : files) {
            paths.add(getPathUrl() + "/" + Uri.encode(file));
        }
        return paths;
    }

    public enum Kind {
        AUDIO("audio") {
            @Override
            public void setDefaultImage(ImageView view) {
                view.setImageResource(R.drawable.kyouzai_default_icon_audio);
            }
        },
        VIDEO("video") {
            @Override
            public void setDefaultImage(ImageView view) {
                view.setImageResource(R.drawable.kyouzai_default_icon_video);
            }
        },
        SLIDESHOW("slideshow") {
            @Override
            public void setDefaultImage(ImageView view) {
                view.setImageResource(R.drawable.kyouzai_default_icon_slideshow);
            }
        },
        PRESENTATION("presentation") {
            @Override
            public void setDefaultImage(ImageView view) {
                view.setImageResource(R.drawable.kyouzai_default_icon_presentation);
            }
        },
        FLASHCARDS("flashcards") {
            @Override
            public void setDefaultImage(ImageView view) {
                view.setImageResource(R.drawable.kyouzai_default_icon_flashcards);
            }
        },
        FOLDER("folder") {
            @Override
            public void setDefaultImage(ImageView view) {
                view.setImageResource(R.drawable.kyouzai_default_icon_folder);
            }
        };

        private final String name;
        Kind(String name) {
            this.name = name;
        }

        public boolean is(String s) {
            return this.name.equals(s);
        }

        public static Kind byString(String s) {
            for(Kind k : Kind.values()) {
                if(k.is(s)) {
                    return k;
                }
            }
            return null;
        }

        public abstract void setDefaultImage(ImageView view);
    }

    public interface Callback {
        void onDownloadStart();
        void onDownloadComplete(boolean newData);
        void onImageDownloadComplete(ImageView view, Bitmap bitmap);
        Context getContext();
    }
}
