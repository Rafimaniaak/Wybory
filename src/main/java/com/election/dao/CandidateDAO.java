package com.election.dao;

import com.election.model.Candidate;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class CandidateDAO {
    public List<Candidate> getAllCandidates() {
        try (Session session = JPAUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Candidate", Candidate.class).list();
        }
    }

    // Dodaj przykładowych kandydatów (do inicjalizacji)
//    public void addSampleCandidates() {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            Transaction transaction = session.beginTransaction();
//
//            // Usuń istniejących kandydatów (opcjonalnie)
//            session.createQuery("DELETE FROM Candidate").executeUpdate();
//
//            // Dodaj nowych kandydatów
//            session.persist(new Candidate("Jan Kowalski", "Partia X"));
//            session.persist(new Candidate("Anna Nowak", "Partia Y"));
//            session.persist(new Candidate("Piotr Wiśniewski", "Partia Z"));
//
//            transaction.commit();
//        }
//    }
}
