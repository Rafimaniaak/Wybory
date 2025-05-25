package com.election.dao;

import com.election.model.User;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.query.Query;

import java.util.List;

//public class UserDAO {
//    public User authenticate(String username, String password) {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            return session.createQuery("FROM User WHERE username = :username AND password = :password", User.class)
//                    .setParameter("username", username)
//                    .setParameter("password", password)
//                    .uniqueResult();
//        }
//    }
public class UserDAO {
    private static SessionFactory sessionFactory;

    // Inicjalizacja SessionFactory przy pierwszym użyciu klasy
    static {
        try {
            // Dla Hibernate 5.x i wyżej
            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml") // lub "persistence.xml"
                    .build();
            sessionFactory = new MetadataSources(registry)
                    .buildMetadata()
                    .buildSessionFactory();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError("Błąd inicjalizacji SessionFactory");
        }
    }

    public List<User> getAllUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User", User.class).list();
        }
    }

    public boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }
    public User findByUsername(String username) {
        Session session = sessionFactory.openSession(); // Użyj istniejącego SessionFactory
        try {
            Query<User> query = session.createQuery("FROM User WHERE username = :username", User.class);
            query.setParameter("username", username);
            return query.uniqueResult();
        } finally {
            session.close(); // Zawsze zamykaj sesję!
        }
    }
//    public void initializeSessionFactory() {
//        Session session = sessionFactory.openSession();
//        session.close(); // Tylko inicjalizacja, bez wykonywania zapytań
//    }
    //private static SessionFactory sessionFactory;

    // Inicjalizacja SessionFactory (np. w bloku statycznym)
    static {
        try {
            StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .configure("hibernate.cfg.xml")
                    .build();

            MetadataSources metadataSources = new MetadataSources(registry);
            metadataSources.addAnnotatedClass(User.class); // Dodaj encję User

            Metadata metadata = metadataSources.getMetadataBuilder().build();
            sessionFactory = metadata.getSessionFactoryBuilder().build();

            System.out.println("SessionFactory zainicjalizowany poprawnie!");
        } catch (Exception e) {
            System.err.println("Błąd inicjalizacji SessionFactory:");
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    public void save(User user) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            System.out.println("Zapisano użytkownika: " + user.getUsername()); // Log sukcesu
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("Błąd zapisu użytkownika " + user.getUsername() + ":");
            e.printStackTrace();
        } finally {
            session.close();
        }
    }
}
