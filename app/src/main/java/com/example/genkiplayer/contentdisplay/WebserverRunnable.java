package com.example.genkiplayer.contentdisplay;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.example.genkiplayer.MainConfig;
import com.example.genkiplayer.server.MyServer;

import java.io.IOException;

public class WebserverRunnable implements Runnable {
    private static final String TAG = "WEBSERVER_RUNNABLE";
    private final ContentActivity activity;
    private static final String SERVICE_TYPE = "_genkicontent._tcp.";
    private NsdManager mNsdManager;

    private final NsdManager.RegistrationListener registrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "NSD registration failed");
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "NSD unregistration failed");
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "NSD service registered");
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "NSD service unregistered");
        }
    };

    public WebserverRunnable(ContentActivity contentActivity) {
        this.activity = contentActivity;
    }

    @Override
    public void run() {
        MyServer server = new MyServer(activity);
        try {
            server.start();
            registerService(server.getListeningPort());
        } catch (IOException e) {
            Log.e(TAG, "could not start server.");
            e.printStackTrace();
        }
        while(activity.runBackgroundThreads) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) { }
        }
        server.stop();
        if(mNsdManager != null) {
            mNsdManager.unregisterService(registrationListener);
        }

        Log.i(TAG, "Stopping...");
    }

    private void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();

        String deviceName;
        try {
            deviceName = MainConfig.getInstance().getSanitizedDeviceName();
        } catch (Exception e) {
            deviceName = "Fire_TV";
            e.printStackTrace();
        }
        serviceInfo.setServiceName(deviceName);
        serviceInfo.setServiceType(MainConfig.getInstance().getContentServiceType());
        serviceInfo.setPort(port);
        serviceInfo.setAttribute("devName", deviceName);

        mNsdManager = (NsdManager) activity.getApplicationContext().getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);

    }

}
