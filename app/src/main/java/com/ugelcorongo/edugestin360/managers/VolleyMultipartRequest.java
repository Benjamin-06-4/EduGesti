package com.ugelcorongo.edugestin360.managers;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public abstract class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private final Map<String, String> mHeaders;
    private final Map<String, String> mParams;
    private final Map<String, DataPart> mByteData;

    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mErrorListener = errorListener;
        this.mHeaders = new HashMap<>();
        this.mParams = new HashMap<>();
        this.mByteData = new HashMap<>();
    }

    @Override
    public Map<String, String> getHeaders() { return mHeaders; }

    @Override
    protected Map<String, String> getParams() { return mParams; }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    private final String boundary = "apiclient-" + System.currentTimeMillis();

    @Override
    public byte[] getBody() throws AuthFailureError {
        Map<String, String> params = getParams();       // Llama al metodo sobrescrito que llena los parámetros
        Map<String, DataPart> byteData = getByteData();   // Llama al metodo sobrescrito que llena los datos del archivo
        return buildMultipartBody(params, byteData);
    }

    private byte[] buildMultipartBody(Map<String, String> params, Map<String, DataPart> byteData) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            // Agregar los parámetros de texto
            if (params != null && params.size() > 0) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                    dos.writeBytes(entry.getValue() + "\r\n");
                }
            }

            // Agregar los archivos (datos en bytes)
            if (byteData != null && byteData.size() > 0) {
                for (Map.Entry<String, DataPart> entry : byteData.entrySet()) {
                    DataPart dataPart = entry.getValue();
                    dos.writeBytes("--" + boundary + "\r\n");
                    dos.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() +
                            "\"; filename=\"" + dataPart.getFileName() + "\"\r\n");
                    dos.writeBytes("Content-Type: " + dataPart.getType() + "\r\n\r\n");
                    dos.write(dataPart.getContent());
                    dos.writeBytes("\r\n");
                }
            }
            dos.writeBytes("--" + boundary + "--\r\n");

            byte[] body = bos.toByteArray();
            return body;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        mErrorListener.onErrorResponse(error);
    }

    // Metodo para agregar parámetros de string
    public void addStringParam(String key, String value) {
        mParams.put(key, value);
    }

    // Metodo para agregar datos de bytes
    public void addByteData(String key, DataPart dataPart) {
        mByteData.put(key, dataPart);
    }

    protected abstract Map<String, DataPart> getByteData();

    public static class DataPart {
        private final String fileName;
        private final byte[] content;
        private final String type;

        public DataPart(String fileName, byte[] content, String type) {
            this.fileName = fileName;
            this.content = content;
            this.type = type;
        }

        public String getFileName() { return fileName; }
        public byte[] getContent() { return content; }
        public String getType() { return type; }
    }
}