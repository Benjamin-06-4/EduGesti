package com.ugelcorongo.edugestin360.managers.file;

import android.content.Context;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;

public class FileUpdaterFactory {
    public static FileUpdater create(Context ctx, String fileName, String url) {
        if (NetworkUtil.isConnected(ctx)) {
            return new HttpFileUpdater(ctx, fileName, url);
        } else {
            return new LocalFileReader(ctx, fileName);
        }
    }
}