package com.ugelcorongo.edugestin360.managers.file;

import android.content.Context;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LocalFileReader implements FileUpdater {
    private final Context ctx;
    private final String  fileName;

    public LocalFileReader(Context ctx, String fileName) {
        this.ctx      = ctx;
        this.fileName = fileName;
    }

    @Override
    public boolean shouldUpdate() {
        return false;
    }

    @Override
    public void update() {
        // nada que hacer; trabajamos con el archivo local existente
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