package com.election.dao;

import com.election.exception.DatabaseException;
import com.election.model.Candidate;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

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

    public void addCandidate(Candidate candidate) throws DatabaseException {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(candidate);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new DatabaseException("Błąd dodawania kandydata: " + e.getMessage(), e);
        }
    }

    public void deleteCandidate(Long id) throws DatabaseException {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Candidate candidate = session.get(Candidate.class, id);
            if (candidate != null) {
                // Sprawdź głosy przed usunięciem
                if (candidate.getVotes() > 0) {
                    throw new DatabaseException("Nie można usunąć kandydata z głosami!", null);
                }
                session.remove(candidate);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new DatabaseException("Błąd usuwania kandydata: " + e.getMessage(), e);
        }
    }

    public void updateCandidate(Candidate candidate) throws DatabaseException {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(candidate);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new DatabaseException("Błąd aktualizacji kandydata: " + e.getMessage(), e);
        }
    }
    public boolean candidateExists(String name, String party) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(c) FROM Candidate c WHERE c.name = :name AND c.party = :party";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("name", name);
            query.setParameter("party", party);
            return query.uniqueResult() > 0;
        } catch (Exception e) {
            throw new DatabaseException("Błąd podczas sprawdzania unikalności kandydata", e);
        }
    }
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
