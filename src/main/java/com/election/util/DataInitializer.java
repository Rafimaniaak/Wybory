package com.election.util;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DataInitializer {
    public static void initialize() {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            if (isDatabaseEmpty(session)) {
                try {
                    executeSqlScript(session, "import.sql");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static boolean isDatabaseEmpty(Session session) {
        return session.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .uniqueResult() == 0;
    }

    private static void executeSqlScript(Session session, String scriptPath) throws IOException {
        String sql = new String(
                Files.readAllBytes(Paths.get("src/main/resources/" + scriptPath)),
                StandardCharsets.UTF_8
        );

        // Usuń komentarze i puste linie
        sql = sql.replaceAll("--.*", "")
                .replaceAll("/\\*.*?\\*/", "")
                .trim();

        Transaction transaction = session.beginTransaction();
        try {
            session.createNativeQuery(sql).executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        }
    }
}