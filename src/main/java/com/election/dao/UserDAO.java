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
import java.util.List;

// DAO dla użytkowników
public class UserDAO {

    // Pobiera wszystkich użytkowników
    public List<User> getAllUsers() throws DatabaseException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User", User.class).list();
        } catch (HibernateException e) {
            throw new DatabaseException("Błąd pobierania użytkowników z bazy danych", e);
        }
    }

    // Pobiera użytkownika po ID
    public User getUserById(Long id) throws DatabaseException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(User.class, id);
        } catch (HibernateException e) {
            throw new DatabaseException("Błąd pobierania użytkownika o ID: " + id, e);
        }
    }

    // Aktualizuje dane użytkownika
    public void updateUser(User user) throws DatabaseException {
        // Najpierw walidacja w osobnej sesji
        validateUniqueConstraints(user);

        Transaction transaction = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            // Ładujemy użytkownika z bazy danych
            User managedUser = session.get(User.class, user.getId());

            // Jeśli użytkownik nie istnieje
            if (managedUser == null) {
                throw new DatabaseException("Użytkownik nie istnieje w systemie", null);
            }

            // Aktualizacja pól tylko jeśli użytkownik istnieje
            managedUser.setFirstName(user.getFirstName());
            managedUser.setLastName(user.getLastName());
            managedUser.setPesel(user.getPesel());
            managedUser.setUsername(user.getUsername());
            managedUser.setRole(user.getRole());

            // Aktualizacja hasła tylko jeśli zostało zmienione
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                managedUser.setPassword(user.getPassword());
            }

            session.merge(managedUser);
            transaction.commit();
            appendUserToSql(managedUser);
        } catch (ValidationException | HibernateException e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new DatabaseException(e.getMessage(), e);
        } catch (IOException e) {
            throw new DatabaseException("Błąd tworzenia kopii zapasowej użytkownika", e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Usuwa użytkownika z bazy
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

    // Walidacja w osobnej sesji
    private void validateUniqueConstraints(User user) throws ValidationException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Sprawdź unikalność loginu
            Query<User> usernameQuery = session.createQuery(
                    "FROM User WHERE username = :username AND id != :id", User.class);
            usernameQuery.setParameter("username", user.getUsername());
            usernameQuery.setParameter("id", user.getId() != null ? user.getId() : -1L);

            if (!usernameQuery.getResultList().isEmpty()) {
                throw new ValidationException("Login już istnieje w systemie!");
            }

            // Sprawdź unikalność PESEL
            Query<User> peselQuery = session.createQuery(
                    "FROM User WHERE pesel = :pesel AND id != :id", User.class);
            peselQuery.setParameter("pesel", user.getPesel());
            peselQuery.setParameter("id", user.getId() != null ? user.getId() : -1L);

            if (!peselQuery.getResultList().isEmpty()) {
                throw new ValidationException("PESEL już istnieje w systemie!");
            }
        }
    }

    // Zapisuje nowego użytkownika
    public void saveUser(User user) throws DatabaseException {
        Transaction transaction = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            // Walidacja unikalności w osobnej sesji
            validateUniqueConstraints(user);

            session.persist(user);
            transaction.commit();
            appendUserToSql(user);
        } catch (ValidationException | HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw new DatabaseException(e.getMessage(), e);
        } catch (IOException e) {
            throw new DatabaseException("Błąd tworzenia kopii zapasowej użytkownika", e);
        } finally {
            if (session != null) session.close();
        }
    }



    // Dodaje użytkownika do pliku SQL (backup)
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

    public int countAdmins() throws DatabaseException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT COUNT(*) FROM User WHERE role = 'ADMIN'",
                    Long.class
            );
            return Math.toIntExact(query.uniqueResult());
        } catch (HibernateException e) {
            throw new DatabaseException("Błąd podczas zliczania administratorów", e);
        }
    }

    // Wyszukuje użytkownika po loginie
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

    // Wyszukuje użytkownika po numerze PESEL
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
