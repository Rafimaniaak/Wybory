package com.election.service;

import com.election.model.Candidate;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color; // Dodany import
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportServicePDF {

    public static void exportToPDF(List<Candidate> candidates, String filePath) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        BaseFont bf = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font font = new Font(bf, 12);
        Font headerFont = new Font(bf, 14, Font.BOLD);

        Paragraph header = new Paragraph("Wyniki wyborów - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), headerFont);
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(20);
        document.add(header);

        int totalVotes = candidates.stream().mapToInt(Candidate::getVotes).sum();

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 3, 2, 2, 2});

        table.addCell(createHeaderCell("ID", headerFont));
        table.addCell(createHeaderCell("Kandydat", headerFont));
        table.addCell(createHeaderCell("Partia", headerFont));
        table.addCell(createHeaderCell("Liczba głosów", headerFont));
        table.addCell(createHeaderCell("Procent", headerFont));

        for (Candidate c : candidates) {
            double percent = totalVotes > 0 ? (c.getVotes() * 100.0) / totalVotes : 0;
            String percentText = String.format("%.2f%%", percent).replace('.', ',');

            table.addCell(createCell(String.valueOf(c.getId()), font));
            table.addCell(createCell(c.getName(), font));
            table.addCell(createCell(c.getParty() != null ? c.getParty() : "", font));
            table.addCell(createCell(String.valueOf(c.getVotes()), font));
            table.addCell(createCell(percentText, font));
        }

        document.add(table);
        document.close();
    }

    private static PdfPCell createHeaderCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(220, 220, 220)); // Szary kolor
        cell.setPadding(5);
        cell.setBorderWidth(1);
        return cell;
    }

    private static PdfPCell createCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        cell.setBorderWidth(1);
        return cell;
    }
}