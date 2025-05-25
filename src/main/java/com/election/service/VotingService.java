package com.election.service;

import com.election.dao.ElectionResultDAO;
import com.election.exception.VotingException;
import com.election.model.Candidate;
import com.election.model.ElectionResult;
import com.election.model.User;
import com.election.model.Vote;
import com.election.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class VotingService {
    private final ElectionResultDAO resultDAO = new ElectionResultDAO();
    public boolean hasUserVoted(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return !session.createQuery(
                    "SELECT v FROM Vote v WHERE v.user = :user",
                    Vote.class
            ).setParameter("user", user).list().isEmpty();
        }
    }
    public void castVote(User user, Candidate candidate) throws VotingException {
        if (hasUserVoted(user)) {
            throw new VotingException("Użytkownik już oddał głos");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                Vote vote = new Vote();
                vote.setUser(user);
                vote.setCandidate(candidate);
                session.persist(vote);

                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
                throw new VotingException("Błąd podczas zapisywania głosu: " + e.getMessage());
            }
        }
    }

    private void updateElectionResults(String candidate) {
        ElectionResult result = resultDAO.findByCandidate(candidate);

        if (result == null) {
            result = new ElectionResult(candidate, 1);
        } else {
            result.setVotes(result.getVotes() + 1);
        }

        resultDAO.saveOrUpdate(result);
    }
}