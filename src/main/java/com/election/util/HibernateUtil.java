package com.election.util;

import com.election.model.ElectionResult;
import com.election.model.User;
import com.election.model.Candidate;
import com.election.model.Vote;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Wczytaj konfigurację z hibernate.cfg.xml z classpath
            Configuration configuration = new Configuration().configure();

            // Dodaj klasy mapowane (jeśli nie masz <mapping> w XML, ale tu są redundantne)
            configuration.addAnnotatedClass(User.class);
            configuration.addAnnotatedClass(Candidate.class);
            configuration.addAnnotatedClass(Vote.class);
            configuration.addAnnotatedClass(ElectionResult.class);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            return configuration.buildSessionFactory(serviceRegistry);

        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
}