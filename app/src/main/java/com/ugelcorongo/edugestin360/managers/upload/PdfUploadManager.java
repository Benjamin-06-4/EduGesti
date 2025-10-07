package com.ugelcorongo.edugestin360.managers.upload;

import android.content.Context;
import android.net.Uri;

import com.ugelcorongo.edugestin360.managers.AbstractVolleyUpload;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;
public class PdfUploadManager extends AbstractVolleyUpload {
    public PdfUploadManager(Context ctx){
        super(ctx, URLPostHelper.PDF.REGISTRAR);
    }
    @Override protected String getDataKey()  { return "files"; }
    @Override protected String getMimeType() { return "application/pdf"; }
    @Override protected String getExtension() { return ".pdf"; }
}