package com.election.service;

import com.election.dao.CandidateDAO;
import com.election.exception.VotingException;
import com.election.model.Candidate;
import com.election.model.CandidateResult;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.stream.Collectors;

public class ElectionService {
    private final CandidateDAO candidateDAO = new CandidateDAO();

    public List<Candidate> getCurrentResults() {
        return candidateDAO.getAllCandidates();
    }

//    public List<CandidateResult> getCurrentResults() {
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//            // Zapytanie HQL zwracające dane w formacie dla CandidateResult
//            String hql = "SELECT new com.election.model.CandidateResult(c.name, c.party, c.votes) FROM Candidate c";
//            return session.createQuery(hql, CandidateResult.class).list();
//        }
//    }
    public List<Candidate> getAllCandidates() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Candidate", Candidate.class).list();
        }
    }
    public void registerVote(Candidate candidate) throws VotingException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            // Ponownie załaduj kandydata z bazy (aby uniknąć problemów z detached entity)
            Candidate managedCandidate = session.get(Candidate.class, candidate.getId());
            managedCandidate.setVotes(managedCandidate.getVotes() + 1); // Inkrementacja głosów

            session.merge(managedCandidate);
            transaction.commit();
        } catch (Exception e) {
            throw new VotingException("Błąd podczas rejestracji głosu: " + e.getMessage());
        }
    }
}