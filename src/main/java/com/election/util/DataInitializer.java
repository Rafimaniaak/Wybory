package com.election.util;

import org.hibernate.Session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// Inicjalizator danych
public class DataInitializer {

    // Inicjalizuje bazę danych jeśli jest pusta
    public static void initializeIfEmpty(Session session) {
        boolean shouldImport = isDatabaseEmpty(session);

        if (shouldImport) {
            System.out.println("Import danych z import.sql...");
            runImportSQL(session);
        } else {
            System.out.println("Baza danych już zawiera dane. Import pominięty.");
        }
    }

    // Sprawdza czy baza jest pusta
    private static boolean isDatabaseEmpty(Session session) {
        List<?> users = session.createQuery("from User").setMaxResults(1).list();
        List<?> candidates = session.createQuery("from Candidate").setMaxResults(1).list();
        return users.isEmpty() && candidates.isEmpty();
    }

    // Wykonuje skrypty SQL
    private static void runImportSQL(Session session) {
        runSqlFile(session, "import.sql");
        runSqlFile(session, "import_dynamic.sql");
    }

    // Wykonuje pojedynczy plik SQL
    private static void runSqlFile(Session session, String fileName) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(DataInitializer.class.getClassLoader().getResourceAsStream(fileName))))) {

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
