package com.ugelcorongo.edugestin360.managers.reports;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.Nullable;

import com.ugelcorongo.edugestin360.domain.models.Pregunta;
import com.ugelcorongo.edugestin360.utils.URLPostHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReportGenerator {

    // Base URL para las evidencias (mostrar como hipervínculo en PDF)
    private static final String BASE_EVIDENCE_URL = URLPostHelper.Imagen.IMG;

    // ---------- PDF generator (android.graphics.pdf.PdfDocument) ----------
    public static File generateMultiFichaPdfReport(
            Context ctx,
            Map<String,String> globalMeta,
            List<Map<String,Object>> blocks
    ) throws IOException {
        final int PAGE_WIDTH = 595;
        final int PAGE_HEIGHT = 842;
        final int margin = 36;

        File base = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (base == null) base = ctx.getFilesDir();
        File repDir = new File(base, "reportes");
        if (!repDir.exists()) repDir.mkdirs();
        String filename = "reporte_multiple_" + System.currentTimeMillis() + ".pdf";
        File out = new File(repDir, filename);

        PdfDocument pdf = new PdfDocument();

        Paint titlePaint = new Paint(); titlePaint.setTextSize(16f); titlePaint.setColor(Color.BLACK); titlePaint.setFakeBoldText(true);
        Paint keyPaint = new Paint(); keyPaint.setTextSize(12f); keyPaint.setColor(Color.DKGRAY); keyPaint.setFakeBoldText(true);
        Paint normalPaint = new Paint(); normalPaint.setTextSize(11f); normalPaint.setColor(Color.BLACK);
        Paint sectionPaint = new Paint(); sectionPaint.setTextSize(13f); sectionPaint.setColor(Color.BLACK); sectionPaint.setFakeBoldText(true);
        Paint headerText = new Paint(); headerText.setColor(Color.WHITE); headerText.setFakeBoldText(true); headerText.setTextSize(12f);
        Paint headerBg = new Paint(); headerBg.setColor(Color.parseColor("#1E88E5"));
        Paint borderPaint = new Paint(); borderPaint.setColor(Color.LTGRAY); borderPaint.setStyle(Paint.Style.STROKE); borderPaint.setStrokeWidth(0.8f);
        Paint linkPaint = new Paint(); linkPaint.setTextSize(11f); linkPaint.setColor(Color.parseColor("#1E88E5")); linkPaint.setUnderlineText(true);

        Paint.FontMetrics fm = normalPaint.getFontMetrics();
        final int lineHeight = (int) Math.ceil(fm.descent - fm.ascent);
        final int lineSpacing = 4;

        int pageIndex = 0;
        for (Map<String,Object> blk : blocks) {
            pageIndex++;
            PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create();
            PdfDocument.Page page = pdf.startPage(pi);
            Canvas canvas = page.getCanvas();
            int y = margin;

            Map<String,String> meta = blk.containsKey("meta") ? (Map<String,String>) blk.get("meta") : new HashMap<>();
            List<com.ugelcorongo.edugestin360.domain.models.Pregunta> preguntasRaw =
                    blk.containsKey("preguntas") ? (List<com.ugelcorongo.edugestin360.domain.models.Pregunta>) blk.get("preguntas")
                            : new ArrayList<>();
            Map<String,String> respuestas = blk.containsKey("respuestas") ? (Map<String,String>) blk.get("respuestas") : new HashMap<>();
            Bitmap chart = blk.containsKey("chartBitmap") ? (Bitmap) blk.get("chartBitmap") : null;

            // Create a rendering copy of preguntas so we don't alter original blocks
            List<com.ugelcorongo.edugestin360.domain.models.Pregunta> preguntas = new ArrayList<>(preguntasRaw.size());
            for (com.ugelcorongo.edugestin360.domain.models.Pregunta p : preguntasRaw) {
                com.ugelcorongo.edugestin360.domain.models.Pregunta copy = new com.ugelcorongo.edugestin360.domain.models.Pregunta();
                copy.setIdFicha(p.getIdFicha());
                copy.setTipoFicha(p.getTipoFicha());
                copy.setIdPregunta(p.getIdPregunta());
                // Prefer the catalog text; if empty, attempt to fill from respuestas (do not modify original p)
                String textFromCatalog = safe(p.getTextoPregunta());
                if (textFromCatalog.isEmpty()) {
                    // try q_<id>_pregunta then q_<id>_text then q_<id>_resp
                    String qid = copy.getIdPregunta();
                    String candidate = "";
                    if (qid != null && !qid.isEmpty()) {
                        candidate = normalize(respuestas.getOrDefault("q_" + qid + "_pregunta", ""));
                        if (candidate.isEmpty()) candidate = normalize(respuestas.getOrDefault("q_" + qid + "_text", ""));
                        if (candidate.isEmpty()) candidate = normalize(respuestas.getOrDefault("q_" + qid + "_resp", ""));
                    }
                    copy.setTextoPregunta(candidate);
                } else {
                    copy.setTextoPregunta(textFromCatalog);
                }
                copy.setSeccion(p.getSeccion());
                copy.setTipoPregunta(p.getTipoPregunta());
                copy.setRequiereComentario(p.isRequiereComentario());
                copy.setRequiereFoto(p.isRequiereFoto());
                // preserve options if present
                try { copy.setOpciones(p.getOpciones()); } catch (Throwable ignored) {}
                preguntas.add(copy);
            }

            // Header: título y meta
            canvas.drawText("REPORTE DE FICHA", margin, y, titlePaint);
            y += 26;

            int col1X = margin;
            int col2X = PAGE_WIDTH / 2;
            int metaLine = 16;

            canvas.drawText("Tipo:", col1X, y, keyPaint);
            canvas.drawText(safe(meta.get("tipoFicha").isEmpty() ? globalMeta != null ? globalMeta.get("tipoFicha") : "" : meta.get("tipoFicha")), col1X + 56, y, normalPaint);
            canvas.drawText("Periodo:", col2X, y, keyPaint);
            String periodo = safe(meta.get("mes").isEmpty() ? globalMeta != null ? globalMeta.get("mes") : "" : meta.get("mes"));
            String ano = safe(meta.get("ano").isEmpty() ? globalMeta != null ? globalMeta.get("ano") : "" : meta.get("ano"));
            canvas.drawText(periodo + "-" + ano, col2X + 64, y, normalPaint);
            y += metaLine;

            canvas.drawText("Área:", col1X, y, keyPaint);
            canvas.drawText(safe(meta.get("area")), col1X + 56, y, normalPaint);
            canvas.drawText("Hora:", col2X, y, keyPaint);
            canvas.drawText(safe(meta.get("fecha_generacion")), col2X + 64, y, normalPaint);
            y += metaLine;

            canvas.drawText("Colegio:", col1X, y, keyPaint);
            canvas.drawText(safe(meta.get("colegio")), col1X + 56, y, normalPaint);
            canvas.drawText("Código Modular:", col2X, y, keyPaint);
            canvas.drawText(safe(meta.get("codmod")), col2X + 112, y, normalPaint);
            y += metaLine;

            canvas.drawText("Director:", col1X, y, keyPaint);
            canvas.drawText(safe(meta.get("director")), col1X + 56, y, normalPaint);
            canvas.drawText("Código Local:", col2X, y, keyPaint);
            canvas.drawText(safe(meta.get("codlocal")), col2X + 96, y, normalPaint);
            y += metaLine;

            canvas.drawText("Clave8:", col1X, y, keyPaint);
            canvas.drawText(safe(meta.get("clave8")), col1X + 56, y, normalPaint);
            y += metaLine + 8;

            if (!safe(meta.get("especialista_realizo")).isEmpty()) {
                canvas.drawText("Especialista:", col1X, y, keyPaint);
                canvas.drawText(safe(meta.get("especialista_realizo")), col1X + 80, y, normalPaint);
                y += metaLine + 4;
            }

            if (chart != null) {
                int chartMaxWidth = PAGE_WIDTH - margin * 2;
                float scale = Math.min((float) chartMaxWidth / chart.getWidth(), 1.0f);
                int dstW = (int) (chart.getWidth() * scale);
                int dstH = (int) (chart.getHeight() * scale);
                android.graphics.Rect src = new android.graphics.Rect(0, 0, chart.getWidth(), chart.getHeight());
                android.graphics.RectF dst = new android.graphics.RectF(margin, y, margin + dstW, y + dstH);
                canvas.drawBitmap(chart, src, dst, null);
                y += dstH + 12;
            }

            // Table header layout
            int tableX = margin;
            int tableWidth = PAGE_WIDTH - margin * 2;
            int colN = 28;
            int colQuestion = (int) (tableWidth * 0.52);
            int colResp = 70;
            int colEvid = 140;
            int colComm = tableWidth - (colN + colQuestion + colResp + colEvid);

            canvas.drawRect(tableX, y, tableX + tableWidth, y + 22, headerBg);
            canvas.drawText("N°", tableX + 6, y + 16, headerText);
            canvas.drawText("Pregunta", tableX + colN + 6, y + 14, headerText);
            canvas.drawText("Resp", tableX + colN + colQuestion + 6, y + 14, headerText);
            canvas.drawText("Evid", tableX + colN + colQuestion + colResp + 6, y + 16, headerText);
            canvas.drawText("Comentario", tableX + colN + colQuestion + colResp + colEvid + 6, y + 14, headerText);
            y += 24;

            // Group by section
            LinkedHashMap<String, List<com.ugelcorongo.edugestin360.domain.models.Pregunta>> grouped = new LinkedHashMap<>();
            for (com.ugelcorongo.edugestin360.domain.models.Pregunta p : preguntas) {
                String sec = p.getSeccion(); if (sec == null) sec = "";
                if (!grouped.containsKey(sec)) grouped.put(sec, new ArrayList<>());
                grouped.get(sec).add(p);
            }

            for (Map.Entry<String, List<com.ugelcorongo.edugestin360.domain.models.Pregunta>> entry : grouped.entrySet()) {
                String sectionName = entry.getKey();
                List<com.ugelcorongo.edugestin360.domain.models.Pregunta> list = entry.getValue();

                if (y + 34 > PAGE_HEIGHT - margin) {
                    pdf.finishPage(page);
                    pageIndex++;
                    pi = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create();
                    page = pdf.startPage(pi);
                    canvas = page.getCanvas();
                    y = margin;
                }

                canvas.drawText(sectionName.isEmpty() ? "Rúbrica" : sectionName, tableX, y, sectionPaint);
                y += 18;

                // repeat header
                canvas.drawRect(tableX, y, tableX + tableWidth, y + 18, headerBg);
                canvas.drawText("N°", tableX + 6, y + 14, headerText);
                canvas.drawText("Pregunta", tableX + colN + 6, y + 14, headerText);
                canvas.drawText("Resp", tableX + colN + colQuestion + 6, y + 14, headerText);
                canvas.drawText("Evid", tableX + colN + colQuestion + colResp + 6, y + 14, headerText);
                canvas.drawText("Comentario", tableX + colN + colQuestion + colResp + colEvid + 6, y + 14, headerText);
                y += 20;

                int idx = 1;
                for (com.ugelcorongo.edugestin360.domain.models.Pregunta p : list) {
                    String id = p.getIdPregunta();
                    String questionText = safe(p.getTextoPregunta());
                    // Responses keys may be q_<rid>_text, q_<rid>_resp or q_<id>_text etc.
                    // We already used respuestas to fill questionText when catalog text missing,
                    // but still try to obtain response/comment/evidence for rendering.
                    String finalResp = normalize(respuestas.getOrDefault("q_" + id + "_text",
                            respuestas.getOrDefault("q_" + id + "_resp", respuestas.getOrDefault("q_" + id + "_respuesta", ""))));
                    if (finalResp.isEmpty()) {
                        // fallback: there may be keys where id is the response record id (not pregunta id).
                        // try to find first key that ends with "_text" and whose corresponding pregunta text equals questionText
                        if (!questionText.isEmpty()) {
                            for (Map.Entry<String,String> re : respuestas.entrySet()) {
                                String k = re.getKey();
                                if (k.endsWith("_text") || k.endsWith("_resp") || k.endsWith("_respuesta")) {
                                    String qk = k.substring(0, k.lastIndexOf('_')); // q_<rid>
                                    String pregKey = qk + "_pregunta";
                                    String qFromMap = safe(respuestas.get(pregKey));
                                    if (!qFromMap.isEmpty() && qFromMap.equalsIgnoreCase(questionText)) {
                                        finalResp = normalize(re.getValue());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    String evidence = normalize(respuestas.getOrDefault("q_" + id + "_foto", respuestas.getOrDefault("q_" + id + "_foto_name", "")));
                    String comment = normalize(respuestas.getOrDefault("q_" + id + "_comentario", respuestas.getOrDefault("q_" + id + "_comment", "")));

                    List<String> qLines = wrapText(questionText, normalPaint, colQuestion - 10);
                    List<String> cLines = wrapText(comment, normalPaint, colComm - 8);

                    int qLinesCount = Math.max(1, qLines.size());
                    int cLinesCount = Math.max(1, cLines.size());
                    int rowsNeeded = Math.max(qLinesCount, cLinesCount);
                    int rowHeight = rowsNeeded * (lineHeight + lineSpacing) + 8;

                    if (y + rowHeight + 20 > PAGE_HEIGHT - margin) {
                        pdf.finishPage(page);
                        pageIndex++;
                        pi = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex).create();
                        page = pdf.startPage(pi);
                        canvas = page.getCanvas();
                        y = margin;
                    }

                    int rowTop = y;
                    int rowBottom = y + rowHeight;
                    canvas.drawRect(tableX, rowTop, tableX + tableWidth, rowBottom, borderPaint);

                    int xN = tableX + colN;
                    int xQ = tableX + colN + colQuestion;
                    int xR = xQ + colResp;
                    int xE = xR + colEvid;
                    canvas.drawLine(xN, rowTop, xN, rowBottom, borderPaint);
                    canvas.drawLine(xQ, rowTop, xQ, rowBottom, borderPaint);
                    canvas.drawLine(xR, rowTop, xR, rowBottom, borderPaint);
                    canvas.drawLine(xE, rowTop, xE, rowBottom, borderPaint);

                    canvas.drawText(String.valueOf(idx), tableX + 6, rowTop + (lineHeight), normalPaint);

                    int qy = rowTop + lineHeight;
                    for (String line : qLines) {
                        canvas.drawText(line, tableX + colN + 6, qy, normalPaint);
                        qy += lineHeight + lineSpacing;
                    }

                    String respToDraw = finalResp == null ? "" : finalResp;
                    if (normalPaint.measureText(respToDraw) > (colResp - 8)) {
                        respToDraw = android.text.TextUtils.ellipsize(respToDraw, new android.text.TextPaint(normalPaint), colResp - 12, android.text.TextUtils.TruncateAt.END).toString();
                    }
                    canvas.drawText(respToDraw, xQ + 6, rowTop + lineHeight, normalPaint);

                    if (!evidence.isEmpty()) {
                        String fullUrl = BASE_EVIDENCE_URL.endsWith("/") ? BASE_EVIDENCE_URL + evidence : BASE_EVIDENCE_URL + "/" + evidence;
                        float avail = colEvid - 8;
                        String toDraw = fullUrl;
                        if (linkPaint.measureText(toDraw) > avail) {
                            toDraw = android.text.TextUtils.ellipsize(toDraw, new android.text.TextPaint(linkPaint), avail - 4, android.text.TextUtils.TruncateAt.MIDDLE).toString();
                        }
                        canvas.drawText(toDraw, xR + 6, rowTop + lineHeight, linkPaint);
                    }

                    int cy = rowTop + lineHeight;
                    for (String cl : cLines) {
                        canvas.drawText(cl, xE + 6, cy, normalPaint);
                        cy += lineHeight + lineSpacing;
                    }

                    y += rowHeight + 4;
                    idx++;
                }
                y += 8;
            }

            pdf.finishPage(page);
        }

        try (FileOutputStream fos = new FileOutputStream(out)) {
            pdf.writeTo(fos);
        } finally {
            pdf.close();
        }
        return out;
    }

    // ---------- Utilities ----------
    private static List<String> wrapText(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) { lines.add(""); return lines; }
        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String trial = cur.length() == 0 ? w : cur + " " + w;
            if (paint.measureText(trial) <= maxWidth) {
                cur = new StringBuilder(trial);
            } else {
                if (cur.length() > 0) lines.add(cur.toString());
                // If single word longer than maxWidth, split the word
                if (paint.measureText(w) <= maxWidth) {
                    cur = new StringBuilder(w);
                } else {
                    String part = w;
                    while (paint.measureText(part) > maxWidth) {
                        int fit = Math.max(1, (int) (part.length() * (maxWidth / paint.measureText(part)))) ;
                        while (fit > 0 && paint.measureText(part.substring(0, fit)) > maxWidth) fit--;
                        if (fit <= 0) break;
                        lines.add(part.substring(0, fit));
                        part = part.substring(fit);
                    }
                    cur = new StringBuilder(part);
                }
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private static byte[] bitmapToPngBytes(Bitmap bmp) {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /**
     * Normaliza valores que puedan venir como null textual ("null") o realmente null.
     * Devuelve cadena vacía si el valor es null o "null" (ignorando mayúsculas).
     */
    private static String normalize(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.equalsIgnoreCase("null")) return "";
        return s;
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}