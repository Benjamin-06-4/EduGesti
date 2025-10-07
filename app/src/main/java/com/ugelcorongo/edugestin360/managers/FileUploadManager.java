package com.ugelcorongo.edugestin360.managers;

import android.net.Uri;

import java.util.Map;

public interface FileUploadManager {
    void upload(Uri fileUri, Map<String,String> meta,
                UploadCallback callback);
    interface UploadCallback {
        void onSuccess();
        void onError(Throwable t);
    }
}