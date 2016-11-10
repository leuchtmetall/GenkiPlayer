package com.example.genkiplayer.util;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.View;

import com.example.genkiplayer.MainConfig;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

public class Utils {
    private static final String TAG = "UTILS";

    public static String getFileExtension(File f) {
        return FilenameUtils.getExtension(f.getName());
    }

    @SuppressLint("SimpleDateFormat")
    public static String formatDate(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp);
    }

    public static final int WIDTH = 0;
    public static final int HEIGHT = 1;
    public static final int PADDING_TOP = 2;
    public static final int PADDING_LEFT = 3;

    public static int[] resizeToFitIntoBox(int width, int height, int boxWidth, int boxHeight) {
        int[] dimensions = new int[4];
        if(width * boxHeight > height * boxWidth) {
            dimensions[HEIGHT] = height * boxWidth / width;
            dimensions[WIDTH] = boxWidth;
        } else {
            dimensions[HEIGHT] = boxHeight;
            dimensions[WIDTH] = width * boxHeight / height;
        }
        dimensions[PADDING_LEFT] = (boxWidth - dimensions[WIDTH]) / 2;
        dimensions[PADDING_TOP] = (boxWidth - dimensions[HEIGHT]) / 2;
        return dimensions;
    }

    public static int[] getDimensions(int width, int height, View container) {
        int maxWidth = container.getWidth();
        int maxHeight = container.getHeight();

        // fall back to full HD if container layout has not finished yet.
        // TODO refactor comsuming code to reliably get container dimensions
        maxWidth = maxWidth > 0 ? maxWidth : 1920;
        maxHeight = maxHeight > 0 ? maxHeight : 1920;

        return resizeToFitIntoBox(width, height, maxWidth, maxHeight);
    }

    synchronized public static Bitmap renderPage(PdfRenderer renderer, int pageNumber, View container) {
        PdfRenderer.Page page = renderer.openPage(pageNumber);

        int[] dimensions = Utils.getDimensions(page.getWidth(), page.getHeight(), container);

        final Bitmap bitmap = Bitmap.createBitmap(dimensions[Utils.WIDTH], dimensions[Utils.HEIGHT], Bitmap.Config.ARGB_4444);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        page.close();
        return bitmap;
    }

    public static String getToken() {
        // IP + Time + Password + Device Name ???
        String input = MainConfig.getInstance().getDeviceTokenSecret();
        try {
            input += MainConfig.getInstance().getSanitizedDeviceName(true);
        } catch (Exception e) {
            input += "Fire TV";
        }
        try {
            return sha1(input);
        } catch (Exception e){
            Log.e(TAG, "Error while generating token");
            return "";
        }

    }

    public static String sha1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }


    public static String readTextFile(File f) {
        if(f == null || !f.exists()) {
            return null;
        }
        try(FileInputStream inputStream = new FileInputStream(f)) {
            return IOUtils.toString(inputStream);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public static long getFreeStorageSpace() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    public static String filenameWithoutExtension(String filename) {
        return FilenameUtils.getBaseName(filename);
//        return FilenameUtils.removeExtension(filename);
    }
}
