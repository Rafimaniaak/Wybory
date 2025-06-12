package com.election.dao;

import com.election.exception.DatabaseException;
import com.election.model.Candidate;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class CandidateDAO {

    // Pobierz wszystkich kandydatów
    public List<Candidate> getAllCandidates() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Candidate", Candidate.class).list();
        } catch (Exception e) {
            throw new DatabaseException("Błąd podczas pobierania kandydatów z bazy danych", e);
        }
    }

//    // Dodaj nowego kandydata
//    public void addCandidate(Candidate candidate) {
//        Transaction transaction = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            transaction = session.beginTransaction();
//            session.persist(candidate);
//            transaction.commit();
//        } catch (Exception e) {
//            if (transaction != null) transaction.rollback();
//            e.printStackTrace();
//        }
//    }
//
//    // Usuń kandydata po ID
//    public void deleteCandidate(Long id) {
//        Transaction transaction = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            transaction = session.beginTransaction();
//            Candidate candidate = session.get(Candidate.class, id);
//            if (candidate != null) {
//                session.remove(candidate);
//            }
//            transaction.commit();
//        } catch (Exception e) {
//            if (transaction != null) transaction.rollback();
//            e.printStackTrace();
//        }
//    }
//
//    // Zaktualizuj dane kandydata
//    public void updateCandidate(Candidate candidate) {
//        Transaction transaction = null;
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            transaction = session.beginTransaction();
//            session.merge(candidate);
//            transaction.commit();
//        } catch (Exception e) {
//            if (transaction != null) transaction.rollback();
//            e.printStackTrace();
//        }
//    }
//
//    // Pobierz kandydata po ID
//    public Candidate getCandidateById(Long id) {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            return session.get(Candidate.class, id);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
}
