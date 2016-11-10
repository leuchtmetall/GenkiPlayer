package com.example.genkiplayer;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.*;
import android.os.Process;
import android.util.Log;

import com.example.genkiplayer.util.DownloadHelper;

import java.net.Inet4Address;
import java.net.Inet6Address;

import static java.lang.String.format;



public class ZeroconfDiscoverService extends Service {
    private static final String TAG = "dnssd";
    private ServiceHandler mServiceHandler;
    private NsdManager nsdManager = null;
    private final MainConfig config = MainConfig.getInstance();

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if(nsdManager != null) {
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener);
                    nsdManager = null;
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping network service discovery");
                }
            }
            nsdManager = (NsdManager) getSystemService(NSD_SERVICE);
            if (nsdManager == null) {
                Log.e(TAG, "Discovery Service could not be started.");
            }
            nsdManager.discoverServices(MainConfig.getInstance().getServiceType(), NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        config.setAppContext(getApplicationContext());
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceHandler = new ServiceHandler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
    }

    /**
     * The DiscoveryListener
     */
    private final NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.w(TAG, format("Failed to stop discovery serviceType=%s, errorCode=%d", serviceType, errorCode));
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.w(TAG, format("Failed to start discovery serviceType=%s, errorCode=%d", serviceType, errorCode));
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Log.v(TAG, format("Service lost serviceInfo=%s", serviceInfo));
            config.setServerIP(null);
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            Log.v(TAG, format("Service found serviceInfo=%s", serviceInfo));

            try {
                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.w(TAG, format("Failed to resolve serviceInfo=%s, errorCode=%d", serviceInfo, errorCode));
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {
                        Log.d(TAG, format("Service resolved serviceInfo=%s", serviceInfo));
                        String ip = "";
                        if(serviceInfo.getHost() instanceof Inet6Address) {
                            ip = "[" + serviceInfo.getHost().getHostAddress() + "]";
                        } else if(serviceInfo.getHost() instanceof Inet4Address) {
                            ip = serviceInfo.getHost().getHostAddress();
                        } else {
                            Log.e(TAG, "unknown address type");
                        }

                        boolean serverValid = false;
                        int tryCount = 2;
                        while(tryCount > 0) {
                            try {
                                String urlString = "http://" + ip + ":" + serviceInfo.getPort() + "/datac";
                                DownloadHelper dh = new DownloadHelper(urlString);
                                serverValid = (dh.downloadToString() != null);
                                tryCount -= 2;
                            } catch (Exception e) {
                                e.printStackTrace();
                                try {Thread.sleep(1000); } catch(InterruptedException ex) {}
                                tryCount -= 1;
                            }
                        }
                        if(serverValid) {
                            config.setServerPort(serviceInfo.getPort());
                            config.setServerIP(ip);
                        } else {
                            Log.i(TAG, "Server " + ip + " did not authenticate successfully.");
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, format("Discovery stopped serviceType=%s", serviceType));
        }

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.i(TAG, format("Discovery started serviceType=%s", serviceType));
        }
    };
}
