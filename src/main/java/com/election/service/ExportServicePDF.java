package com.election.service;

import com.election.model.Candidate;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.util.List;

public class ExportServicePDF {

    public static void exportToPDF(List<Candidate> candidates, String filePath) throws Exception {
        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        document.add(new Paragraph("Wyniki wyborów"));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.addCell("ID");
        table.addCell("Nazwisko i Imię");
        table.addCell("Partia");
        table.addCell("Liczba głosów");

        for (Candidate c : candidates) {
            table.addCell(String.valueOf(c.getId()));
            table.addCell(c.getName());
            table.addCell(c.getParty());
            table.addCell(String.valueOf(c.getVotes()));
        }

        document.add(table);
        document.close();
    }
}
