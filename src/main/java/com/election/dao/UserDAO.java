package com.election.dao;

import com.election.exception.DatabaseException;
import com.election.exception.ValidationException;
import com.election.model.User;
import com.election.util.HibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class UserDAO {

    public List<User> getAllUsers() throws DatabaseException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User", User.class).list();
        } catch (HibernateException e) {
            throw new DatabaseException("Błąd pobierania użytkowników z bazy danych", e);
        }
    }

    public User getUserById(Long id) throws DatabaseException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(User.class, id);
        } catch (HibernateException e) {
            throw new DatabaseException("Błąd pobierania użytkownika o ID: " + id, e);
        }
    }

    public void updateUser(User user) throws DatabaseException {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(user); // Zamiast session.update(user)
            transaction.commit();

            appendUserToSql(user);
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw new DatabaseException("Błąd aktualizacji użytkownika: " + user.getUsername(), e);
        } catch (IOException e) {
            throw new DatabaseException("Błąd tworzenia kopii zapasowej użytkownika", e);
        }
    }

    public void deleteUser(User user) throws DatabaseException {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(user); // Zamiast session.delete(user)
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw new DatabaseException("Błąd usuwania użytkownika: " + user.getUsername(), e);
        }
    }

    public void saveUser(User user) throws DatabaseException {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(user); // Zamiast session.save(user)
            transaction.commit();

            appendUserToSql(user);
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw new DatabaseException("Błąd zapisu użytkownika: " + user.getUsername(), e);
        } catch (IOException e) {
            throw new DatabaseException("Błąd tworzenia kopii zapasowej użytkownika", e);
        }
    }

    private static void appendUserToSql(User user) throws IOException {
        String filePath = "src/main/resources/import_dynamic.sql";
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
        }
    }

    public User findByUsername(String username) throws DatabaseException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User WHERE username = :username", User.class
            );
            query.setParameter("username", username);
            return query.uniqueResult();
        } catch (HibernateException e) {
            throw new DatabaseException("Błąd wyszukiwania użytkownika po loginie: " + username, e);
        }
    }

    public User findByPesel(String pesel) throws DatabaseException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery(
                    "FROM User WHERE pesel = :pesel", User.class
            );
            query.setParameter("pesel", pesel);
            return query.uniqueResult();
        } catch (HibernateException e) {
            throw new DatabaseException("Błąd wyszukiwania użytkownika po PESEL: " + pesel, e);
        }
    }
}
