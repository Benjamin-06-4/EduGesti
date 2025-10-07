package com.ugelcorongo.edugestin360.managers.file;

import java.io.IOException;
import java.util.List;

public interface FileUpdater {
    boolean shouldUpdate();

    /** Descarga/guarda el archivo local; lanza IOException si falla */
    void update() throws IOException;

    /** Lee todas las líneas del archivo local */
    List<String> readLines() throws IOException;
}