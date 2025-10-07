package com.ugelcorongo.edugestin360;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.ugelcorongo.edugestin360.managers.upload.PendingUploadProcessor;

public class MyApp extends Application {
    private ExecutorService executor;
    private PendingUploadProcessor processor;

    @Override
    public void onCreate() {
        super.onCreate();
        executor  = Executors.newSingleThreadExecutor();
        processor = new PendingUploadProcessor(this);

        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest req = new NetworkRequest.Builder().build();
        cm.registerNetworkCallback(req, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // Se dispara cuando vuelve la red
                executor.execute(() -> processor.processAll());
            }
        });
    }
}