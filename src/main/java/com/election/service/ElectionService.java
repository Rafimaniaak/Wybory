package com.election.service;

import com.election.exception.VotingException;
import com.election.model.Candidate;
import com.election.model.CandidateResult;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class ElectionService {

    public List<CandidateResult> getCurrentResults() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT new com.election.model.CandidateResult(c.id, c.name, c.party, c.votes) FROM Candidate c";
            return session.createQuery(hql, CandidateResult.class).getResultList();
        }
    }

    public List<Candidate> getAllCandidates() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Candidate", Candidate.class).getResultList();
        }
    }

    public void registerVote(Candidate candidate) throws VotingException {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = null;

        try {
            transaction = session.beginTransaction();

            Candidate managedCandidate = session.get(Candidate.class, candidate.getId());
            managedCandidate.setVotes(managedCandidate.getVotes() + 1);

            session.update(managedCandidate);
            transaction.commit();

        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new VotingException("Błąd podczas rejestracji głosu: " + e.getMessage());
        } finally {
            session.close();
        }
    }
}