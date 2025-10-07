package com.ugelcorongo.edugestin360.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "attendance.db";
    private static final int DB_VERSION = 5;
    private static final String TABLE_USERS = "users";
    private static final String COL_ID = "user_id";
    private static final String COL_HASH = "password_hash";
    private static final String COL_ROLE = "role";
    private static final String COL_DOCID = "docidentidad";

    public DBHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String ddl = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_ID + " TEXT PRIMARY KEY, "
                + COL_HASH + " TEXT NOT NULL, "
                + COL_ROLE + " TEXT NOT NULL, "
                + COL_DOCID + " TEXT NOT NULL)";
        db.execSQL(ddl);
    }

    /** Comprueba si userId está en la tabla users */
    public boolean userExists(String userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_USERS,
                new String[]{ COL_ID },
                COL_ID + " = ?",
                new String[]{ userId },
                null, null, null
        );
        boolean exists = c.moveToFirst();
        c.close();
        db.close();
        return exists;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // añadimos rol en v3
            db.execSQL("ALTER TABLE " + TABLE_USERS
                    + " ADD COLUMN " + COL_ROLE
                    + " TEXT NOT NULL DEFAULT 'docente'");
        }
        if (oldVersion < 4) {
            // añadimos docidentidad en v4
            db.execSQL("ALTER TABLE " + TABLE_USERS
                    + " ADD COLUMN " + COL_DOCID
                    + " TEXT NOT NULL DEFAULT ''");
        }
        // futuras migraciones (if oldVersion < 5, etc.)
    }

    /** Inserta o actualiza la contraseña hasheada */
    public void upsertUser(String userId, String passwordHash, String role, String docIdentidad) {
        if (passwordHash == null) passwordHash = "";
        passwordHash = passwordHash.trim().toLowerCase();

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ID, userId);
        cv.put(COL_HASH, passwordHash);
        cv.put(COL_ROLE, role);
        cv.put(COL_DOCID,  docIdentidad);
        db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }


    public void updateUserDocIdent(String userId, String newDocIdent) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DOCID, newDocIdent);
        db.update(TABLE_USERS,
                cv,
                COL_ID + " = ?",
                new String[]{ userId });
        db.close();
    }

    /** Recupera el rol del usuario */
    public String getUserRole(String userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_USERS,
                new String[]{COL_ROLE},
                COL_ID + "=?",
                new String[]{userId},
                null, null, null
        );
        String role = null;
        if (c.moveToFirst()) {
            role = c.getString(0);
        }
        c.close();
        db.close();
        return role;
    }

    /** Recupera la docidentidad asociada a este userId */
    public String getUserDocIdent(String userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_USERS,
                new String[]{ COL_DOCID },
                COL_ID + "=?",
                new String[]{ userId },
                null, null, null
        );
        String doc = null;
        if (c.moveToFirst()) {
            doc = c.getString(0);
        }
        c.close();
        db.close();
        return doc;
    }

    /** Verifica si existe al menos un usuario almacenado */
    public boolean hasStoredUser() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USERS, null);
        boolean exists = false;
        if (c.moveToFirst()) exists = c.getInt(0) > 0;
        c.close();
        db.close();
        return exists;
    }

    public void insertAttendance(String t, double lat, double lon) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("teacher", t);
        v.put("lat", lat);
        v.put("lon", lon);
        v.put("timestamp", System.currentTimeMillis());
        db.insert("attendance", null, v);
    }

    public void deleteAttendanceRecord(String t, double lat, double lon) {
        getWritableDatabase().delete(
                "attendance",
                "teacher=? AND lat=? AND lon=?",
                new String[]{t, String.valueOf(lat), String.valueOf(lon)}
        );
    }

    public void insertEvidence(String t, double lat, double lon, Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        ContentValues v = new ContentValues();
        v.put("teacher", t);
        v.put("lat", lat);
        v.put("lon", lon);
        v.put("timestamp", System.currentTimeMillis());
        v.put("photo", baos.toByteArray());
        getWritableDatabase().insert("evidence", null, v);
    }

    public void deleteEvidenceRecord(String t, double lat, double lon) {
        getWritableDatabase().delete(
                "evidence",
                "teacher=? AND lat=? AND lon=?",
                new String[]{t, String.valueOf(lat), String.valueOf(lon)}
        );
    }

    /** Comprueba que el hash de contraseña coincida */
    public boolean verifyPassword(String userId, String clearPassword) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_USERS,
                new String[]{COL_HASH},
                COL_ID + "=?",
                new String[]{ userId },
                null, null, null);

        if (!c.moveToFirst()) {
            c.close(); db.close();
            return false;
        }

        String storedHash = c.getString(0);
        c.close(); db.close();

        if (storedHash == null) return false;
        return storedHash.trim().toLowerCase().equals(hash(clearPassword));
    }

    /** Hash SHA-256 simple (ilustra; mejorar con PBKDF2 en producción) */
    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b)); // lowercase hex
            }
            return sb.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}