package com.example.genkiplayer.util;

import android.util.Log;

import com.example.genkiplayer.MainConfig;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadHelper {
    private static final String TAG = "DOWNLOAD HELPER";
    private HttpURLConnection connection = null;
    private URL url;
    private boolean force;
    private long lastModified = -1;
    private String format;
    private ChallengeResponse cr;

    public DownloadHelper(String remoteURLString, boolean force, String format) throws MalformedURLException {
        url = new URL(remoteURLString);
        this.force = force;
        this.format = format;
        cr = MainConfig.getInstance().generateNewChallengeResponse();
    }

    public DownloadHelper(String remoteURLString, boolean force) throws MalformedURLException {
        this(remoteURLString, force, "");
    }
    public DownloadHelper(String remoteURLString) throws MalformedURLException {
        this(remoteURLString, true, "");
    }

    public long getLastModified() throws IOException {
        if(lastModified == -1) {
            openConnection();
            lastModified = connection.getLastModified();
            Log.d(TAG, "Last Modified " + Utils.formatDate(lastModified) + " (" + url.toString() + ")");
        }
        return lastModified;
    }

    public boolean downloadToFile(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        return downloadToStreamAndCloseIt(fos, f);
    }

    public String downloadToString() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(downloadToStreamAndCloseIt(baos, null)) {
            return baos.toString("UTF-8");
        } else {
            return null;
        }
    }


    private boolean downloadToStreamAndCloseIt(OutputStream os, File fileToUpdate) throws IOException {
        openConnection();
        Log.i(TAG, "HTTP Response " + connection.getResponseCode() +
                " (" + url.toString() + ")"
                + (needToDownload() ? ", will download" : "")
                + (force ? ", forced download" : ""));
        if(!cr.checkResponse(connection.getHeaderField("X-Genki-Response"))) {
            throw new IOException("Server not authenticated!");
        }
        if (needToDownload()) {
            IOException ex = null;
            try (DataInputStream dis = new DataInputStream(connection.getInputStream())) {
                byte[] buffer = new byte[4096];
                int length;

                int downloaded = 0;
                int fileLength = connection.getContentLength();

                int i = 0;
                while ((length = dis.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                    i++;
                    downloaded += length;
                    if (i % 100 == 0) {
                        Log.v(TAG, "Download Progress: " + (downloaded * 100 / fileLength) + "%");
                    }
                }
                if (fileToUpdate != null) {
                    boolean didSetLastModified = fileToUpdate.setLastModified(connection.getLastModified());
                    Log.i(TAG, "Download Complete. Did set last modified: " + didSetLastModified);
                }
            } catch (IOException e) {
                ex = e;
            } finally {
                close();
                os.close();
            }
            if(ex != null ) {
                throw ex;
            }
            return true;
        }
        return false;
    }

    public void close() {
        if(connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    private void openConnection() throws IOException {
        if(connection == null) {
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("X-Genki-Challenge", cr.getChallenge());
        }
    }

    private boolean needToDownload() throws IOException {
        return connection.getResponseCode() == HttpURLConnection.HTTP_OK ||
                (force && connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED);
    }

    public String getFormat() {
        return format;
    }

}
