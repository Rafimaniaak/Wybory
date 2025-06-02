package com.election.service;

import com.election.model.Candidate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ExportService {

    public static void exportToCSV(List<Candidate> candidates, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("ID,Nazwisko i Imię,Partia,Liczba głosów\n");
            for (Candidate c : candidates) {
                writer.write(String.format("%d,%s,%s,%d\n",
                        c.getId(), c.getName(), c.getParty(), c.getVotes()));
            }
        }
    }
}
