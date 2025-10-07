package com.ugelcorongo.edugestin360.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class UriUtils {
    private UriUtils() {}

    /**
     * Lee todos los bytes de un Uri de contenido.
     */
    public static byte[] readBytesFromUri(Context ctx, Uri uri) {
        try (InputStream is = ctx.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Guarda un Bitmap en la galería bajo Pictures/EduGestión360 y
     * devuelve la Uri para posteriores lecturas/envío.
     */
    public static Uri saveBitmapReturnUri(
            Context ctx,
            Bitmap bitmap,
            String displayName
    ) {
        ContentValues vals = new ContentValues();
        vals.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
        vals.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Carpeta virtual sin permisos WRITE_EXTERNAL
            vals.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/EduGestión360"
            );
        }

        Uri uri = ctx.getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, vals);
        if (uri == null) {
            throw new RuntimeException("No se pudo crear Uri para imagen");
        }

        try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
            if (!bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90,
                    os
            )) {
                throw new IOException("Compresión JPEG fallida");
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Opcional: eliminar uri pendiente si la compresión falla
        }

        return uri;
    }
}