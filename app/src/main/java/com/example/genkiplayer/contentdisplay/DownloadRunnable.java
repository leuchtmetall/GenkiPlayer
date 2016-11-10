package com.example.genkiplayer.contentdisplay;

import android.util.Log;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.util.DownloadHelper;
import com.example.genkiplayer.util.SlideshowTimings;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;

public class DownloadRunnable implements Runnable {
    private static final String TAG = "DOWNLOAD RUNNABLE";
    private ContentActivity activity;
    private MainConfig config = MainConfig.getInstance();


    private int nextDownloadCountdown = 0;
    public DownloadRunnable(ContentActivity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {
        Log.i(TAG, "Starting...");
        while(activity.runBackgroundThreads) {
            // check for new content every minute
            if(config.getNeedsReload()) {
                nextDownloadCountdown = 0;
            }
            if(nextDownloadCountdown <= 0) {
                nextDownloadCountdown = 30;
                if (getContentFromServer()) {
                    Log.i(TAG, "got new content from server");
                    selectContentForDisplay();
                }
            }
            nextDownloadCountdown--;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
        }
        Log.i(TAG, "Stopping...");
    }

    private synchronized boolean getContentFromServer() {
        if(config.getServerIp() == null) {
            nextDownloadCountdown = 2;
            return false;
        }
        boolean needsForceDownload = config.needsForceReload();

            try {
                String[] formats = config.getContentDisplayFormats();
                DownloadHelper[] dlHelpers = new DownloadHelper[formats.length];
                int i = 0;
                for (String format: config.getContentDisplayFormats()) {
                    String remoteUrl = config.getServerURL(format);
                    if (remoteUrl == null) {
                        Log.i(TAG, "Remote URL was null");
                        continue;
                    }
                    dlHelpers[i++] = new DownloadHelper(remoteUrl, needsForceDownload, format);
                }
                // find newest file and close all others
                DownloadHelper newestHelper = null;
                for(DownloadHelper dh: dlHelpers) {
                    if (newestHelper == null) {
                        newestHelper = dh;
                    } else if (dh.getLastModified() > newestHelper.getLastModified()) {
                        newestHelper.close();
                        newestHelper = dh;
                    } else {
                        dh.close();
                    }
                }

                Log.v(TAG, "Last Modified: Local : " + config.getLastUpdate());
                Log.v(TAG, "Last Modified: Server: " + newestHelper.getLastModified());
//                if(config.getLastUpdate() < newestHelper.getLastModified()) { // newer than existing
                if(config.getLastUpdate() != newestHelper.getLastModified()) { // different from existing

                    File tempFile = new File(activity.regularContentTempPath(), "content." + newestHelper.getFormat());
                    if(newestHelper.downloadToFile(tempFile)) {
                        // get new timings from server
                        DownloadHelper timingsDownload = new DownloadHelper(config.getServerURL("txt"));
                        String newTimings = timingsDownload.downloadToString();
                        if(newTimings != null) {
                            config.setSlideshowTimings(new SlideshowTimings(newTimings));
                        }
                        config.setLastUpdate(newestHelper.getLastModified());
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    newestHelper.close();
                }
            } catch (MalformedURLException mue) {
                Log.e(TAG, "malformed url error", mue);
                mue.printStackTrace();
            } catch (IOException ioe) {
                if(ioe instanceof ConnectException) {
                    Log.e(TAG, ioe.getMessage());
                } else {
                    Log.e(TAG, "IO Exception", ioe);
                }
            } catch (SecurityException se) {
                Log.e(TAG, "security error", se);
            } catch (Exception e) {
                e.printStackTrace();
            }
        return false;
    }

    private void selectContentForDisplay() {
        Log.v(TAG, "selectContentForDisplay() called");
        File f = null;
        for(String format: config.getContentDisplayFormats()) {
            File file = new File(activity.regularContentTempPath(), "content." + format);
            if(file.exists() && (f == null || f.lastModified() < file.lastModified())) {
                if(f != null) {
                    Log.d(TAG, "f.lastModified: " + f.lastModified());
                    Log.d(TAG, "g.lastModified: " + file.lastModified());
                    Log.i(TAG, "Delete temp File " + f.getName() + ", success: " + f.delete());
                }
                f = file;
            }
        }
        if(f != null) {
            Log.i(TAG, "setting new content: " + f.toString());
            activity.getDisplayRunnable().setNewContent(f);
        }
    }
}
