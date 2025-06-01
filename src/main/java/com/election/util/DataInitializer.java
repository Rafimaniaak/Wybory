package com.election.util;

import com.election.model.Candidate;
import com.election.model.User;
import org.hibernate.Session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

public class DataInitializer {

    public static void initializeIfEmpty(Session session) {
        boolean shouldImport = isDatabaseEmpty(session);

        if (shouldImport) {
            System.out.println("Import danych z import.sql...");
            runImportSQL(session);
        } else {
            System.out.println("Baza danych już zawiera dane. Import pominięty.");
        }
    }

    private static boolean isDatabaseEmpty(Session session) {
        List<?> users = session.createQuery("from User").setMaxResults(1).list();
        List<?> candidates = session.createQuery("from Candidate").setMaxResults(1).list();
        return users.isEmpty() && candidates.isEmpty();
    }

    private static void runImportSQL(Session session) {
        runSqlFile(session, "import.sql");
        runSqlFile(session, "import_dynamic.sql");
    }

    private static void runSqlFile(Session session, String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                DataInitializer.class.getClassLoader().getResourceAsStream(fileName)))) {

            String sql = reader.lines().collect(Collectors.joining("\n"));

            session.doWork(connection -> {
                try (Statement stmt = connection.createStatement()) {
                    for (String part : sql.split(";")) {
                        if (!part.trim().isEmpty()) {
                            stmt.execute(part.trim());
                        }
                    }
                }
            });

            System.out.println("Zaimportowano dane z " + fileName);

        } catch (Exception e) {
            System.err.println("Błąd przy imporcie pliku " + fileName + ": " + e.getMessage());
        }
    }

}
