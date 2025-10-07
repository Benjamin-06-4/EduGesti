package com.ugelcorongo.edugestin360.managers;

import android.content.Context;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.ugelcorongo.edugestin360.utils.NetworkUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractVolleyUpload implements FileUploadManager {
    protected Context ctx;
    protected String endpoint;
    public AbstractVolleyUpload(Context ctx, String endpoint){
        this.ctx=ctx; this.endpoint=endpoint;
    }

    @Override
    public void upload(Uri fileUri, Map<String,String> meta, UploadCallback cb){
        byte[] data = getBytes(fileUri);
        VolleyMultipartRequest req = new VolleyMultipartRequest(
                Request.Method.POST, endpoint,
                resp -> cb.onSuccess(),
                err -> cb.onError(err)
        ){
            @Override protected Map<String,String> getParams() { return meta; }
            @Override protected Map<String,DataPart> getByteData(){
                String key = getDataKey();
                return Collections.singletonMap(
                        key, new DataPart(generateFileName(), data, getMimeType())
                );
            }
        };
        Volley.newRequestQueue(ctx).add(req);
    }

    /**
     * Envío bloqueante. Lanza IOException si falla o no hay red.
     */
    public void uploadSync(Uri fileUri, Map<String,String> meta) throws IOException {
        if (!NetworkUtil.isConnected(ctx)) {
            throw new IOException("Sin conexión");
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        // Reutiliza el mismo callback para señalizar fin
        upload(fileUri, meta, new UploadCallback() {
            @Override public void onSuccess() {
                latch.countDown();
            }
            @Override public void onError(Throwable t) {
                errorRef.set(new IOException(t));
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new IOException("Envío interrumpido", e);
        }

        if (errorRef.get() != null) {
            throw (IOException) errorRef.get();
        }
    }

    protected abstract String getDataKey();
    protected abstract String getMimeType();
    protected byte[] getBytes(Uri uri) {
        try (InputStream inputStream = ctx.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] dataChunk = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(dataChunk)) != -1) {
                buffer.write(dataChunk, 0, bytesRead);
            }
            return buffer.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
    protected String generateFileName(){
        return System.currentTimeMillis() + getExtension();
    }
    protected abstract String getExtension();
}