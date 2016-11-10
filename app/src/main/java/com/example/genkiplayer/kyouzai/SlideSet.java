package com.example.genkiplayer.kyouzai;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SlideSet {
    private static final String TAG = "SlideSet";
    private ArrayList<String> mPaths;
    private File[] localFiles;
    protected ArrayList<Pair<SlideSpan, Integer>> slides;
    private View mContainer;

    public SlideSet(ArrayList<String> paths, View container) {
        mPaths = paths;
        mContainer = container;
        slides = new ArrayList<>();
    }

    public int length() {
        return slides.size();
    }

    public void LoadDataAsync(final Callback callback) {
        localFiles = new File[mPaths.size()];
        final AtomicInteger filesLoaded  = new AtomicInteger(0);
        final AtomicInteger filesFinishedLoading = new AtomicInteger(0);
        final AtomicInteger errors  = new AtomicInteger(0);
        final Boolean[] hasChangedFiles = {false};
        for(int i = 0; i < mPaths.size(); i++) {
            String path = mPaths.get(i);
            final int index = i;
            Log.v(TAG, "Loading File " + (i+1) + " of " + mPaths.size());
            CacheManager.getInstance().getFile(path, new CacheManager.Callback() {

                @Override
                public void error(String description) {
                    Log.e(TAG, "error getting Slide data: " + description);
                    errors.incrementAndGet();
                }

                @Override
                public void gotFile(File file, boolean isFallback, boolean isEarlyCache, boolean gotNewData) {
                    if(isFallback || isEarlyCache || gotNewData) {
                        filesLoaded.incrementAndGet();
                    }

                    if(!isEarlyCache) {
                        filesFinishedLoading.incrementAndGet();
                    }

                    Log.i(TAG, "Got File " + filesLoaded.get() + " (" + filesFinishedLoading.get() + " complete) of " + mPaths.size());
                    if(gotNewData && localFiles[index] != null && !localFiles[index].equals(file)) {
                        hasChangedFiles[0] = true;
                    }
                    localFiles[index] = file;
                    callback.updateProgress(filesLoaded.get(), mPaths.size());

                    if((filesLoaded.get() + errors.get()) >= mPaths.size()) {
                        filesLoaded.set(0);
                        prepareData();
                        callback.dataReady(errors.get() > 0);
                    }
                    if(hasChangedFiles[0] && filesFinishedLoading.get() + errors.get() >= mPaths.size()) {
                        prepareData();
                        callback.dataReady(errors.get() > 0);
                    }
                }
            });
        }
    }

    private void prepareData() {

        for(int i = 0; i < localFiles.length; i++) {
            File f = localFiles[i];
            String name = Utils.filenameWithoutExtension(mPaths.get(i));
            try {
                name = URLDecoder.decode(name, "UTF-8");
            } catch (UnsupportedEncodingException e) { }
            switch (Utils.getFileExtension(f).toLowerCase()) {
                case "pdf":
                    PdfSlideSpan pdfSlideSpan = new PdfSlideSpan(f, name);
                    for(int j = 0; j < pdfSlideSpan.count(); j++) {
                        slides.add(new Pair<SlideSpan, Integer>(pdfSlideSpan, j));
                    }
                    break;
                default:
                    ImageSlideSpan imageSlideSpan = new ImageSlideSpan(f, name);
                    slides.add(new Pair<SlideSpan, Integer>(imageSlideSpan, 1));
            }
        }
    }

    public Bitmap getSlide(int slideNumber) {
        Pair<SlideSpan, Integer> file = slides.get(slideNumber);
        return file.first.getSlide(file.second);
    }

    public Bitmap getThumbnailSlide(int slideNumber) {
        Pair<SlideSpan, Integer> pair = slides.get(slideNumber);
        return getThumbnailSlideForPair(pair);
    }

    public Bitmap getThumbnailSlideForPair(Pair<SlideSpan, Integer> pair ) {
        File file = pair.first.getFile();
        File tempFile = MainConfig.getInstance().getAppContext().getCacheDir();
        tempFile = new File(tempFile, "thumbTemp" + Utils.filenameWithoutExtension(file.getAbsolutePath()) + "p" + pair.second + ".jpg");
        if(!tempFile.exists() || tempFile.lastModified() < file.lastModified()) {
            Bitmap bitmap = pair.first.getSlide(pair.second);
            int[] newSize = Utils.resizeToFitIntoBox(bitmap.getWidth(), bitmap.getHeight(), 400, 300);
            bitmap = Bitmap.createScaledBitmap(bitmap, newSize[Utils.WIDTH], newSize[Utils.HEIGHT], false);
            try(FileOutputStream out = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return BitmapFactory.decodeFile(tempFile.getAbsolutePath());
    }

    public String getSlideName(int slideNumber) {
        Pair<SlideSpan, Integer> file = slides.get(slideNumber);
        return file.first.getName();
    }

    public Pair<SlideSpan, Integer> getRawSlide(int index) {
        return slides.get(index);
    }

    public void removeSlide(int index) {
        slides.remove(index);
    }

    protected abstract class SlideSpan {
        File file;
        String name;

        public SlideSpan(File f, String name) {
            this.file = f;
            this.name = name;
        }

        abstract int count();
        abstract Bitmap getSlide(int slideNumber);
        public String getName() { return name; }
        public File getFile() { return file; }
    }

    protected class PdfSlideSpan extends SlideSpan {
        PdfRenderer renderer;

        public PdfSlideSpan(File f, String name) {
            super(f, name);
            ParcelFileDescriptor fd;
            try {
                fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                renderer = new PdfRenderer(fd);
            } catch (IOException e) {
                renderer = null;
                e.printStackTrace();
            }
        }

        @Override
        int count() {
            return renderer.getPageCount();
        }

        @Override
        Bitmap getSlide(int slideNumber) {
            if(renderer == null) {
                return null;
            } else {
                return Utils.renderPage(renderer, slideNumber, mContainer);
            }
        }
    }

    protected class ImageSlideSpan extends SlideSpan {
//        byte[] data;
        public ImageSlideSpan(File f, String name) {
            super(f, name);
//            try {
//                data = FileUtils.readFileToByteArray(f);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        int count() {
            return 1;
        }

        @Override
        Bitmap getSlide(int slideNumber) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
//            return BitmapFactory.decodeByteArray(data, 0, data.length);
        }
    }



    public interface Callback {
        void dataReady(boolean withErrors);
        void updateProgress(int count, int of);
    }
}
