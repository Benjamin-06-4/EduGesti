package com.ugelcorongo.edugestin360.managers.file;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HttpFileUpdater implements FileUpdater {
    private final Context ctx;
    private final String fileName;
    private final String fileUrl;

    public HttpFileUpdater(Context ctx, String fileName, String fileUrl) {
        this.ctx      = ctx;
        this.fileName = fileName;
        this.fileUrl  = fileUrl;
    }

    @Override
    public boolean shouldUpdate() {
        return true;
    }

    @Override
    public void update() throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        conn.setRequestMethod("GET");

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + conn.getResponseCode());
        }

        try (InputStream in = conn.getInputStream();
             FileOutputStream fos =
                     ctx.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            byte[] buf = new byte[8 * 1024];
            int   len;
            while ((len = in.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public List<String> readLines() throws IOException {
        FileInputStream fis = ctx.openFileInput(fileName);
        BufferedReader  br  = new BufferedReader(new InputStreamReader(fis));
        List<String>    ls  = new ArrayList<>();
        String          line;
        while ((line = br.readLine()) != null) {
            ls.add(line);
        }
        br.close();
        return ls;
    }
}