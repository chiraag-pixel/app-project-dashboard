package com.skillgap.service;

import com.skillgap.exception.BusinessException;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

/**
 * Extracts readable text from uploaded documents using Apache PDFBox (PDF) and
 * Apache POI (DOCX). Image-only/scanned documents produce no text; OCR is NOT
 * supported and the user is told so explicitly.
 */
@Service
public class DocumentExtractionService {

    /** @param extension lower-case file extension without the dot (pdf / docx) */
    public String extractText(byte[] bytes, String extension) {
        try {
            return switch (extension) {
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                default -> throw new BusinessException("Unsupported file type: ." + extension
                        + ". Only PDF and DOCX files are accepted.");
            };
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException("The file could not be read. It may be corrupted or password protected. "
                    + "Note: scanned (image-only) documents are not supported - OCR is not available in this version.");
        }
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = PDDocument.load(bytes)) {
            if (document.isEncrypted()) {
                throw new BusinessException("This PDF is password protected. Please upload an unprotected file.");
            }
            if (document.getNumberOfPages() == 0) {
                throw new BusinessException("This PDF has no pages.");
            }
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /** True when the extracted text contains no usable words (likely scanned). */
    public boolean isBlank(String text) {
        if (text == null) {
            return true;
        }
        String letters = text.replaceAll("[^\\p{L}]", "");
        return letters.length() < 20;
    }
}
