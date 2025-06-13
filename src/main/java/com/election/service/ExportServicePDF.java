package com.election.service;

import com.election.model.Candidate;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Serwis eksportu do PDF
public class ExportServicePDF {

    // Eksportuje wyniki do pliku PDF
    public static void exportToPDF(List<Candidate> candidates, String filePath) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // Dodaj czcionkę z obsługą polskich znaków
        BaseFont bf = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font font = new Font(bf, 12);

        // Nagłówek z datą
        document.add(new Paragraph("Wyniki wyborów - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), font));
        document.add(new Paragraph(" "));

        // Tabela z danymi
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        // Nagłówki tabeli
        table.addCell(createCell("ID", font, Element.ALIGN_CENTER));
        table.addCell(createCell("Nazwisko i Imię", font, Element.ALIGN_CENTER));
        table.addCell(createCell("Partia", font, Element.ALIGN_CENTER));
        table.addCell(createCell("Liczba głosów", font, Element.ALIGN_CENTER));

        // Wiersze z danymi
        for (Candidate c : candidates) {
            table.addCell(createCell(String.valueOf(c.getId()), font, Element.ALIGN_CENTER));
            table.addCell(createCell(c.getName(), font, Element.ALIGN_LEFT));
            table.addCell(createCell(c.getParty() != null ? c.getParty() : "", font, Element.ALIGN_LEFT));
            table.addCell(createCell(String.valueOf(c.getVotes()), font, Element.ALIGN_CENTER));
        }

        document.add(table);
        document.close();
    }

    // Pomocnicza metoda do tworzenia komórek z formatowaniem
    private static PdfPCell createCell(String content, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        return cell;
    }
}
