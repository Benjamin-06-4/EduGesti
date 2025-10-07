package com.ugelcorongo.edugestin360.managers.upload;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.ugelcorongo.edugestin360.managers.AbstractVolleyUpload;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import java.util.Map;

public class ImageUploadManager extends AbstractVolleyUpload {
    public ImageUploadManager(Context ctx){
        super(ctx, URLPostHelper.Imagen.REGISTRAR);
    }
    @Override protected String getDataKey()  { return "evidencia"; }
    @Override protected String getMimeType() { return "image/jpeg"; }
    @Override protected String getExtension() { return ".jpg"; }

    /**
     * @param url              Endpoint a invocar
     * @param params           Map de parámetros POST
     * @param responseListener Callback en caso de éxito
     * @param errorListener    Callback en caso de error
     */
    public void postForm(
            String url,
            Map<String,String> params,
            Response.Listener<String> responseListener,
            Response.ErrorListener errorListener
    ) {
        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                responseListener,
                errorListener
        ) {
            @Override
            protected Map<String,String> getParams() {
                return params;
            }
        };
        Volley.newRequestQueue(ctx).add(req);
    }
}