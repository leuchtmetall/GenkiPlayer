package com.example.genkiplayer.contentdisplay;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.util.Utils;

import java.io.File;
import java.io.FilenameFilter;

public class DisplayRunnable implements Runnable {
    private static final String TAG = "DISPLAY_RUNNABLE";
    private final ContentActivity activity;
    private int currentPage = -1;
    private File fileToDisplay = null;
    private File newContent = null;
    private int displayCountdown;
    private final MainConfig config = MainConfig.getInstance();
    private File deleteExcept;

    public DisplayRunnable(ContentActivity contentActivity) {
        this.activity = contentActivity;
    }

    @Override
    public void run() {
        while(activity.runBackgroundThreads) {
            boolean hasNewContent = (newContent != null);
            fileToDisplay = getFileToDisplay();
            if(fileToDisplay == null || !fileToDisplay.exists() || fileToDisplay.length() == 0) {
                config.setForceReload();
            } else if ("pdf".equals(Utils.getFileExtension(fileToDisplay))) { // pdf
                activity.stopVideo();
                displaySlideshow();
            } else if ("m4v".equals(Utils.getFileExtension(fileToDisplay))) { // m4v
                displayVideo(hasNewContent);
            }
            deleteUnusedFilesIfNecessary();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        Log.i(TAG, "Stopping...");
    }



    private void deleteUnusedFilesIfNecessary() {
        if(deleteExcept != null) {
            File[] files = activity.regularContentPath().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.contains("content");
                }
            });
            for(File file: files) {
                if(!file.equals(deleteExcept)) {
                    Log.i(TAG, "Delete File " + file.getName() + ", success: " + file.delete());
                }
            }
            deleteExcept = null;
        }
    }

    private void displaySlideshow() {
        if(displayCountdown <= 0) {
            try {
                ParcelFileDescriptor fd = ParcelFileDescriptor.open(fileToDisplay, ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer renderer = new PdfRenderer(fd);

                currentPage = (currentPage + 1) % renderer.getPageCount();
                final Bitmap bitmap = Utils.renderPage(renderer, currentPage, activity.getContainer());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.showSlide(bitmap);
                    }
                });
                renderer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            displayCountdown = config.getSlideshowTimings().secondsForSlide(currentPage);
        }
        displayCountdown--;
    }

    private void displayVideo(boolean hasNewContent) {
        activity.startVideo(fileToDisplay.getAbsolutePath(), hasNewContent);
    }

    private File getFileToDisplay() {
        if(newContent != null) {
            String filename = newContent.getName();
            String[] temp = filename.split("\\.");
            String extension = temp[temp.length-1];
            File newFilename = new File(activity.regularContentPath() + "/content." + extension);
            boolean didMove = newContent.renameTo(newFilename);
            Log.i(TAG, "Move temp file: " + newContent.getPath() + " -> " + newFilename + ", success: " + didMove);
            if(didMove) {
                deleteExcept = newFilename;
                fileToDisplay = newFilename;
            } else {
                Log.w(TAG, "could not move File, loading again...");
                config.setLastUpdate(0); // reload
            }
            newContent = null;
        }
        if(fileToDisplay != null) {
            return fileToDisplay;
        }
        return getMostCurrentFile(true);
    }

    public File getMostCurrentFile(boolean deleteOld) {
        // else get file
        File[] files = activity.regularContentPath().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.contains("content");
            }
        });
        if(files.length == 0) {
            Log.e(TAG, "No content display file found");
            return null;
        } else if(files.length > 1) { // show newest
            Log.d(TAG, "display files found: " + files.length);
            File f = null;
            for(File file: files) {
                if(f == null || f.lastModified() < file.lastModified()) { // f is older
                    if(f != null) {
                        Log.d(TAG, "f.lastModified: " + f.lastModified() + " " + f.getName());
                        Log.d(TAG, "g.lastModified: " + file.lastModified() + " " + f.getName());
                        if(deleteOld) {
                            Log.i(TAG, "Delete Temp File " + f.getName() + ", success: " + f.delete());
                        }
                    }
                    f = file;
                }
            }
            return f;
        } else { // length == 1
            Log.d(TAG, "one display file found.");

            fileToDisplay = files[0];
            return files[0];
        }
    }

    public void setNewContent(File newContent) {
        Log.i(TAG, "setting newContent to" + newContent.toString());
        this.newContent = newContent;
        this.displayCountdown = 0;
        this.currentPage = -1;
    }



}
