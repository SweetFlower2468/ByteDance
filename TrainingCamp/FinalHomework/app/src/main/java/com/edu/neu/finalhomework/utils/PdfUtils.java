package com.edu.neu.finalhomework.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;

public class PdfUtils {

    private static final String TAG = "PdfUtils";

    public static String extractTextFromPdf(Context context, Uri uri) {
        PDDocument document = null;
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) return null;

            document = PDDocument.load(inputStream);
            if (document.isEncrypted()) {
                Log.e(TAG, "PDF is encrypted, skip parsing");
                return null;
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setWordSeparator(" ");
            stripper.setLineSeparator("\n");
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());

            String text = stripper.getText(document);
            if (text != null) {
                text = text.replace("\r", "").trim();
            }
            if (text != null && text.isEmpty()) {
                text = null;
            }
            return text;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting text from PDF", e);
            return null;
        } finally {
            if (document != null) {
                try { document.close(); } catch (Exception ignored) {}
            }
        }
    }
}

