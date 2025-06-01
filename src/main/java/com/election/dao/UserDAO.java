package com.election.dao;

import com.election.model.User;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class UserDAO {

    public List<User> getAllUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<User> users = session.createQuery("FROM User", User.class).list();
            System.out.println("DEBUG: Znaleziono użytkowników: " + users.size());
            users.forEach(u -> System.out.println(u.getUsername() + " | " + u.isHasVoted()));
            return users;
        } catch (Exception e) {
            System.err.println("Błąd pobierania użytkowników:");
            e.printStackTrace();
            return List.of(); // Zwróć pustą listę w razie błędu
        }
    }
    public User getUserById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(User.class, id);
        } catch (Exception e) {
            System.err.println("Błąd pobierania użytkownika o ID: " + id);
            e.printStackTrace();
            return null;
        }
    }

    public void updateUser(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(user);
            transaction.commit();
            System.out.println("Zaktualizowano użytkownika: " + user.getUsername());

            // Aktualizacja backupu
            appendUserToSql(user);  // Możesz to zostawić lub pominąć przy aktualizacji

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("Błąd aktualizacji użytkownika:");
            e.printStackTrace();
        }
    }

    public void deleteUser(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.delete(user);
            transaction.commit();
            System.out.println("Usunięto użytkownika: " + user.getUsername());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("Błąd usuwania użytkownika:");
            e.printStackTrace();
        }
    }


    public void saveUser(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(user);
            transaction.commit();
            session.close();
            // Dodajemy kopię zapasową
            appendUserToSql(user);

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }
    public static void appendUserToSql(User user) {
        String filePath = "src/main/resources/import_dynamic.sql";  // <-- zapis do resources (dev-only)

        // Składnia SQL INSERT
        String insert = String.format(
                "INSERT INTO USERS (id, username, password, role, has_voted) VALUES (%d, '%s', '%s', '%s', %b);%n",
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRole(),
                user.isHasVoted()
        );

        try (FileWriter fw = new FileWriter(filePath, true)) {
            fw.write(insert);
        } catch (IOException e) {
            e.printStackTrace(); // tylko IO
        }
    }

    public User findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User WHERE username = :username", User.class);
            query.setParameter("username", username);
            return query.uniqueResult();
        } catch (Exception e) {
            System.err.println("Błąd wyszukiwania użytkownika " + username + ":");
            e.printStackTrace();
            return null;
        }
    }
    public User findByPesel(String pesel) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User WHERE pesel = :pesel", User.class)
                    .setParameter("pesel", pesel)
                    .uniqueResult();
        }
    }


    public boolean isAdmin(User user) {
        return "ADMIN".equals(user.getRole());
    }
}
