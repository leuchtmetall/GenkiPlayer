package com.example.genkiplayer.kyouzai;

import android.content.Context;
import android.util.Log;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.util.DownloadHelper;
import com.example.genkiplayer.util.Utils;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheManager {
    private static final long CACHE_THRESHOLD = 1024 * 1024 * 500; // 500 MB
    private static final String TAG = "CACHE MANAGER";
    private static CacheManager ourInstance = new CacheManager();
    public static CacheManager getInstance() {
        return ourInstance;
    }

    private File dbFile;
    private File mCachePath;
    private MainConfig config;
    private ConcurrentHashMap<String, String> files;
    private boolean cleanupInProgress = false;
    private int cleanupCountdown = 0;

    private CacheManager() {
        config = MainConfig.getInstance();
        getFromDisk();
        checkFreeSpaceAsync();
    }

    public boolean clearCache() {
        try {
            for(File f : getCacheDir().listFiles()) {
                f.delete();
            }
            files = new ConcurrentHashMap<>();
            writeToDisk();
            return true;
        } catch (NoAppContextException e) {
            return false;
        }
    }

    private AtomicInteger runningDownloads = new AtomicInteger(0);
    private static int MAXIMUM_SIMULTANEOUS_DOWNLOADS = 12;

    public Thread getFile(final String path, final Callback callback) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "starting download. " + runningDownloads.get() + " downloads currently running.");

                File cachePath;
                try {
                    cachePath = getCacheDir();
                } catch (NoAppContextException e) {
                    e.printStackTrace();
                    callback.error("No App Context");
                    return;
                }
                boolean fileExists = false;
                boolean needsDownload = false;
                String filename = files.get(path);
                File theFile;

                if(filename == null) {
                    Log.d(TAG, "File '" + path + "' not in DB");
                    needsDownload = true;
                    try {
                        do {
                            filename = "data" + UUID.randomUUID().toString() + "." + Utils.getFileExtension(new File(path));
                            theFile = new File(cachePath, filename);
                        } while(theFile.exists());
                        theFile.createNewFile();
                        Log.d(TAG, "created File: " + filename);
                    } catch (IOException e) {
                        callback.error("Could not create temp file");
                        return;
                    }
                } else {
                    theFile = new File(cachePath, filename);
                    if(theFile.exists()) {
                        Log.d(TAG, "File '" + path + "'in DB and exists");
                        fileExists = true;
                        if(config.getServerIp() == null) {
                            callback.gotFile(theFile, true, false, false);
                            return;
                        } else {
                            callback.gotFile(theFile, false, true, false);
                        }
                    } else {
                        Log.d(TAG, "File in DB but does not exists on disk");
                        needsDownload = true;
                        files.remove(path);
                        writeToDisk();
                    }
                }


                while(runningDownloads.get() > MAXIMUM_SIMULTANEOUS_DOWNLOADS) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runningDownloads.incrementAndGet();



                while(config.getServerIp() == null) {
                    Log.v(TAG, "No server found (yet). sleeping...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        callback.error("Interrupted while waiting for server response.");
                        runningDownloads.decrementAndGet();
                        return;
                    }
                }

                String basePath = config.getServerBaseDataURL();
                DownloadHelper downloadHelper;
                try {
                    downloadHelper = new DownloadHelper(basePath + path);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    callback.error("Malformed URL Exception");
                    runningDownloads.decrementAndGet();
                    return;
                }


                if(fileExists) {
                    try {
                        needsDownload = (downloadHelper.getLastModified() != theFile.lastModified());
                    } catch (IOException e) {
                        Log.e(TAG, "IOException " + e.getMessage() + ", returning cached file " + theFile.getAbsolutePath());
                        callback.gotFile(theFile, true, false, false);
                        downloadHelper.close();
                        runningDownloads.decrementAndGet();
                        return;
                    }
                }

                if(needsDownload) {
                    try {
                        Log.d(TAG, "Download to File " + theFile.getAbsolutePath());
                        File tempFile = File.createTempFile("tmp", Utils.getFileExtension(theFile), getCacheDir());
                        downloadHelper.downloadToFile(tempFile);
                        tempFile.renameTo(theFile);
                        files.put(path, theFile.getName());
                        writeToDisk();
                        callback.gotFile(theFile, false, false, true);
                        checkFreeSpace();
                    } catch (IOException e) {
                        e.printStackTrace();
                        callback.error("IOException while downloading");
                    } catch (NoAppContextException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "returning cached file: " + theFile.getAbsolutePath());
                    callback.gotFile(theFile, false, false, false);
                }
                downloadHelper.close();
                runningDownloads.decrementAndGet();
            }
        });
        t.start();
        return t;
    }

    public interface Callback {
        void error(String description);
        void gotFile(File file, boolean isFallback, boolean isEarlyCache, boolean gotNewData);
    }

    private void checkFreeSpaceAsync() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                checkFreeSpace();
            }
        });
        t.start();
    }

    private void checkFreeSpace() {
        if(!cleanupInProgress && cleanupCountdown % 10 == 0) {
            cleanupCountdown += 1;
            cleanupInProgress = true;
            try {
                File[] files = getCacheDir().listFiles();
                Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
                int i = 0;
                while (Utils.getFreeStorageSpace() < CACHE_THRESHOLD && i < files.length) {
                    Log.i(TAG, "deleting  cached File " + files[i].getName() + ", modified: " + Utils.formatDate(files[i].lastModified()));
                    files[i].delete();
                    i++;
                }

            } catch (NoAppContextException e) {
                e.printStackTrace();
            }
            cleanupInProgress = false;
        }

    }

    private File getDbFile() throws NoAppContextException {
        if(dbFile == null) {
            Context appContext = config.getAppContext();
            if(appContext == null) {
                throw new NoAppContextException();
            }
            File dir = appContext.getDir("fileCacheList", Context.MODE_PRIVATE);
            dbFile = new File(dir, "cachedFiles.json");
        }
        return dbFile;
    }

    private File getCacheDir() throws NoAppContextException {
        if(mCachePath == null) {
            Context appContext = config.getAppContext();
            if(appContext == null) {
                throw new NoAppContextException();
            }
            mCachePath = appContext.getCacheDir();
            mCachePath = appContext.getDir("data_cache", Context.MODE_PRIVATE);
        }
        return mCachePath;
    }

    synchronized private void writeToDisk() {
        JSONObject jsonObject = new JSONObject(files);
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getDbFile()), "utf-8"))) {
            writer.write(jsonObject.toString());
            Log.v(TAG, "Cache Data: " + jsonObject.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoAppContextException e) {
            e.printStackTrace();
        }
    }

    synchronized private void getFromDisk() {
        try {
            String json = Utils.readTextFile(getDbFile());
            ConcurrentHashMap<String, String> newFiles = new ConcurrentHashMap<>();
            if (getDbFile().exists()) {
                JSONObject jsonObject = new JSONObject(json);
                Iterator<String> iterator = jsonObject.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String filename = jsonObject.getString(key);
                    newFiles.put(key, filename);
                }
            }
            files = newFiles;
        } catch (NoAppContextException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            clearCache();
            e.printStackTrace();
        }
    }

    private class NoAppContextException extends Exception {}
}
