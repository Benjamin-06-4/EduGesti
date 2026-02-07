package com.ugelcorongo.edugestin360.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public final class HttpHelper {
    public static String getSync(String baseUrl, Map<String,String> params, Context ctx) throws Exception {
        StringBuilder q = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String,String> e : params.entrySet()) {
                if (q.length() > 0) q.append("&");
                q.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(e.getValue() == null ? "" : e.getValue(), "UTF-8"));
            }
        }
        String full = baseUrl + (q.length() > 0 ? ("?" + q.toString()) : "");
        URL url = new URL(full);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
        conn.setDoInput(true);
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return null;
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        br.close();
        conn.disconnect();
        return sb.toString().trim();
    }
}